package com.olivetrust.charity.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.ui.theme.OliveLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // Animation states
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(0.7f) }
        val translateY = remember { Animatable(50f) }
        
        LaunchedEffect(Unit) {
            // Animate splash content in with a nice sequence
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                alpha.animateTo(1f, animationSpec = tween(1000))
            }
            launch {
                translateY.animateTo(0f, animationSpec = tween(1000, easing = EaseOutCubic))
            }
            
            // Hold screen for a short delay
            delay(2500)
            
            // Navigate to Login Screen
            navigator.replace(LoginScreen())
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Background subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .alpha(alpha.value)
                    .scale(scale.value)
                    .offset(y = translateY.value.dp)
            ) {
                // Large Unified Olive Logo
                OliveLogo(size = 160.dp)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Olive Trust",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Empowering Communities",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 4.sp
                    )
                )
            }
            
            // Version or subtitle at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHARITY MANAGEMENT SYSTEM",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.alpha(alpha.value * 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(140.dp)
                        .height(4.dp)
                        .alpha(alpha.value * 0.4f),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            }
        }
    }
}
