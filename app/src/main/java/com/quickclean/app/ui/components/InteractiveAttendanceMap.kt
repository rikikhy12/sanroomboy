package com.quickclean.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.quickclean.app.data.api.MstAbsenItem
import com.quickclean.app.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveAttendanceMap(
    userLat: Double,
    userLong: Double,
    targetLat: Double,
    targetLong: Double,
    targetName: String,
    radiusMeters: Double = 50.0,
    isWithinRadius: Boolean = true,
    allLocations: List<MstAbsenItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var mapType by remember { mutableStateOf("streets") } // "streets" (Google Standard) or "satellite" (Google Hybrid)

    // Google Maps Tile URLs (100% Reliable, Fast, and Never Blocked in Indonesia)
    val tileUrl = if (mapType == "satellite") {
        "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"
    } else {
        "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
    }

    val circleColor = if (isWithinRadius) "#10B981" else "#EF4444"

    val mapHtml = remember(userLat, userLong, targetLat, targetLong, targetName, radiusMeters, isWithinRadius, mapType) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                * { box-sizing: border-box; }
                body, html { margin: 0; padding: 0; height: 100%; width: 100%; background: #0f172a; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; }
                #map { height: 100%; width: 100%; }
                
                /* Super Prominent (Pantaang) User Pin */
                .user-pin-wrap {
                    position: relative;
                    width: 36px;
                    height: 36px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .user-pulse {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    background: rgba(0, 245, 255, 0.45);
                    animation: pulseWave 1.4s infinite ease-out;
                }
                .user-core {
                    width: 16px;
                    height: 16px;
                    border-radius: 50%;
                    background: #00f5ff;
                    border: 3px solid #ffffff;
                    box-shadow: 0 0 10px #00f5ff, 0 2px 6px rgba(0,0,0,0.5);
                    z-index: 2;
                }
                .user-tag {
                    position: absolute;
                    top: -24px;
                    background: #0f172a;
                    color: #00f5ff;
                    font-size: 10px;
                    font-weight: 800;
                    padding: 2px 6px;
                    border-radius: 6px;
                    border: 1px solid #00f5ff;
                    white-space: nowrap;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.5);
                }

                /* Station Pin */
                .station-pin {
                    background: $circleColor;
                    color: #ffffff;
                    padding: 5px 10px;
                    border-radius: 12px;
                    font-size: 11px;
                    font-weight: 800;
                    border: 2px solid #ffffff;
                    box-shadow: 0 3px 8px rgba(0,0,0,0.45);
                    white-space: nowrap;
                    display: inline-block;
                }
                
                @keyframes pulseWave {
                    0% { transform: scale(0.4); opacity: 0.9; }
                    100% { transform: scale(1.6); opacity: 0; }
                }
                .leaflet-control-attribution { display: none !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                });

                // Google Maps Tiles (Always reliable)
                L.tileLayer('$tileUrl', {
                    maxZoom: 20,
                    subdomains: ['mt0', 'mt1', 'mt2', 'mt3']
                }).addTo(map);

                // User Location Marker (Pantaang & Menyala)
                var userIcon = L.divIcon({
                    className: 'user-pin-container',
                    html: '<div class="user-pin-wrap"><div class="user-pulse"></div><div class="user-core"></div><div class="user-tag">📍 ANDA</div></div>',
                    iconSize: [36, 36],
                    iconAnchor: [18, 18]
                });
                var userMarker = L.marker([$userLat, $userLong], { icon: userIcon }).addTo(map);

                // Target Station Marker
                var stationIcon = L.divIcon({
                    className: 'station-pin-container',
                    html: '<div class="station-pin">🏢 $targetName</div>',
                    iconAnchor: [40, 18]
                });
                var stationMarker = L.marker([$targetLat, $targetLong], { icon: stationIcon }).addTo(map);

                // 50m Radius Circle around Target Station
                var radiusCircle = L.circle([$targetLat, $targetLong], {
                    color: '$circleColor',
                    fillColor: '$circleColor',
                    fillOpacity: 0.25,
                    radius: $radiusMeters,
                    weight: 2.5,
                    dashArray: '6, 6'
                }).addTo(map);

                // Measurement line
                var latlngs = [[$userLat, $userLong], [$targetLat, $targetLong]];
                var polyline = L.polyline(latlngs, {color: '#ffffff', weight: 2, dashArray: '5, 8', opacity: 0.8}).addTo(map);

                // Auto-fit bounds so both User and Station are 100% visible
                var bounds = L.latLngBounds([[$userLat, $userLong], [$targetLat, $targetLong]]);
                map.fitBounds(bounds, { padding: [45, 45], maxZoom: 18 });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        webViewRef = this
                        loadDataWithBaseURL("https://www.google.com", mapHtml, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://www.google.com", mapHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Map Layer Switcher (Peta Jalan vs Satelit Google)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandDark.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mapType == "streets") TealPrimary else Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Jalan",
                            color = if (mapType == "streets") Color.White else Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { mapType = "streets" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (mapType == "satellite") TealPrimary else Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Satelit",
                            color = if (mapType == "satellite") Color.White else Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { mapType = "satellite" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Legend Overlay (Posisi Anda & Radius 50M)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BrandDark.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00F5FF))
                    )
                    Text("Posisi Anda", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("•", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isWithinRadius) StatusClean else Color(0xFFEF4444))
                    )
                    Text("Radius 50m", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
