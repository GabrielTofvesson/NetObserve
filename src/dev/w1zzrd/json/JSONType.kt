package dev.w1zzrd.json

import java.math.BigDecimal

sealed class JSONType {
    class JSONArray(private val array: MutableList<JSONType> = ArrayList()): JSONType(), MutableList<JSONType> by array {
        override fun toString(): String {
            val builder = StringBuilder()

            builder.append('[')

            var isFirst = true
            for (value in this) {
                if (isFirst) isFirst = false
                else builder.append(',')

                builder.append(value.toString())
            }

            return builder.append(']').toString()
        }
    }

    class JSONObject(private val content: MutableMap<String, JSONType> = HashMap()): JSONType(), MutableMap<String, JSONType> by content {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append('{')

            var isFirst = true
            for ((key, value) in content) {
                if (!isFirst) builder.append(',')
                else isFirst = false
                builder.append(JSONValue.JSONString(key).toString()).append(':').append(value.toString())
            }

            return builder.append("}").toString()
        }
    }

    sealed class JSONValue<T>(val value: T): JSONType() {
        class JSONString(value: String): JSONValue<String>(value) {
            override fun toString() = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }

        class JSONNumber(value: BigDecimal): JSONValue<BigDecimal>(value) {
            override fun toString() = value.toString()
        }

        override fun equals(other: Any?) = value == other
        override fun hashCode() = value.hashCode()
    }

    abstract override fun toString(): String


    fun obj() = this as JSONObject
    fun arr() = this as JSONArray
    fun str() = this as JSONValue.JSONString
    fun num() = this as JSONValue.JSONNumber

    companion object {
        fun parse(content: String) = parseOrNull(content)!!

        private fun String.skipSpaces(from: Int): Int {
            for (index in from until length)
                if (!Character.isSpaceChar(this[index]))
                    return index
            return -1
        }

        private fun String.parseString(from: Int): String? {
            for (index in from + 1 until length)
                if (this[index] == '"' && this[index - 1] != '\\')
                    return substring(from + 1 until index)
            return null
        }

        private fun String.parseJSONObject(from: Int): Pair<JSONObject?, Int> {
            var current = skipSpaces(from + 1)

            if (current == -1)
                return null to 0

            if (this[current] == '}')
                return JSONObject() to current + 1

            val content = HashMap<String, JSONType>()
            do {
                if(this[current] == '"') {
                    // Read key
                    val key = parseString(current) ?: return null to 0

                    // Skip ':' delimiter
                    var after = skipSpaces(current + key.length + 2)
                    if (after == -1 || this[after] != ':')
                        return null to 0

                    after = skipSpaces(after + 1)
                    if (after == -1)
                        return null to 0

                    // Read value
                    val value = parseJSONType(after)
                    if (value.first == null)
                        return null to 0

                    content[key] = value.first!!


                    // Skip ',' (or find end of object)
                    current = skipSpaces(value.second)
                    if (this[current] == '}')
                        return JSONObject(content) to current + 1

                    if (this[current] != ',')
                        return null to 0

                    current = skipSpaces(current + 1)

                    // Improper placement of closing bracket
                    if (this[current] == '}')
                        return null to 0
                }
                else return null to 0
            } while (true)
        }

        private fun String.parseJSONArray(from: Int): Pair<JSONArray?, Int> {
            var current = skipSpaces(from + 1)

            val array = ArrayList<JSONType>()
            while (current < length && this[current] != ']') {
                val parsed = parseJSONType(current)
                if (parsed.first == null)
                    return null to 0

                array.add(parsed.first!!)

                current = skipSpaces(parsed.second)
                when {
                    current >= length -> return null to 0
                    current == length - 1 && this[current] != ']' -> return null to 0
                    this[current] == ']' -> return JSONArray(array) to current + 1
                    this[current] != ',' -> return null to 0
                }
                current = skipSpaces(current + 1)
            }

            return JSONArray(array) to current + 1
        }

        private fun String.parseJSONString(from: Int): Pair<JSONValue.JSONString?, Int> {
            val string = parseString(from)
            return if (string != null) JSONValue.JSONString(string) to string.length + from + 2
            else null to 0
        }

        private fun String.parseJSONNumber(from: Int): Pair<JSONValue.JSONNumber?, Int> {
            var hasDecimal = false
            for (index in from until length)
                if (this[index] == '-') {
                    if(index != from)
                        return null to 0
                }
                else if (this[index] == '.') {
                    if (hasDecimal) return null to 0
                    else hasDecimal = true
                }
                else if (!Character.isDigit(this[index]))
                    return if (index == from) null to 0
                    else JSONValue.JSONNumber(substring(from until index).toBigDecimal()) to index

            return JSONValue.JSONNumber(substring(from).toBigDecimal()) to length - 1
        }

        private fun String.parseJSONType(from: Int): Pair<JSONType?, Int> {
            val next = skipSpaces(from)
            if (next == -1)
                return null to 0

            return when(this[next]) {
                '[' -> parseJSONArray(next)
                '"' -> parseJSONString(next)
                '{' -> parseJSONObject(next)
                else -> parseJSONNumber(next)
            }
        }

        fun parseOrNull(content: String) = content.parseJSONType(0).first
    }
}