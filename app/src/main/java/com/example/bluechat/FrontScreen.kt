package com.example.bluechat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FrontPageScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Automatically proceed after delay
    LaunchedEffect(Unit) {
        delay(1200)
        onGetStartedClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient overlay for modern effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer
            Spacer(modifier = Modifier.weight(0.5f))

            // Logo without border
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bluechat),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // App Name
            Text(
                text = "BlueChat",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Catchy sentence
            Text(
                text = "Connect Instantly, Chat Seamlessly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

            // Middle spacer
            Spacer(modifier = Modifier.weight(0.5f))

//            // Single larger button
//            Button(
//                onClick = onGetStartedClick,
//                modifier = Modifier
//                    .fillMaxWidth(0.8f)
//                    .height(56.dp)
//                    .clip(RoundedCornerShape(50)),
//                shape = RoundedCornerShape(28.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color.Transparent
//                ),
//                elevation = ButtonDefaults.buttonElevation(
//                    defaultElevation = 4.dp,
//                    pressedElevation = 8.dp
//                ),
//                contentPadding = PaddingValues(0.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .background(
//                            Brush.horizontalGradient(
//                                colors = listOf(
//                                    MaterialTheme.colorScheme.primary,
//                                    MaterialTheme.colorScheme.secondary
//                                )
//                            ),
//                            shape = RoundedCornerShape(28.dp)
//                        )
//                        .fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "Get Started",
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//            }

            // Bottom spacer
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Make sure to wrap your NavHost in a theme with dark colors
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF212121),
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}