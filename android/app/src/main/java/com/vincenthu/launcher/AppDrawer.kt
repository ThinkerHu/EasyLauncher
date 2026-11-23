@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vincenthu.launcher

import android.content.res.Configuration
import android.graphics.Bitmap
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LibraryInk = Color.White
private val LibraryMuted = Color.White.copy(alpha = .78f)

/** Category library and its expanded/search views share one edge-to-edge activity surface. */
@Composable
internal fun AppDrawerPage(
    model: LauncherModel,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onLaunch: (LauncherApp, AppLaunchSource?) -> Unit,
    appTile: @Composable (LauncherApp, Dp, (AppLaunchSource?) -> Unit) -> Unit
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val appMap = remember(model.apps) { model.apps.associateBy { it.id } }
    val groups = remember(model.apps) {
        model.apps.groupBy { AppLibraryCategories.classify(it.label, it.packageName, it.systemCategory) }
    }
    val suggestions = remember(model.recentAppIds, model.apps) {
        val recent = DockHistory.visible(model.recentAppIds, appMap.keys, emptyList(), limit = 8)
        // With no history, use real installed apps to keep the initial library useful.
        (recent.mapNotNull(appMap::get) + model.apps).distinctBy { it.packageName }.take(8)
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var groupBackdrop by remember { mutableStateOf<Bitmap?>(null) }
    var groupOrigin by remember { mutableStateOf<Offset?>(null) }
    var openingGroup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun openGroup(key: String, origin: Offset?) {
        if (openingGroup) return
        openingGroup = true
        groupOrigin = origin
        scope.launch {
            try {
                groupBackdrop = context.findActivity()?.window?.let { captureIconMenuBackdrop(it) }
                selected = key
            } finally { openingGroup = false }
        }
    }
    val term = query.trim()
    val selectedCategory = AppLibraryCategory.entries.firstOrNull { it.name == selected }
    val expanded = selected != null
    val groupOpen = selected != null && selected != "all"
    val selectedApps = when (selected) {
        "recommended" -> suggestions
        "all" -> model.apps
        else -> groups[selectedCategory].orEmpty()
    }
    val results = remember(model.apps, term, selectedApps) {
        if (term.isEmpty()) selectedApps else model.apps.filter {
            it.label.contains(term, true) || it.packageName.contains(term, true)
        }
    }
    val gridState = rememberLazyGridState()
    val expandedState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(term, selected) { expandedState.scrollToItem(0) }
    fun navigateBack() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
        when {
            query.isNotEmpty() -> onQueryChange("")
            expanded -> selected = null
            else -> onClose()
        }
    }
    // Own BACK throughout the full-screen page, including its category root.
    // Do not delegate the root case to the underlying launcher's callback.
    BackHandler { navigateBack() }

    Box(Modifier.fillMaxSize().background(Color(0xFF205D50))
        .pointerInput(Unit) { detectTapGestures { } }) {
        LibraryGlassWallpaper(Modifier.matchParentSize())
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .then(if (groupOpen) Modifier.clearAndSetSemantics { } else Modifier)) {
            val width = minOf(maxWidth - if (maxWidth < 420.dp) 28.dp else 48.dp, 660.dp)
            val cardColumns = if (landscape && width >= 580.dp) 4 else 2
            val gap = if (width < 450.dp) 14.dp else 18.dp
            val cardSize = (width - gap * (cardColumns - 1)) / cardColumns
            val cardHeight = if (cardColumns == 2) minOf(cardSize, 218.dp) else cardSize
            val contentTop = if (landscape) 8.dp else 18.dp
            Column(Modifier.width(width).fillMaxHeight().align(Alignment.TopCenter).padding(top = contentTop)) {
                LibrarySearch(
                    query, onQueryChange,
                    onAllApps = { selected = "all"; onQueryChange("") },
                    onClose = onClose,
                    onBack = ::navigateBack,
                    backDescription = if (term.isNotEmpty()) "退出搜索" else if (expanded) "返回应用库" else "返回桌面"
                )
                Spacer(Modifier.height(if (landscape) 16.dp else 18.dp))
                if (term.isNotEmpty() || selected == "all") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (term.isNotEmpty()) "搜索结果" else when (selected) {
                                "recommended" -> "建议"; "all" -> "全部应用"; else -> selectedCategory?.label.orEmpty()
                            },
                            Modifier.weight(1f), color = LibraryInk, fontSize = 20.sp, fontWeight = FontWeight.Medium
                        )
                        Text("${results.size} 个应用", color = LibraryMuted, fontSize = 12.sp)
                    }
                    if (results.isEmpty()) {
                        LibraryEmptyState(model, term, onQueryChange)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(94.dp),
                            state = expandedState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(results, key = { it.id }) { app ->
                                appTile(app, 60.dp) { source -> onLaunch(app, source) }
                            }
                        }
                    }
                } else if (model.apps.isEmpty()) {
                    LibraryEmptyState(model, "", onQueryChange)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cardColumns), state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        item(key = "recommended", span = { GridItemSpan(2) }) {
                            LibraryGroup(
                                title = "建议", apps = suggestions,
                                cardHeight = cardHeight, recommended = true,
                                onOpen = { origin -> openGroup("recommended", origin) }, onLaunch = onLaunch
                            )
                        }
                        items(AppLibraryCategory.entries.filter { !groups[it].isNullOrEmpty() }, key = { it.name }) { category ->
                            LibraryGroup(
                                title = category.label, apps = groups.getValue(category), cardHeight = cardHeight,
                                onOpen = { origin -> openGroup(category.name, origin) }, onLaunch = onLaunch
                            )
                        }
                    }
                }
            }
        }
        ApplicationGroupOverlay(
            group = selected?.takeIf { groupOpen }?.let {
                AppGroupPresentation(it, if (it == "recommended") "建议" else selectedCategory?.label.orEmpty(),
                    selectedApps, groupBackdrop, groupOrigin)
            },
            onClose = { selected = null }, appTile = appTile, onLaunch = onLaunch
        )
        GroupBackEdges(onBack = ::navigateBack)
    }
}

@Composable
private fun LibraryEmptyState(model: LauncherModel, term: String, onQueryChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            model.loading -> {
                CircularProgressIndicator(Modifier.size(28.dp), color = LibraryInk, strokeWidth = 2.dp)
                Text("正在整理应用…", Modifier.padding(top = 16.dp), color = LibraryMuted)
            }
            model.loadFailed -> {
                Text("应用列表更新失败", color = LibraryInk)
                TextButton(onClick = model::refresh) { Text("重新加载", color = LibraryInk) }
            }
            else -> {
                Text(if (term.isEmpty()) "暂无可显示的应用" else "没有找到匹配的应用", color = LibraryInk, fontSize = 16.sp)
                if (term.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text("清除搜索", color = LibraryInk) }
            }
        }
    }
}

@Composable
private fun LibrarySearch(
    query: String, onQueryChange: (String) -> Unit,
    onAllApps: () -> Unit, onClose: () -> Unit, onBack: () -> Unit,
    backDescription: String
) {
    val focusManager = LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    // Actions live outside the text field so pressing BACK never focuses the
    // editor or reopens a vendor input method after navigation.
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .15f), Color(0xFF173D32).copy(alpha = .16f))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .42f), Color.White.copy(alpha = .10f), Color.White.copy(alpha = .24f))), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
            Canvas(Modifier.size(16.dp)) {
                drawLine(LibraryInk, Offset(size.width * .65f, size.height * .15f), Offset(size.width * .3f, size.height * .5f), 1.7.dp.toPx(), StrokeCap.Round)
                drawLine(LibraryInk, Offset(size.width * .3f, size.height * .5f), Offset(size.width * .65f, size.height * .85f), 1.7.dp.toPx(), StrokeCap.Round)
            }
        }
        BasicTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                .semantics { contentDescription = "搜索应用库" },
            textStyle = TextStyle(color = LibraryInk, fontSize = 14.sp),
            singleLine = true, cursorBrush = androidx.compose.ui.graphics.SolidColor(LibraryInk),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focusManager.clearFocus(force = true) }),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("搜索应用库", color = LibraryMuted, fontSize = 14.sp)
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.semantics { contentDescription = "清除搜索" }) {
                Text("×", color = LibraryMuted, fontSize = 23.sp)
            }
        }
        Box {
            IconButton(onClick = { keyboard?.hide(); focusManager.clearFocus(force = true); menu = true },
                modifier = Modifier.semantics { contentDescription = "应用库更多选项" }) {
                Canvas(Modifier.size(18.dp)) {
                    repeat(3) { drawCircle(LibraryMuted, 1.4.dp.toPx(), Offset(size.width / 2, size.height * (.22f + it * .28f))) }
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("全部应用") }, onClick = { menu = false; onAllApps() })
                DropdownMenuItem(text = { Text("返回桌面") }, onClick = { menu = false; onClose() })
            }
        }
    }
}

@Composable
private fun LibraryGroup(
    title: String, apps: List<LauncherApp>, cardHeight: Dp,
    recommended: Boolean = false, onOpen: (Offset?) -> Unit,
    onLaunch: (LauncherApp, AppLaunchSource?) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var origin by remember { mutableStateOf<Offset?>(null) }
    val open = { onOpen(origin) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(cardHeight).onGloballyPositioned { origin = it.localToRoot(Offset(it.size.width / 2f, it.size.height / 2f)) }
                .shadow(4.dp, shape, ambientColor = Color.Black.copy(alpha = .08f), spotColor = Color.Black.copy(alpha = .08f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .15f), Color.White.copy(alpha = .055f), Color(0xFFBCF6E4).copy(alpha = .085f))))
                .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .48f), Color.White.copy(alpha = .08f), Color.White.copy(alpha = .26f))), shape)
                .clickable(onClickLabel = "查看$title", onClick = open)
                .semantics { contentDescription = "$title，${apps.size} 个应用" }
                .padding(14.dp)
        ) {
            val cols = if (recommended) 4 else 2
            val tileSize = minOf(84.dp, (maxWidth - 12.dp * (cols - 1)) / cols, (maxHeight - 12.dp) / 2)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                repeat(2) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(cols) { col ->
                            val index = row * cols + col
                            Box(Modifier.size(tileSize), contentAlignment = Alignment.Center) {
                                if (!recommended && index == 3 && apps.size > 4) {
                                    MiniAppCluster(apps.drop(3).take(4), open)
                                } else {
                                    apps.getOrNull(index)?.let { LibraryIcon(it, Modifier.fillMaxSize(), onLaunch) }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Keep every category reachable even when all four slots contain directly launchable icons.
        Text(title, color = LibraryInk, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "查看$title", onClick = open)
                .padding(top = 6.dp, bottom = 2.dp))
    }
}

@Composable
private fun MiniAppCluster(apps: List<LauncherApp>, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().clickable(onClickLabel = "展开分类", onClick = onOpen)
        .semantics { contentDescription = "展开分类，查看全部应用" }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) { col ->
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        apps.getOrNull(row * 2 + col)?.let { app ->
                            Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryIcon(app: LauncherApp, modifier: Modifier, onLaunch: (LauncherApp, AppLaunchSource?) -> Unit) {
    val anchor = rememberAppLaunchAnchor()
    val interaction = remember(app.id) { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    IconMenuAnchor(app.id, preview = {
        Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
    }, launchAnchor = anchor) { openMenu, iconModifier ->
        Box(modifier.pressFeedback(interaction)
            .combinedClickable(interactionSource = interaction, indication = null,
                onClick = { onLaunch(app, anchor.source()) },
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); openMenu() })) {
            Image(app.icon.asImageBitmap(), "打开：${app.label}", Modifier.fillMaxSize().then(iconModifier)
                .onGloballyPositioned { anchor.coordinates = it }.clip(RoundedCornerShape(16.dp)))
        }
    }
}
