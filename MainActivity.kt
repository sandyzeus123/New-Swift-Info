package com.swiftcockpit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private val Ink = Color(0xFF080A0D)
private val Panel = Color(0xFF11161C)
private val Panel2 = Color(0xFF171E26)
private val Orange = Color(0xFFFF642D)
private val OrangeSoft = Color(0xFFFF8A50)
private val White = Color(0xFFF4F6F8)
private val Muted = Color(0xFF87919C)
private val Line = Color(0xFF29323C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        setContent { CockpitApp(this) }
    }
}

@Composable
private fun CockpitApp(context: Context) {
    var speedKmh by remember { mutableFloatStateOf(0f) }
    var gpsReady by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var clock by remember { mutableStateOf("--:--") }
    var date by remember { mutableStateOf("--") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            date = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now).uppercase(Locale.getDefault())
            delay(1000)
        }
    }

    DisposableEffect(permissionGranted) {
        if (permissionGranted) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                    gpsReady = true
                }
            }
            try {
                val provider = when {
                    manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }
                if (provider != null) {
                    @Suppress("MissingPermission")
                    manager.requestLocationUpdates(provider, 1000L, 0f, listener)
                    @Suppress("MissingPermission")
                    manager.getLastKnownLocation(provider)?.let {
                        speedKmh = if (it.hasSpeed()) it.speed * 3.6f else 0f
                        gpsReady = true
                    }
                }
            } catch (_: SecurityException) { }
            onDispose { manager.removeUpdates(listener) }
        } else {
            onDispose { }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Orange,
        onBackground = White, onSurface = White
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF10151B), Ink, Color(0xFF090B0E)))
        )) {
            // restrained technical grid / carbon-like texture
            Canvas(Modifier.fillMaxSize()) {
                val step = 34.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    for (y in 0..(size.height / step).toInt()) {
                        val px = x * step
                        val py = y * step
                        drawCircle(Color.White.copy(alpha = 0.018f), radius = 1.1f, center = Offset(px, py))
                    }
                }
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Orange.copy(alpha = .07f), Color.Transparent)),
                    topLeft = Offset(0f, size.height * .82f),
                    size = Size(size.width, size.height * .18f)
                )
            }

            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
                Header(clock, date, gpsReady)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpeedPanel(
                        speed = speedKmh,
                        gpsReady = gpsReady,
                        modifier = Modifier.weight(0.95f).fillMaxHeight()
                    )
                    NavigationPanel(
                        modifier = Modifier.weight(1.55f).fillMaxHeight(),
                        onMaps = { openMaps(context) }
                    )
                    RightRail(
                        modifier = Modifier.weight(0.82f).fillMaxHeight(),
                        onMusic = { openMusic(context) },
                        onApps = { openApps(context) },
                        onSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Footer(
                    gpsReady = gpsReady,
                    onEnableGps = {
                        if (!permissionGranted) permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )) else context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
            }
        }
    }
}

@Composable
private fun Header(clock: String, date: String, gpsReady: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(24.dp).clip(CircleShape).background(Orange))
                Spacer(Modifier.width(10.dp))
                Text("SWIFT", color = White, fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text(" / COCKPIT", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
            Text("DRIVER INTERFACE  •  V0.1", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.padding(start = 14.dp, top = 3.dp))
        }
        Spacer(Modifier.weight(1f))
        StatusPill(icon = if (gpsReady) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed, label = if (gpsReady) "GPS LOCK" else "GPS SEARCH")
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(clock, color = White, fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
            Text(date, color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun SpeedPanel(speed: Float, gpsReady: Boolean, modifier: Modifier = Modifier) {
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE SPEED", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (gpsReady) Orange else Muted))
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                SpeedGauge(speed = speed, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val display = animateFloatAsState(speed, tween(450, easing = FastOutSlowInEasing), label = "speed")
                    Text(display.value.toInt().toString().padStart(2, '0'), color = White, fontSize = 62.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                    Text("km/h", color = OrangeSoft, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SOURCE", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text("GPS", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SIGNAL", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text(if (gpsReady) "ACTIVE" else "WAITING", color = if (gpsReady) Orange else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SpeedGauge(speed: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = min(size.width, size.height)
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = side * .40f
        val stroke = side * .025f
        val start = 135f
        val sweep = 270f
        drawArc(Line, start, sweep, false, Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        val fraction = (speed / 220f).coerceIn(0f, 1f)
        if (fraction > 0f) drawArc(
            Brush.sweepGradient(listOf(Orange.copy(alpha = .35f), OrangeSoft, Orange, Orange.copy(alpha = .35f)), Offset(cx, cy)),
            start, sweep * fraction, false,
            Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2),
            style = Stroke(stroke * 1.2f, cap = StrokeCap.Round)
        )
        for (i in 0..40) {
            val angle = Math.toRadians((start + sweep * i / 40.0))
            val inner = radius - if (i % 5 == 0) stroke * 2.2f else stroke * 1.1f
            val outer = radius + stroke * .55f
            val x1 = cx + kotlin.math.cos(angle).toFloat() * inner
            val y1 = cy + kotlin.math.sin(angle).toFloat() * inner
            val x2 = cx + kotlin.math.cos(angle).toFloat() * outer
            val y2 = cy + kotlin.math.sin(angle).toFloat() * outer
            drawLine(if (i % 5 == 0) White.copy(alpha = .55f) else Muted.copy(alpha = .35f), Offset(x1, y1), Offset(x2, y2), if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx())
        }
    }
}

@Composable
private fun NavigationPanel(modifier: Modifier, onMaps: () -> Unit) {
    CockpitPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NAVIGATION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.NearMe, null, tint = Orange, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2730), Color(0xFF10171D), Color(0xFF172129))))
                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                    .clickable { onMaps() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    // abstract road-grid artwork; real turn-by-turn map opens in Maps
                    val roads = listOf(
                        listOf(Offset(w*.05f,h*.72f), Offset(w*.28f,h*.55f), Offset(w*.48f,h*.62f), Offset(w*.70f,h*.30f), Offset(w*.96f,h*.20f)),
                        listOf(Offset(w*.12f,h*.05f), Offset(w*.32f,h*.30f), Offset(w*.48f,h*.62f), Offset(w*.55f,h*.95f)),
                        listOf(Offset(w*.72f,h*.02f), Offset(w*.68f,h*.30f), Offset(w*.82f,h*.55f), Offset(w*.95f,h*.83f)),
                        listOf(Offset(w*.02f,h*.35f), Offset(w*.30f,h*.34f), Offset(w*.58f,h*.18f), Offset(w*.92f,h*.48f))
                    )
                    roads.forEachIndexed { index, points ->
                        for (i in 0 until points.lastIndex) drawLine(
                            if (index == 0) Orange.copy(alpha = .85f) else Color(0xFF46535E).copy(alpha = .55f),
                            points[i], points[i+1], if (index == 0) 3.dp.toPx() else 1.4.dp.toPx(), cap = StrokeCap.Round
                        )
                    }
                    drawCircle(Orange.copy(alpha = .18f), 17.dp.toPx(), Offset(w*.48f,h*.62f))
                    drawCircle(Orange, 5.dp.toPx(), Offset(w*.48f,h*.62f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Map, null, tint = White.copy(alpha = .9f), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("TAP TO OPEN MAPS", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Live navigation via installed map app", color = Muted, fontSize = 9.sp)
                }
                Text("MAP PREVIEW", color = Muted.copy(alpha = .65f), fontSize = 8.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("DESTINATION", color = Muted, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    Text("No active route", color = White, fontSize = 12.sp)
                }
                Button(
                    onClick = onMaps,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("NAVIGATE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun RightRail(modifier: Modifier, onMusic: () -> Unit, onApps: () -> Unit, onSettings: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitPanel(Modifier.weight(1f).fillMaxWidth().clickable { onMusic() }) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, null, tint = Orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MEDIA", color = Muted, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Your music", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open media player", color = Muted, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayCircleFilled, null, tint = Orange, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OPEN PLAYER", color = OrangeSoft, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        CockpitPanel(Modifier.weight(.82f).fillMaxWidth().clickable { onApps() }) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Filled.GridView, null, tint = Orange, modifier = Modifier.size(20.dp))
                Column {
                    Text("QUICK ACCESS", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Open installed apps", color = Muted, fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickGlyph(Icons.Filled.Map)
                    QuickGlyph(Icons.Filled.Phone)
                    QuickGlyph(Icons.Filled.Settings)
                }
            }
        }
        CockpitPanel(Modifier.height(58.dp).fillMaxWidth().clickable { onSettings() }) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, null, tint = Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Text("SYSTEM SETTINGS", color = White, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Orange)
            }
        }
    }
}

@Composable
private fun QuickGlyph(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF222B34)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Footer(gpsReady: Boolean, onEnableGps: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Orange))
            Spacer(Modifier.width(8.dp))
            Text("SWIFT VXI", color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("   /   DRIVER COCKPIT", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(if (gpsReady) "GPS DATA RECEIVING" else "GPS PERMISSION / SIGNAL REQUIRED", color = Muted, fontSize = 8.sp, letterSpacing = 1.1.sp)
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onEnableGps, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(if (gpsReady) "GPS SETTINGS" else "ENABLE GPS", color = OrangeSoft, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun StatusPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF1B232B)).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Orange, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = White, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CockpitPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A222B), Panel, Color(0xFF0E1318))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF3A4652), Color(0xFF202832), Color(0xFF303B45))), RoundedCornerShape(18.dp)),
        content = content
    )
}

private fun openMaps(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=")).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { context.startActivity(intent) }
    catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openMusic(context: Context) {
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) }
    catch (_: Exception) { openApps(context) }
}

private fun openApps(context: Context) {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
