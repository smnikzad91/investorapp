package ir.devtrader.investor.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Positions/trades come back as raw, shape-not-guaranteed JSON — these read known fields opportunistically. */
fun JsonObject.stringField(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

fun JsonObject.firstNumberField(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.doubleOrNull }

fun JsonObject.rawString(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: this[key]?.toString() ?: "—"
