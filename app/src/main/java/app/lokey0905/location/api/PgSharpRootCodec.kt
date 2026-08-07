package app.lokey0905.location.api

import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class PgSharpRootVersionInfo(
    val appVersion: String,
    val downloadUrl: String,
    val changelog: String,
    val supportedPogoVersions: List<String>
)

internal object PgSharpRootCodec {
    const val SEED = "bnpoeat9NTOiTAv4U6ddZO"

    private val versionRegex = Regex(
        pattern = """(?i)(?:^|[^\d])v?(\d+(?:\.\d+)+)(?=$|[^\d])"""
    )

    fun deriveAesKey(seed: String = SEED): ByteArray {
        return md5(seed.toByteArray(StandardCharsets.UTF_8))
    }

    fun deriveIv(aesKey: ByteArray): ByteArray {
        return md5(aesKey)
    }

    fun decryptChange(change: String): String {
        val aesKey = deriveAesKey()
        val iv = deriveIv(aesKey)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(aesKey, "AES"),
            IvParameterSpec(iv)
        )

        val encryptedBytes = Base64.getDecoder().decode(change.trim())
        return String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
    }

    fun parseApiResponse(response: String): PgSharpRootVersionInfo {
        val encryptedChange = JSONObject(response).getString("change")
        return parseDecryptedJson(decryptChange(encryptedChange))
    }

    fun parseDecryptedJson(json: String): PgSharpRootVersionInfo {
        val root = JSONObject(json)
        // Live responses currently expose m/d directly; accept config.m/config.d too.
        val config = root.optJSONObject("config") ?: root
        val app = config.getJSONObject("m")
        val appName = app.getString("n")
        val appVersion = extractAppVersion(appName)
            ?: throw JSONException("PGSharp Root app version is missing from m.n")
        val downloadUrl = app.getString("u").trim()
        if (downloadUrl.isEmpty()) {
            throw JSONException("PGSharp Root APK URL is missing from m.u")
        }

        val supportedVersions = buildList {
            val versionConfig = config.getJSONObject("d")
            val versions = versionConfig.keys()
            while (versions.hasNext()) {
                val version = versions.next()
                val entry = versionConfig.optJSONObject(version) ?: continue
                if (entry.optBoolean("v", false)) {
                    add(version)
                }
            }
        }

        return PgSharpRootVersionInfo(
            appVersion = appVersion,
            downloadUrl = downloadUrl,
            changelog = app.optString("change"),
            supportedPogoVersions = supportedVersions
        )
    }

    fun extractAppVersion(appName: String): String? {
        return versionRegex.find(appName)?.groupValues?.get(1)
    }

    private fun md5(bytes: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(bytes)
    }
}
