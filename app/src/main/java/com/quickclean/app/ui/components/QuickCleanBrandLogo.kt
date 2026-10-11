package com.quickclean.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickclean.app.ui.theme.*

@Composable
fun QuickCleanBrandLogo(
    modifier: Modifier = Modifier,
    isLarge: Boolean = true,
    showSubheading: Boolean = true
) {
    if (isLarge) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing Luxury Emblem Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = CyanLaser)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D9488),
                                Color(0xFF0284C7),
                                Color(0xFF6366F1)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                CyanLaser,
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Secondary glow layer
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                // Icon Combination
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "SanSanRoom QuickClean Emblem",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unified Brand Name Typography "SanSanRoom"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SanSan",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Room",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanLaser,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "HOTEL & APARTMENT MANAGEMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.2.sp
            )

            if (showSubheading) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TealPrimary.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanLaser.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusClean)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "APARTEMEN GUNUNG PUTRI SQUARE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealContainer,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    } else {
        // Compact Version for Header / TopAppBar
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(TealPrimary, CyanLaser)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SanSan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Room",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanLaser
                    )
                }
                Text(
                    text = "Apartemen Gunung Putri",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TealContainer
                )
            }
        }
    }
}
