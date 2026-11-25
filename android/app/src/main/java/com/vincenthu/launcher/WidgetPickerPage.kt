package com.vincenthu.launcher

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val WidgetInk = Color(0xFF263F50)
private val WidgetMuted = Color(0xFF69818D)
private val WidgetAccent = Color(0xFF3B6C78)
private val WidgetLine = Color(0xFFDDE7EB)

/** Full-screen catalog. Only the explicit add button starts binding a widget. */
@Composable
internal fun WidgetPickerPage(
    host: WidgetHostController,
    adding: Boolean,
    addError: String?,
    onBack: () -> Unit,
    onAdd: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val loader = remember { WidgetPreviewLoader(context) }
    var catalog by remember { mutableStateOf<WidgetCatalogSnapshot?>(null) }
    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val appListState = rememberLazyListState()
    LaunchedEffect(query, selectedPackage) { gridState.scrollToItem(0) }
    val keyboard = LocalSoftwareKeyboardController.current
    val motion = LocalLauncherMotionEnabled.current
    LaunchedEffect(attempt) {
        error = false
        try {
            catalog = withContext(Dispatchers.IO) { loader.catalog(host) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { error = true }
    }
    val entries = catalog?.items.orEmpty()
    val selected = entries.firstOrNull { it.key == selectedKey }
    BackHandler { if (selectedKey != null) selectedKey = null else onBack() }
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF2F6F7)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                WidgetIconButton(if (selected != null) "返回小组件列表" else "返回桌面", "back") {
                    if (selected != null) selectedKey = null else onBack()
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(if (selected != null) "小组件预览" else "小组件", color = WidgetInk, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (selected != null) selected.appLabel else "挑选喜欢的样式，让信息触手可及", color = WidgetMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (selected != null) WidgetIconButton("关闭小组件选择", "close", onBack)
                else if (catalog != null) WidgetChip("${entries.size} 款")
            }
            HorizontalDivider(color = WidgetLine)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    error -> WidgetEmptyState("暂时无法读取小组件", "请稍后重试，桌面内容不会受到影响。", "重新加载", { attempt++ })
                    catalog == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = WidgetAccent, strokeWidth = 2.dp)
                        Text("正在载入小组件…", color = WidgetMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
                    }
                    entries.isEmpty() -> WidgetEmptyState("还没有可添加的小组件", "安装支持桌面小组件的应用后，可以在这里找到。")
                    else -> Crossfade(targetState = selected?.key, animationSpec = tween(if (motion) 180 else 0), label = "widget-selection") { key ->
                        val item = entries.firstOrNull { it.key == key }
                        val provider = catalog?.providers?.get(key)
                        if (item != null && provider != null) {
                            WidgetDetail(item, provider, loader, adding, addError) { keyboard?.hide(); onAdd(provider) }
                        } else {
                            WidgetBrowse(
                                entries, catalog!!.providers, loader, gridState, appListState, query, { query = it }, selectedPackage,
                                { selectedPackage = it },
                                { selectedKey = it.key; keyboard?.hide() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetBrowse(
    items: List<WidgetCatalogItem>, providers: Map<String, AppWidgetProviderInfo>, loader: WidgetPreviewLoader,
    gridState: LazyGridState, appListState: LazyListState,
    query: String, onQueryChange: (String) -> Unit, selectedPackage: String?, onPackageChange: (String?) -> Unit,
    onSelect: (WidgetCatalogItem) -> Unit
) {
    val groups = remember(items) { items.groupBy { it.packageName } }
    val visible = remember(items, query, selectedPackage) { WidgetCatalog.filter(items, query, selectedPackage) }
    val keyboard = LocalSoftwareKeyboardController.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                LazyColumn(Modifier.width(210.dp).fillMaxHeight().padding(horizontal = 14.dp), state = appListState, contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Text("按应用浏览", color = WidgetMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)) }
                    item { WidgetAppRow(null, "全部小组件", items.size, selectedPackage == null, loader) { onPackageChange(null) } }
                    items(groups.entries.toList(), key = { it.key }) { (pkg, entries) ->
                        WidgetAppRow(pkg, entries.first().appLabel, entries.size, selectedPackage == pkg, loader) { onPackageChange(pkg) }
                    }
                }
                VerticalDivider(color = WidgetLine)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                OutlinedTextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    placeholder = { Text("搜索应用或小组件", fontSize = 14.sp) },
                    leadingIcon = { WidgetGlyph("search", Modifier.size(19.dp), WidgetMuted) },
                    trailingIcon = if (query.isNotEmpty()) ({ WidgetIconButton("清除搜索", "close") { onQueryChange("") } }) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = .8f), focusedContainerColor = Color.White,
                        unfocusedBorderColor = WidgetLine, focusedBorderColor = WidgetAccent
                    ), shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp).heightIn(min = 52.dp)
                )
                if (!wide) {
                    androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                        item { FilterChip(selected = selectedPackage == null, onClick = { onPackageChange(null) }, label = { Text("全部") }) }
                        items(groups.entries.toList(), key = { it.key }) { (pkg, entries) ->
                            FilterChip(selected = selectedPackage == pkg, onClick = { onPackageChange(pkg) }, label = { Text(entries.first().appLabel) })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (query.isNotBlank()) "搜索结果" else groups[selectedPackage]?.firstOrNull()?.appLabel ?: "全部小组件", color = WidgetInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${visible.size} 款", color = WidgetMuted, fontSize = 12.sp)
                }
                if (visible.isEmpty()) {
                    WidgetEmptyState("没有找到相关小组件", "试试其他关键词，或切换到全部应用。", "清除筛选", { onQueryChange(""); onPackageChange(null) }, Modifier.weight(1f).fillMaxWidth())
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(230.dp),
                        state = gridState,
                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(visible, key = { it.key }) { item ->
                            providers[item.key]?.let { info -> WidgetPreviewCard(item, info, loader) { onSelect(item) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetAppRow(pkg: String?, label: String, count: Int, selected: Boolean, loader: WidgetPreviewLoader, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (selected) Color(0xFFE0ECEE) else Color.Transparent) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pkg != null) WidgetAppIcon(pkg, loader, Modifier.size(30.dp))
            else Box(Modifier.size(30.dp).background(Color.White.copy(alpha = .8f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { WidgetGlyph("widgets", Modifier.size(18.dp), WidgetAccent) }
            Text(label, Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 13.sp, color = WidgetInk, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(count.toString(), color = WidgetMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun WidgetPreviewCard(item: WidgetCatalogItem, provider: AppWidgetProviderInfo, loader: WidgetPreviewLoader, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, interactionSource = interaction, shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = .88f), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
        modifier = Modifier.fillMaxWidth().pressFeedback(interaction)
    ) {
        Column(Modifier.padding(12.dp)) {
            WidgetPreviewStage(item, provider, loader, Modifier.fillMaxWidth().height(150.dp), onClick)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                WidgetAppIcon(item.packageName, loader, Modifier.size(30.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                    Text(item.label, color = WidgetInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.appLabel, color = WidgetMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(28.dp).background(Color(0xFFE9F1F2), CircleShape), contentAlignment = Alignment.Center) { WidgetGlyph("forward", Modifier.size(14.dp), WidgetAccent) }
            }
            Text(item.sizeLabel + if (item.resizable) " · 尺寸自适应" else "", color = WidgetMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 3.dp))
            if (item.compatibilityHint != null) Text("华为主题专属 · 兼容性受限", color = Color(0xFF91632E), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp), maxLines = 1)
        }
    }
}

@Composable
private fun WidgetDetail(item: WidgetCatalogItem, provider: AppWidgetProviderInfo, loader: WidgetPreviewLoader, adding: Boolean, error: String?, onAdd: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp)) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                WidgetDetailPreview(item, provider, loader, Modifier.weight(1.2f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                    WidgetDetails(item, loader)
                    WidgetAddFeedback(error)
                    WidgetAddButton(item, adding, onAdd, Modifier.padding(top = 26.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    WidgetDetailPreview(item, provider, loader, Modifier.fillMaxWidth().height(340.dp))
                    WidgetDetails(item, loader)
                    WidgetAddFeedback(error)
                }
                WidgetAddButton(item, adding, onAdd, Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun WidgetDetailPreview(item: WidgetCatalogItem, provider: AppWidgetProviderInfo, loader: WidgetPreviewLoader, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(30.dp), color = Color.White.copy(alpha = .65f), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetChip("外观预览")
                Spacer(Modifier.weight(1f))
                Text(item.sizeLabel, color = WidgetMuted, fontSize = 11.sp)
            }
            WidgetPreviewStage(item, provider, loader, Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp))
            Text("预览由应用提供，实际内容以添加后为准", color = WidgetMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WidgetDetails(item: WidgetCatalogItem, loader: WidgetPreviewLoader) {
    Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetAppIcon(item.packageName, loader, Modifier.size(48.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text(item.appLabel, color = WidgetMuted, fontSize = 12.sp)
            Text(item.label, color = WidgetInk, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
        }
    }
    Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetChip(if (item.resizable) "尺寸自适应" else "应用指定尺寸")
        WidgetChip(if (item.configurable) "添加时配置" else "直接添加")
    }
    Text(item.sizeLabel, color = WidgetMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 13.dp))
    HorizontalDivider(Modifier.padding(vertical = 22.dp), color = WidgetLine)
    Text("添加到桌面", color = WidgetInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Text("自动排入已有的小组件页面，放满后再增加新页。横竖屏切换时自动调整排列。", color = WidgetMuted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 9.dp))
    if (item.configurable) Text("此组件需要先完成应用的配置，取消后不会添加到桌面。", color = WidgetMuted, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
    item.compatibilityHint?.let {
        Surface(Modifier.fillMaxWidth().padding(top = 18.dp), color = Color(0xFFF5EDDd), shape = RoundedCornerShape(16.dp)) {
            Text("兼容性提示\n$it。预览可显示，不代表组件内容可以正常加载。", color = Color(0xFF865B2F), fontSize = 12.sp, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
        }
    }
    }
}

@Composable
private fun WidgetAddButton(item: WidgetCatalogItem, adding: Boolean, onAdd: () -> Unit, modifier: Modifier) {
    Button(onClick = onAdd, enabled = !adding, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp).semantics { contentDescription = "添加${item.label}到桌面" },
        shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = WidgetAccent)) {
        if (adding) CircularProgressIndicator(Modifier.size(18.dp), color = WidgetAccent, strokeWidth = 2.dp)
        else WidgetGlyph("add", Modifier.size(18.dp), Color.White)
        Text(if (adding) "正在添加…" else if (item.configurable) "配置并添加" else "添加到桌面", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun WidgetPreviewStage(item: WidgetCatalogItem, provider: AppWidgetProviderInfo, loader: WidgetPreviewLoader, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val preview by produceState<WidgetPreview?>(initialValue = null, key1 = item.key, key2 = loader) {
        value = withContext(Dispatchers.IO) { loader.preview(provider) }
    }
    var layoutFailed by remember(item.key) { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFFE3ECF0), Color(0xFFD4E4E5), Color(0xFFE7EDEF)))).padding(16.dp), contentAlignment = Alignment.Center) {
        when (val state = preview) {
            null -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = WidgetAccent)
            is WidgetPreview.Artwork -> Image(state.bitmap.asImageBitmap(), contentDescription = "${item.label}外观预览", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            is WidgetPreview.Layout -> if (!layoutFailed) {
                AndroidView(
                    factory = { context -> FrameLayout(context).apply {
                        try { addView(RemoteViews(state.packageName, state.layoutId).apply(context, this)) }
                        catch (_: Exception) { layoutFailed = true }
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    } },
                    modifier = Modifier.fillMaxSize()
                )
                // Preview layouts may contain actions; browsing must never trigger them.
                Box(Modifier.matchParentSize().clickable(role = Role.Image, onClick = onClick ?: {}))
            } else WidgetPreviewUnavailable(item, loader)
            WidgetPreview.Unavailable -> WidgetPreviewUnavailable(item, loader)
        }
    }
}

@Composable
private fun WidgetAddFeedback(error: String?) {
    if (error != null) Text(error, color = Color(0xFF91632E), fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun WidgetPreviewUnavailable(item: WidgetCatalogItem, loader: WidgetPreviewLoader) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WidgetAppIcon(item.packageName, loader, Modifier.size(40.dp))
        Text("应用未提供外观预览", color = WidgetMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun WidgetAppIcon(packageName: String, loader: WidgetPreviewLoader, modifier: Modifier) {
    val icon by produceState<Bitmap?>(null, packageName, loader) { value = withContext(Dispatchers.IO) { loader.icon(packageName) } }
    val bitmap = icon
    if (bitmap != null) Image(bitmap.asImageBitmap(), null, modifier.clip(RoundedCornerShape(9.dp)))
    else Box(modifier.background(Color(0xFFDCE7EA), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { WidgetGlyph("widgets", Modifier.fillMaxSize().padding(7.dp), WidgetAccent) }
}

@Composable
private fun WidgetChip(text: String) {
    Text(text, color = WidgetAccent, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFE4EEF0)).padding(horizontal = 10.dp, vertical = 6.dp))
}

@Composable
private fun WidgetEmptyState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(modifier.padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(Color(0xFFE3ECEF), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { WidgetGlyph("widgets", Modifier.size(30.dp), WidgetAccent) }
        Text(title, color = WidgetInk, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
        Text(message, color = WidgetMuted, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 9.dp))
        if (action != null) TextButton(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) { Text(action, color = WidgetAccent) }
    }
}

@Composable
private fun WidgetIconButton(label: String, glyph: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = .8f), CircleShape).semantics { contentDescription = label }) {
        WidgetGlyph(glyph, Modifier.size(20.dp), WidgetInk)
    }
}

@Composable
private fun WidgetGlyph(kind: String, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(w*x1,h*y1), Offset(w*x2,h*y2), stroke, StrokeCap.Round)
        when (kind) {
            "back" -> { line(.62f,.18f,.3f,.5f); line(.3f,.5f,.62f,.82f) }
            "forward" -> { line(.38f,.18f,.7f,.5f); line(.7f,.5f,.38f,.82f) }
            "close" -> { line(.24f,.24f,.76f,.76f); line(.24f,.76f,.76f,.24f) }
            "search" -> { drawCircle(color, w*.29f, Offset(w*.43f,h*.43f), style = Stroke(stroke)); line(.65f,.65f,.9f,.9f) }
            "add" -> { line(.5f,.18f,.5f,.82f); line(.18f,.5f,.82f,.5f) }
            else -> for (row in 0..1) for (col in 0..1) drawRoundRect(color, Offset(w*(.12f+col*.43f), h*(.12f+row*.43f)), androidx.compose.ui.geometry.Size(w*.31f,h*.31f), androidx.compose.ui.geometry.CornerRadius(w*.07f), style = Stroke(stroke))
        }
    }
}
