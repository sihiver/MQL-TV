package com.sihiver.mqltv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.theme.LocalClockFormat
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ExpiredRed = Color(0xFFFF2222)
private val ExpiredRedDim = Color(0xFFFF4444)
private val ExpiredRedSoft = Color(0xFFFF8888)
private val ExpiredBg = Color(0xFF080C12)

@Composable
fun ExpiredScreen(
    expiresAt: String? = null,
    contactNumber: String = "+62 xxx-xxxx-xxxx",
    onBack: () -> Unit,
    onLogout: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)

    val clockFormat = LocalClockFormat.current
    var clockText by remember { mutableStateOf("--:--:--") }
    val infinite = rememberInfiniteTransition(label = "expired")
    val iconScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "iconScale",
    )
    val badgeAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "badgeAlpha",
    )
    val topBarAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "topBarAlpha",
    )
    val tickerOffset by infinite.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "ticker",
    )

    LaunchedEffect(clockFormat) {
        val fmt = if (clockFormat == "12h") {
            DateTimeFormatter.ofPattern("hh:mm:ss a")
        } else {
            DateTimeFormatter.ofPattern("HH:mm:ss")
        }
        while (true) {
            clockText = LocalTime.now().format(fmt)
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ExpiredBg)) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF080C12), Color(0xFF0A0A0A), Color(0xFF100810))),
            ),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 60.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(ExpiredRed.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(ExpiredRed.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .alpha(topBarAlpha)
                .background(Brush.horizontalGradient(listOf(ExpiredRed, Color(0xFFFF6600), ExpiredRed))),
        )

        CornerDecor(Modifier.align(Alignment.TopStart).padding(30.dp))
        CornerDecor(Modifier.align(Alignment.TopEnd).padding(30.dp), topRight = true)
        CornerDecor(Modifier.align(Alignment.BottomStart).padding(30.dp), bottomLeft = true)
        CornerDecor(Modifier.align(Alignment.BottomEnd).padding(30.dp), bottomRight = true)

        Text(
            "IPTV SYSTEM",
            Modifier.align(Alignment.TopStart).padding(start = 60.dp, top = 32.dp),
            color = ExpiredRed.copy(alpha = 0.6f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
        )
        Text(
            clockText,
            Modifier.align(Alignment.TopEnd).padding(end = 60.dp, top = 36.dp),
            color = Color(0xFF444444),
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(160.dp).scale(iconScale), contentAlignment = Alignment.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .scale(1f + i * 0.15f)
                            .alpha(0.15f - i * 0.04f)
                            .border(3.dp, ExpiredRed.copy(alpha = 0.3f), CircleShape),
                    )
                }
                Canvas(Modifier.size(90.dp)) {
                    drawCircle(ExpiredRed.copy(alpha = 0.08f), size.minDimension / 2)
                    drawCircle(ExpiredRed, size.minDimension / 2, style = Stroke(3.dp.toPx()))
                    val cx = size.width / 2
                    drawLine(ExpiredRed, Offset(cx, size.height * 0.22f), Offset(cx, size.height * 0.61f), 6.dp.toPx())
                    drawCircle(ExpiredRed, 4.dp.toPx(), center = Offset(cx, size.height * 0.76f))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "● LAYANAN TIDAK AKTIF ●",
                modifier = Modifier.alpha(badgeAlpha),
                color = ExpiredRedDim,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(28.dp))
            Text("EXPIRED", fontSize = 110.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 8.sp)
            Text(
                "Langganan Berakhir",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = ExpiredRedDim,
                letterSpacing = 16.sp,
            )
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier
                    .width(600.dp)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, ExpiredRed, Color.Transparent))),
            )
            Spacer(Modifier.height(36.dp))
            Text(
                buildString {
                    append("Masa aktif langganan IPTV Anda telah habis.\n")
                    append("Silakan perpanjang untuk melanjutkan akses ke semua channel.")
                    if (!expiresAt.isNullOrBlank() && expiresAt != "—") append("\n\nBerakhir: $expiresAt")
                },
                color = Color(0xFFAAAAAA),
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier
                    .border(1.dp, ExpiredRed.copy(alpha = 0.2f))
                    .background(ExpiredRed.copy(alpha = 0.04f))
                    .padding(horizontal = 60.dp, vertical = 24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "HUBUNGI ADMIN",
                        Modifier.offset(y = (-34).dp).background(ExpiredBg).padding(horizontal = 16.dp),
                        color = ExpiredRedDim,
                        fontSize = 12.sp,
                        letterSpacing = 5.sp,
                    )
                    Text("Perpanjang sekarang — WhatsApp / Telegram", color = Color(0xFF888888), fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(contactNumber, color = ExpiredRedSoft, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                }
            }
            Spacer(Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TvFocusableBox(
                    onClick = onBack,
                    accentColor = ExpiredRed,
                    modifier = Modifier.border(1.dp, ExpiredRed.copy(alpha = 0.4f)).padding(horizontal = 28.dp, vertical = 14.dp),
                ) { _ ->
                    Text("← Kembali", color = ExpiredRedSoft, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
                if (onLogout != null) {
                    TvFocusableBox(
                        onClick = onLogout,
                        accentColor = ExpiredRed,
                        modifier = Modifier.border(1.dp, ExpiredRed.copy(alpha = 0.4f)).padding(horizontal = 28.dp, vertical = 14.dp),
                    ) { _ ->
                        Text("Keluar akun", color = ExpiredRedSoft, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Text(
            "ERR_SUBSCRIPTION_EXPIRED  |  CODE: 0x4E4F5355  |  SESSION TERMINATED",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp),
            color = Color(0xFF333333),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(ExpiredRed.copy(alpha = 0.08f))
                .border(1.dp, ExpiredRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            val ticker =
                "⚠ LANGGANAN ANDA TELAH BERAKHIR — HUBUNGI ADMIN UNTUK PERPANJANGAN     " +
                    "⚠ SUBSCRIPTION EXPIRED — PLEASE RENEW TO CONTINUE SERVICE     "
            Text(
                ticker + ticker,
                Modifier.offset(x = (tickerOffset * 1200).dp),
                color = ExpiredRedDim,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CornerDecor(
    modifier: Modifier = Modifier,
    topRight: Boolean = false,
    bottomLeft: Boolean = false,
    bottomRight: Boolean = false,
) {
    Canvas(modifier.size(80.dp)) {
        val stroke = 3.dp.toPx()
        val len = 80.dp.toPx()
        val color = ExpiredRed.copy(alpha = 0.6f)
        when {
            topRight -> {
                drawLine(color, Offset(size.width - len, 0f), Offset(size.width, 0f), stroke)
                drawLine(color, Offset(size.width, 0f), Offset(size.width, len), stroke)
            }
            bottomLeft -> {
                drawLine(color, Offset(0f, size.height - len), Offset(0f, size.height), stroke)
                drawLine(color, Offset(0f, size.height), Offset(len, size.height), stroke)
            }
            bottomRight -> {
                drawLine(color, Offset(size.width - len, size.height), Offset(size.width, size.height), stroke)
                drawLine(color, Offset(size.width, size.height - len), Offset(size.width, size.height), stroke)
            }
            else -> {
                drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke)
                drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke)
            }
        }
    }
}
