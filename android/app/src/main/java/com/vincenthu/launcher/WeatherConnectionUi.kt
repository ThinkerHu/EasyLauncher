package com.vincenthu.launcher

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri

@Composable
internal fun WeatherConnectionPage(model: LauncherModel, onSaved: () -> Unit) {
    var host by rememberSaveable { mutableStateOf(model.weatherApiHost) }
    // Credentials are deliberately excluded from saveable state and screenshots.
    var key by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 580.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFE0EDF3)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("连接和风天气", color = Color(0xFF29465A), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Text("实况、逐小时预报和未来五日天气，统一显示在你的桌面。", color = Color(0xFF617F91), fontSize = 13.sp, lineHeight = 21.sp)
                }
            }
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE5ECEF))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("账户连接", color = Color(0xFF29465A), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    OutlinedTextField(value = host, onValueChange = { host = it.trim(); error = null }, label = { Text("API Host") },
                        placeholder = { Text("abcxyz.re.qweatherapi.com", fontSize = 12.sp) },
                        supportingText = { Text("控制台 → 设置 → API Host；由和风分配，无需购买域名", fontSize = 10.sp) },
                        singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next))
                    OutlinedTextField(value = key, onValueChange = { key = it.trim(); error = null }, label = { Text("API Key") },
                        placeholder = { Text(if (model.weatherConfigured) "已保存，留空保留现有 Key" else "填写控制台创建的 API Key", fontSize = 12.sp) },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Button(onClick = {
                        runCatching { model.saveWeatherConnection(host, key) }
                            .onSuccess { key = ""; focus.clearFocus(); onSaved() }
                            .onFailure { error = if (it is IllegalArgumentException) it.message else "保存连接失败，请重试" }
                    }, enabled = host.isNotBlank() && (key.isNotBlank() || model.weatherConfigured), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(13.dp)) {
                        Text("保存并连接", fontSize = 14.sp)
                    }
                }
            }
            Text("Key 在此设备加密保存，不写入项目源码，也不包含在系统备份中。每次刷新请求实况、每日和小时预报，前台每 30 分钟自动更新。", color = Color(0xFF738B98), fontSize = 11.sp, lineHeight = 19.sp)
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, "https://console.qweather.com/".toUri())) } }) { Text("打开和风控制台", fontSize = 12.sp) }
        }
    }
}
