package com.vincenthu.launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Account credentials stay off source control, ordinary preferences and Android backups. */
class WeatherConnection(val host: String, val apiKey: String) {
    override fun toString() = "WeatherConnection(host=$host, apiKey=REDACTED)"
    companion object {
        fun validated(host: String, apiKey: String): WeatherConnection {
            val normalized = host.trim().removePrefix("https://").trimEnd('/').lowercase(java.util.Locale.ROOT)
            require(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9-]{1,63})?\\.qweatherapi\\.com").matches(normalized)) {
                "请填写和风分配的 API Host，如 abcxyz.qweatherapi.com；不是 Key 的访问限制"
            }
            require(Regex("[A-Za-z0-9_-]{16,128}").matches(apiKey.trim())) { "请填写有效的 API Key" }
            return WeatherConnection(normalized, apiKey.trim())
        }
    }
}

internal class WeatherConnectionStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "qweather.connection"))
    private val alias = "easylauncher.qweather.aes.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(): WeatherConnection? = runCatching {
        val envelope = file.openRead().bufferedReader().use { JSONObject(it.readText()) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)))
        }
        val json = JSONObject(String(cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
        WeatherConnection.validated(json.getString("host"), json.getString("key"))
    }.getOrNull()

    fun save(connection: WeatherConnection) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = JSONObject().put("host", connection.host).put("key", connection.apiKey).toString().toByteArray(Charsets.UTF_8)
        val envelope = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(payload), Base64.NO_WRAP)).toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try { stream.write(envelope); file.finishWrite(stream) }
        catch (failure: Exception) { file.failWrite(stream); throw failure }
    }
}
