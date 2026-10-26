package com.example.prayer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.util.Calendar

class FajrAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnScreenOnAndUnlock()

        val cityName = intent.getStringExtra("cityName") ?: "شهر شما"
        val offsetMinutes = intent.getIntExtra("offsetMinutes", 0)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FajrAlarmScreen(
                        cityName = cityName,
                        offsetMinutes = offsetMinutes,
                        onDismiss = {
                            FajrAlarmService.stopAlarm(this)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun FajrAlarmScreen(
    cityName: String,
    offsetMinutes: Int,
    onDismiss: () -> Unit
) {
    var currentTimeString by remember { mutableStateOf(getCurrentTimeString()) }
    var currentJalaliString by remember { mutableStateOf(getCurrentJalaliString()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeString = getCurrentTimeString()
            currentJalaliString = getCurrentJalaliString()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate Midnight
                        Color(0xFF1E1B4B), // Dawn Indigo
                        Color(0xFF311042)  // Dawn Purple
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "بیدارباش هوشمند اذان صبح",
                        color = Color(0xFFFBBF24),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = currentJalaliString,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Pulsing Alarm Icon & Large Clock
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .background(Color(0xFFE11D48).copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFFB7185),
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = currentTimeString,
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val subText = if (offsetMinutes > 0) {
                    "$offsetMinutes دقیقه مانده به اذان صبح به افق $cityName"
                } else {
                    "زمان اذان صبح به افق $cityName فرارسیده است"
                }

                Text(
                    text = subText,
                    color = Color(0xFFE2E8F0),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "وقت بیداری و مناجات سحرگاهی",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Dismiss Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AlarmOff,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "قطع زنگ بیدارباش",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

private fun getCurrentTimeString(): String {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)
    val second = now.get(Calendar.SECOND)
    val str = String.format(java.util.Locale.US, "%02d:%02d:%02d", hour, minute, second)
    return PersianCalendarHelper.toPersianDigits(str)
}

private fun getCurrentJalaliString(): String {
    val now = Calendar.getInstance()
    val jDate = PersianCalendarHelper.gregorianToJalali(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH)
    )
    val weekdayIdx = (now.get(Calendar.DAY_OF_WEEK) + 1) % 7
    val weekdayName = PersianCalendarHelper.WEEKDAY_NAMES_PERSIAN.getOrElse(weekdayIdx) { "" }
    return "$weekdayName، ${jDate.formattedPersian}"
}
