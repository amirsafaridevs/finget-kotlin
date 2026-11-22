package ir.finget.sdk

/** Base error for client-side validation failures. */
open class FingetException(message: String) : Exception(message)

/** HTTP or transport failure talking to the Finget API. */
class ApiException(
    message: String,
    val status: Int = 0,
    val errorCode: String? = null,
    val body: Map<String, Any?> = emptyMap(),
) : FingetException(message)
