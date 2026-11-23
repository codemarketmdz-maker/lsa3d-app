package com.codemarket.lsa3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Ángulos en grados. Brazo "der"/"izq" = derecha/izquierda DEL AVATAR. */
data class Pose(
    val rPitch: Float = 0f,   // elevar brazo hacia adelante/arriba (0 = colgando, 90 = al frente, 180 = arriba)
    val rRoll: Float = 8f,    // separar brazo hacia afuera
    val rElbow: Float = 10f,  // flexión del codo
    val lPitch: Float = 0f,
    val lRoll: Float = 8f,
    val lElbow: Float = 10f,
    val headYaw: Float = 0f   // giro de cabeza
) {
    companion object {
        val REST = Pose()

        fun lerp(a: Pose, b: Pose, t: Float): Pose {
            val k = t.coerceIn(0f, 1f)
            fun m(x: Float, y: Float) = x + (y - x) * k
            return Pose(
                m(a.rPitch, b.rPitch), m(a.rRoll, b.rRoll), m(a.rElbow, b.rElbow),
                m(a.lPitch, b.lPitch), m(a.lRoll, b.lRoll), m(a.lElbow, b.lElbow),
                m(a.headYaw, b.headYaw)
            )
        }
    }
}

data class Keyframe(val tMs: Int, val pose: Pose)

data class SignEntry(
    val id: String,
    val words: List<String>,
    /** true solo si la seña fue validada por personas sordas / intérpretes de LSA. */
    val validated: Boolean,
    val source: String,
    val keyframes: List<Keyframe>
) {
    val durationMs: Int get() = (keyframes.maxOfOrNull { it.tMs } ?: 0).coerceAtLeast(400)
}

data class PlanItem(val word: String, val sign: SignEntry?)

data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = V3(x * k, y * k, z * k)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun len() = sqrt(x * x + y * y + z * z)
    fun norm(): V3 {
        val l = len()
        return if (l < 1e-6f) this else V3(x / l, y / l, z / l)
    }
}

/** Puntos 3D del torso superior calculados por cinemática directa. */
class Skeleton(
    val shoulderR: V3, val shoulderL: V3,
    val hipR: V3, val hipL: V3,
    val elbowR: V3, val wristR: V3, val handR: V3,
    val elbowL: V3, val wristL: V3, val handL: V3,
    val head: V3, val nose: V3
)

object Kinematics {
    private fun rad(d: Float) = Math.toRadians(d.toDouble()).toFloat()
    private const val UPPER = 0.5f
    private const val FORE = 0.45f
    private const val HAND = 0.1f

    private fun arm(shoulder: V3, side: Float, pitch: Float, roll: Float, elbow: Float): Triple<V3, V3, V3> {
        val p = rad(pitch); val r = rad(roll); val e = rad(elbow)
        var d = V3(0f, -cos(p), sin(p))
        d = V3(side * sin(r) * (-d.y), d.y * cos(r), d.z).norm()
        var n = V3(0f, 0f, 1f) - d * V3(0f, 0f, 1f).dot(d)
        if (n.len() < 0.2f) n = V3(0f, 1f, 0f) - d * V3(0f, 1f, 0f).dot(d)
        val f = (d * cos(e) + n.norm() * sin(e)).norm()
        val elbowPos = shoulder + d * UPPER
        val wrist = elbowPos + f * FORE
        val hand = wrist + f * HAND
        return Triple(elbowPos, wrist, hand)
    }

    fun compute(pose: Pose, female: Boolean): Skeleton {
        val sw = if (female) 0.30f else 0.38f
        val hw = if (female) 0.20f else 0.22f
        val sR = V3(-sw, 1f, 0f); val sL = V3(sw, 1f, 0f)
        val (eR, wR, hR) = arm(sR, -1f, pose.rPitch, pose.rRoll, pose.rElbow)
        val (eL, wL, hL) = arm(sL, 1f, pose.lPitch, pose.lRoll, pose.lElbow)
        val head = V3(0f, 1.38f, 0f)
        val yaw = rad(pose.headYaw)
        val nose = head + V3(sin(yaw) * 0.2f, 0f, cos(yaw) * 0.2f)
        return Skeleton(sR, sL, V3(-hw, 0f, 0f), V3(hw, 0f, 0f), eR, wR, hR, eL, wL, hL, head, nose)
    }
}
