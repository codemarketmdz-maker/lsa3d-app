package com.codemarket.lsa3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

/**
 * Avatar 3D del torso superior dibujado con proyección en perspectiva.
 * Arrastrar con el dedo gira la cámara. Las manos son esferas: todavía no hay
 * configuración de dedos (hace falta un modelo con manos riggeadas y señas validadas).
 */
@Composable
fun Avatar3D(pose: Pose, female: Boolean, modifier: Modifier = Modifier) {
    var viewYaw by remember { mutableFloatStateOf(0f) }
    val skin = Color(0xFFE9B995)
    val cloth = if (female) Color(0xFF087E8B) else Color(0xFF2B4C7E)
    val clothDark = if (female) Color(0xFF056069) else Color(0xFF1D3557)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, drag ->
                change.consume()
                viewYaw = (viewYaw + drag.x * 0.4f).coerceIn(-70f, 70f)
            }
        }
    ) {
        val sk = Kinematics.compute(pose, female)
        val yaw = Math.toRadians(viewYaw.toDouble())
        val cy = cos(yaw).toFloat()
        val sy = sin(yaw).toFloat()
        val unit = size.height / 1.9f
        val cx = size.width / 2f
        val baseY = size.height * 0.93f
        val focal = 4f

        fun proj(p: V3): Offset {
            val x = p.x * cy + p.z * sy
            val z = -p.x * sy + p.z * cy
            val k = focal / (focal - z)
            return Offset(cx + x * unit * k, baseY - p.y * unit * k)
        }
        fun depth(p: V3) = -p.x * sy + p.z * cy
        fun scale(p: V3) = focal / (focal - depth(p))

        // Torso
        val torso = Path().apply {
            moveTo(proj(sk.shoulderR).x, proj(sk.shoulderR).y)
            lineTo(proj(sk.shoulderL).x, proj(sk.shoulderL).y)
            lineTo(proj(sk.hipL).x, proj(sk.hipL).y)
            lineTo(proj(sk.hipR).x, proj(sk.hipR).y)
            close()
        }
        drawPath(torso, cloth)
        drawLine(skin, proj(V3(0f, 1f, 0f)), proj(V3(0f, 1.2f, 0f)), strokeWidth = unit * 0.1f, cap = StrokeCap.Round)

        // Orden de dibujo: lo más lejano primero
        val arms = listOf(
            Triple(sk.shoulderR, sk.elbowR, sk.wristR) to sk.handR,
            Triple(sk.shoulderL, sk.elbowL, sk.wristL) to sk.handL
        ).sortedBy { depth(it.first.third) }
        for ((joints, hand) in arms) drawArm(joints.first, joints.second, joints.third, hand, ::proj, ::scale, unit, cloth, clothDark, skin)

        // Cabeza
        val hc = proj(sk.head)
        drawCircle(skin, radius = unit * 0.2f * scale(sk.head), center = hc)
        drawCircle(clothDark, radius = unit * 0.035f * scale(sk.nose), center = proj(sk.nose))
    }
}

private fun DrawScope.drawArm(
    shoulder: V3, elbow: V3, wrist: V3, hand: V3,
    proj: (V3) -> Offset, scale: (V3) -> Float, unit: Float,
    cloth: Color, clothDark: Color, skin: Color
) {
    drawLine(clothDark, proj(shoulder), proj(elbow), strokeWidth = unit * 0.1f * scale(elbow), cap = StrokeCap.Round)
    drawLine(cloth, proj(elbow), proj(wrist), strokeWidth = unit * 0.085f * scale(wrist), cap = StrokeCap.Round)
    drawLine(skin, proj(wrist), proj(hand), strokeWidth = unit * 0.07f * scale(hand), cap = StrokeCap.Round)
    drawCircle(skin, radius = unit * 0.075f * scale(hand), center = proj(hand))
}
