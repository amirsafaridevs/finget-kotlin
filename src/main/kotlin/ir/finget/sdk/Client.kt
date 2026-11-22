package ir.finget.sdk

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/** Official Finget public API client (`/api/v1`). */
class Client(
    apiKey: String,
    baseUrl: String = DEFAULT_BASE_URL,
    authUrl: String = DEFAULT_AUTH_URL,
    private val timeoutMs: Int = 20_000,
) {
    private val apiKey: String = apiKey.trim().also {
        if (it.isEmpty()) throw FingetException("Finget API key is empty.")
    }
    private val baseUrl: String = baseUrl.trimEnd('/')
    private val authUrl: String = authUrl.trimEnd('/')

    init {
        if (timeoutMs < 1000) throw FingetException("Request timeout must be at least 1 second.")
    }

    companion object {
        const val VERSION = "1.0.0"
        const val DEFAULT_BASE_URL = "https://finget.ir/api/v1"
        const val DEFAULT_AUTH_URL = "https://finget.ir/auth"

        /** Build a client from environment variables (`FINGET_API_KEY`, optional base/auth URLs). */
        @JvmStatic
        @JvmOverloads
        fun fromEnv(apiKeyEnv: String = "FINGET_API_KEY"): Client {
            val key = System.getenv(apiKeyEnv)?.trim().orEmpty()
            if (key.isEmpty()) {
                throw FingetException("Environment variable $apiKeyEnv is not set.")
            }
            val base = System.getenv("FINGET_BASE_URL")?.takeIf { it.isNotBlank() }
            val auth = System.getenv("FINGET_AUTH_URL")?.takeIf { it.isNotBlank() }
            return Client(
                apiKey = key,
                baseUrl = base ?: DEFAULT_BASE_URL,
                authUrl = auth ?: DEFAULT_AUTH_URL,
            )
        }
    }

    /** Validate the API key and return site metadata. */
    fun getSite(): Map<String, Any?> = request("GET", "/site")

    /** Exchange a one-time callback code for `{user, isNew}`. */
    fun exchangeCode(code: String): Map<String, Any?> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) throw FingetException("Login code is empty.")
        return request("POST", "/auth/token", mapOf("code" to trimmed))
    }

    /** Fetch a linked user's current profile. */
    fun getUser(userId: String): Map<String, Any?> {
        val uid = userId.trim()
        if (uid.isEmpty()) throw FingetException("User id is empty.")
        return request("GET", "/users/${enc(uid)}")
    }

    /** Create a notification for linked users (expects HTTP 201). */
    fun sendNotification(payload: Map<String, Any?>): Map<String, Any?> =
        request("POST", "/notifications", payload, okStatuses = setOf(201))

    /** Fetch notification delivery status. */
    fun getNotification(notificationId: String): Map<String, Any?> {
        val nid = notificationId.trim()
        if (nid.isEmpty()) throw FingetException("Notification id is empty.")
        return request("GET", "/notifications/${enc(nid)}")
    }

    /** Return a cryptographically secure hex state string. */
    @JvmOverloads
    fun createState(nbytes: Int = 16): String {
        if (nbytes < 8) throw FingetException("State length is too short.")
        val bytes = ByteArray(nbytes)
        SecureRandom().nextBytes(bytes)
        val hex = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        bytes.forEachIndexed { idx, b ->
            val v = b.toInt() and 0xff
            hex[idx * 2] = digits[v ushr 4]
            hex[idx * 2 + 1] = digits[v and 0x0f]
        }
        return String(hex)
    }

    /**
     * Build the Sign-in-with-Finget redirect URL and CSRF state.
     * @return map with keys `url` and `state`
     */
    @JvmOverloads
    fun loginUrl(clientId: String, state: String? = null): Map<String, String> {
        val cid = clientId.trim()
        if (cid.isEmpty()) throw FingetException("Site client_id is empty.")
        val s = state ?: createState()
        val q = "client_id=${enc(cid)}&state=${enc(s)}"
        return mapOf("url" to "$authUrl?$q", "state" to s)
    }

    private fun request(
        method: String,
        path: String,
        body: Map<String, Any?>? = null,
        okStatuses: Set<Int> = setOf(200),
    ): Map<String, Any?> {
        val url = URI.create("$baseUrl$path").toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("User-Agent", "finget-kotlin/$VERSION")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                conn.outputStream.use { os ->
                    os.write(Json.stringify(body).toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.use { readAll(it) }.orEmpty()
            val payload = Json.parseObject(raw)
            if (status !in okStatuses) {
                val message = (payload["message"] as? String)
                    ?: "Finget request failed (HTTP $status)."
                val code = payload["code"] as? String
                throw ApiException(message, status, code, payload)
            }
            return payload
        } catch (e: ApiException) {
            throw e
        } catch (e: FingetException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(e.message ?: "Could not reach Finget.", 0)
        } finally {
            conn.disconnect()
        }
    }

    private fun readAll(stream: java.io.InputStream): String =
        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
