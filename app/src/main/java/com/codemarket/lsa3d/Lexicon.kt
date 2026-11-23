package com.codemarket.lsa3d

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Normaliza texto igual para frases y para el léxico (minúsculas, sin tildes ni puntuación). */
fun normalizeText(s: String): String =
    Normalizer.normalize(s.lowercase(Locale("es", "AR")), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

object Lexicon {
    /** Lee assets/lexicon_lsa.json. Devuelve frase normalizada -> seña. */
    fun load(context: Context): Map<String, SignEntry> {
        val out = HashMap<String, SignEntry>()
        runCatching {
            val text = context.assets.open("lexicon_lsa.json").bufferedReader().use { it.readText() }
            val signs = JSONObject(text).getJSONArray("signs")
            for (i in 0 until signs.length()) {
                val o = signs.getJSONObject(i)
                val wordsArr = o.getJSONArray("words")
                val words = (0 until wordsArr.length()).map { wordsArr.getString(it) }
                val kfArr = o.getJSONArray("keyframes")
                val kfs = (0 until kfArr.length()).map { j ->
                    val k = kfArr.getJSONObject(j)
                    val p = k.optJSONObject("pose") ?: JSONObject()
                    fun f(name: String, def: Float) = p.optDouble(name, def.toDouble()).toFloat()
                    val d = Pose.REST
                    Keyframe(
                        k.getInt("t"),
                        Pose(
                            f("rPitch", d.rPitch), f("rRoll", d.rRoll), f("rElbow", d.rElbow),
                            f("lPitch", d.lPitch), f("lRoll", d.lRoll), f("lElbow", d.lElbow),
                            f("headYaw", d.headYaw)
                        )
                    )
                }.sortedBy { it.tMs }
                val entry = SignEntry(
                    id = o.getString("id"),
                    words = words,
                    validated = o.optBoolean("validated", false),
                    source = o.optString("source", ""),
                    keyframes = kfs
                )
                words.forEach { out[normalizeText(it)] = entry }
            }
        }
        return out
    }
}

object Planner {
    private const val MAX_PHRASE = 4

    /** Convierte texto en una lista de palabras con su seña (o null si no existe). */
    fun plan(text: String, lexicon: Map<String, SignEntry>): List<PlanItem> {
        val tokens = normalizeText(text).split(" ").filter { it.isNotEmpty() }
        val result = ArrayList<PlanItem>()
        var i = 0
        while (i < tokens.size) {
            var matched = false
            for (n in minOf(MAX_PHRASE, tokens.size - i) downTo 1) {
                val phrase = tokens.subList(i, i + n).joinToString(" ")
                val sign = lexicon[phrase]
                if (sign != null) {
                    result.add(PlanItem(phrase, sign)); i += n; matched = true; break
                }
            }
            if (!matched) { result.add(PlanItem(tokens[i], null)); i++ }
        }
        return result
    }
}

class Segment(val item: PlanItem, val startMs: Long, val endMs: Long)

class Timeline(val segments: List<Segment>, val totalMs: Long) {
    companion object {
        private const val GAP = 300L
        private const val MISSING = 700L

        fun build(items: List<PlanItem>): Timeline {
            var cursor = 0L
            val segs = items.map { item ->
                val dur = item.sign?.durationMs?.toLong() ?: MISSING
                val s = Segment(item, cursor, cursor + dur)
                cursor += dur + GAP
                s
            }
            return Timeline(segs, cursor)
        }
    }

    /** Pose en el instante t y el índice de la palabra activa (-1 si ninguna). */
    fun poseAt(t: Long): Pair<Pose, Int> {
        for ((idx, s) in segments.withIndex()) {
            if (t < s.startMs || t >= s.endMs + GAP) continue
            val sign = s.item.sign ?: return Pose.REST to idx
            val local = (t - s.startMs).toInt()
            val kfs = sign.keyframes
            if (kfs.isEmpty()) return Pose.REST to idx
            if (t >= s.endMs) {
                val back = ((t - s.endMs).toFloat() / GAP)
                return Pose.lerp(kfs.last().pose, Pose.REST, back) to idx
            }
            if (local <= kfs.first().tMs) return kfs.first().pose to idx
            for (k in 0 until kfs.size - 1) {
                val a = kfs[k]; val b = kfs[k + 1]
                if (local in a.tMs..b.tMs) {
                    val span = (b.tMs - a.tMs).coerceAtLeast(1)
                    return Pose.lerp(a.pose, b.pose, (local - a.tMs).toFloat() / span) to idx
                }
            }
            return kfs.last().pose to idx
        }
        return Pose.REST to -1
    }
}
