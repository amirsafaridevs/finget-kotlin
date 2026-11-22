package ir.finget.sdk

/**
 * Tiny JSON helper (objects/arrays/strings/numbers/booleans/null).
 * Enough for Finget API request/response maps — no third-party JSON library.
 */
internal object Json {
    fun stringify(value: Any?): String = buildString { write(value) }

    fun parseObject(raw: String): Map<String, Any?> {
        if (raw.isBlank()) return emptyMap()
        val parsed = Parser(raw).parseValue()
        @Suppress("UNCHECKED_CAST")
        return (parsed as? Map<String, Any?>) ?: emptyMap()
    }

    private fun StringBuilder.write(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value)
            is Number -> append(value)
            is String -> writeString(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                for ((k, v) in value) {
                    if (k !is String) continue
                    if (!first) append(',')
                    first = false
                    writeString(k)
                    append(':')
                    write(v)
                }
                append('}')
            }
            is Collection<*> -> {
                append('[')
                var first = true
                for (item in value) {
                    if (!first) append(',')
                    first = false
                    write(item)
                }
                append(']')
            }
            is Array<*> -> write(value.toList())
            else -> writeString(value.toString())
        }
    }

    private fun StringBuilder.writeString(s: String) {
        append('"')
        for (ch in s) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun parseValue(): Any? {
            skipWs()
            if (i >= s.length) return null
            val ch = s[i]
            return when {
                ch == '{' -> parseObject()
                ch == '[' -> parseArray()
                ch == '"' -> parseString()
                ch == 't' -> parseLiteral("true", true)
                ch == 'f' -> parseLiteral("false", false)
                ch == 'n' -> parseLiteral("null", null)
                ch == '-' || ch in '0'..'9' -> parseNumber()
                else -> throw FingetException("Invalid JSON at position $i")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val out = linkedMapOf<String, Any?>()
            skipWs()
            if (peek('}')) {
                i++
                return out
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                out[key] = parseValue()
                skipWs()
                when {
                    peek(',') -> {
                        i++
                    }
                    peek('}') -> {
                        i++
                        return out
                    }
                    else -> throw FingetException("Expected ',' or '}' in object")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val out = mutableListOf<Any?>()
            skipWs()
            if (peek(']')) {
                i++
                return out
            }
            while (true) {
                out += parseValue()
                skipWs()
                when {
                    peek(',') -> {
                        i++
                    }
                    peek(']') -> {
                        i++
                        return out
                    }
                    else -> throw FingetException("Expected ',' or ']' in array")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (i < s.length) {
                val ch = s[i++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) throw FingetException("Unterminated string escape")
                        when (val esc = s[i++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000c')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw FingetException("Invalid unicode escape")
                                val hex = s.substring(i, i + 4)
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> throw FingetException("Invalid escape \\$esc")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw FingetException("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = i
            if (peek('-')) i++
            while (i < s.length && s[i] in '0'..'9') i++
            var isDouble = false
            if (peek('.')) {
                isDouble = true
                i++
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (peek('e') || peek('E')) {
                isDouble = true
                i++
                if (peek('+') || peek('-')) i++
                while (i < s.length && s[i] in '0'..'9') i++
            }
            val raw = s.substring(start, i)
            return if (isDouble) raw.toDouble() else raw.toLong()
        }

        private fun parseLiteral(lit: String, value: Any?): Any? {
            if (!s.startsWith(lit, i)) throw FingetException("Expected $lit")
            i += lit.length
            return value
        }

        private fun expect(ch: Char) {
            skipWs()
            if (i >= s.length || s[i] != ch) throw FingetException("Expected '$ch'")
            i++
        }

        private fun peek(ch: Char): Boolean = i < s.length && s[i] == ch

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}
