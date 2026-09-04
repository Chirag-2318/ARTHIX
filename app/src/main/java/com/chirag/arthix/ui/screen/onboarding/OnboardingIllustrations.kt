package com.chirag.arthix.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PastelBlush = Color(0xFFFFE5E5)
val PastelSage = Color(0xFFE5F9E0)
val PastelSky = Color(0xFFE5F0FF)
val PastelCream = Color(0xFFFFF9E5)
val PastelLavender = Color(0xFFF0E5FF)
val BrandCoral = Color(0xFFE4463A)
val TextNearBlack = Color(0xFF1A1A1A)

@Composable
fun OnboardingHeroVisual(step: OnboardingStep) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        when (step) {
            OnboardingStep.GESTURES -> ShakeToLogHero()
            OnboardingStep.NOTIFICATION_EXPLAINER -> NotificationAccessHero()
            OnboardingStep.BATTERY_OPTIMIZATION -> BackgroundAccessHero()
            OnboardingStep.SYSTEM_PERMISSION -> OverlayHero()
            OnboardingStep.CAMERA_MIC -> CameraMicHero()
            OnboardingStep.READY -> ReadyHero()
            else -> {}
        }
    }
}

@Composable
fun ShakeToLogHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Phone mockup
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(260.dp)
                .graphicsLayer { rotationZ = -12f }
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = BrandCoral, spotColor = BrandCoral)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(6.dp, Color(0xFFF0F0F5), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Vibration, contentDescription = null, modifier = Modifier.size(48.dp), tint = BrandCoral)
        }
        
        // Floating expense card
        Card(
            modifier = Modifier
                .offset(x = 40.dp, y = (-40).dp)
                .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("₹450 — Lunch", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NotificationAccessHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Back card
        Card(
            modifier = Modifier
                .width(220.dp)
                .offset(y = (-20).dp)
                .graphicsLayer { scaleX = 0.9f; scaleY = 0.9f; alpha = 0.6f }
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(PastelSky, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(80.dp).height(10.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(5.dp)))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.width(120.dp).height(8.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp)))
                }
            }
        }
        
        // Front card
        Card(
            modifier = Modifier
                .width(240.dp)
                .offset(y = 10.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = BrandCoral.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).background(PastelBlush, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Expense logged: ₹200", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 13.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Auto-captured from GPay", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun BackgroundAccessHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // App icon glowing
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = BrandCoral, spotColor = BrandCoral)
                .background(BrandCoral, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        
        // Faint overlapping icons in background to imply "other apps"
        Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color.LightGray, modifier = Modifier.offset(x = (-80).dp, y = (-60).dp).size(48.dp))
        Icon(Icons.Outlined.Map, contentDescription = null, tint = Color.LightGray, modifier = Modifier.offset(x = 80.dp, y = 50.dp).size(48.dp))
        Icon(Icons.Outlined.Camera, contentDescription = null, tint = Color.LightGray, modifier = Modifier.offset(x = (-70).dp, y = 70.dp).size(40.dp))
    }
}

@Composable
fun OverlayHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Wireframe of another app
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(280.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFF0F0F5), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp)))
            }
            
            // The floating bubble overlay
            Box(
                modifier = Modifier
                    .offset(x = 110.dp, y = 140.dp)
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, spotColor = BrandCoral.copy(alpha = 0.5f))
                    .background(BrandCoral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun CameraMicHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        // Receipt scan side
        Box(
            modifier = Modifier
                .offset(x = (-40).dp, y = (-20).dp)
                .width(100.dp)
                .height(140.dp)
                .graphicsLayer { rotationZ = -8f }
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .shadow(8.dp, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.LightGray))
                Box(modifier = Modifier.width(60.dp).height(4.dp).background(Color.LightGray))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.LightGray))
            }
            // Scanner line
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(BrandCoral).offset(y = 60.dp))
        }
        
        // Voice waveform side
        Box(
            modifier = Modifier
                .offset(x = 60.dp, y = (-10).dp)
                .size(72.dp)
                .background(PastelLavender, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(32.dp))
        }
        
        // Resulting card
        Card(
            modifier = Modifier
                .offset(y = 80.dp)
                .shadow(16.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ready to log", fontWeight = FontWeight.Bold, color = TextNearBlack, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ReadyHero() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = BrandCoral, spotColor = BrandCoral)
                .background(Color.White, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = BrandCoral, modifier = Modifier.size(72.dp))
        }
    }
}

@Composable
fun FloatingIconBubbles(icons: List<ImageVector>, colors: List<Color>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEachIndexed { index, icon ->
            val size = if (index % 2 == 0) 48.dp else 40.dp
            val offset = if (index > 0) (-12).dp else 0.dp
            val zIndex = icons.size - index.toFloat()
            
            Box(
                modifier = Modifier
                    .offset(x = offset * index)
                    .size(size)
                    .zIndex(zIndex)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f))
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(2.dp).background(colors[index % colors.size], CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextNearBlack.copy(alpha = 0.7f),
                        modifier = Modifier.size(size * 0.45f)
                    )
                }
            }
        }
    }
}
