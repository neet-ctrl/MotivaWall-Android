package com.motivawall.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.motivawall.app.core.ImageEdits
import com.motivawall.app.core.HistoryTransfer
import com.motivawall.app.core.WallpaperTarget
import com.motivawall.app.data.WallpaperHistory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()
        setContent { MotivaWallApp() }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_IMAGES)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 42)
    }
}

private val Midnight = Color(0xFF0A0A1A)
private val SurfaceDark = Color(0xFF1A1A2E)
private val Purple = Color(0xFF6C63FF)
private val Coral = Color(0xFFFF6B6B)

@Composable
fun MotivaWallApp(viewModel: MainViewModel = hiltViewModel()) {
    val setup by viewModel.setup.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val schedules by viewModel.schedules.collectAsState(initial = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    var setupMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> rememberLocalUri(context, uri); viewModel.selectImage(uri); setupMode = true }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> rememberLocalUri(context, uri); viewModel.selectPdf(uri); setupMode = true }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(HistoryTransfer.encode(history).toByteArray()) } }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> viewModel.importHistory(reader.readText()) } }
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(setup.message) { setup.message?.let { scope.launch { snackbar.showSnackbar(it) } } }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Purple, secondary = Coral, background = Midnight, surface = SurfaceDark
    )) {
        Surface(Modifier.fillMaxSize(), color = Midnight) {
            Scaffold(
                containerColor = Midnight,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!setupMode) NavigationBar(containerColor = SurfaceDark) {
                        val items = listOf(
                            NavItem("Home", Icons.Default.Home),
                            NavItem("PDF", Icons.Default.Description),
                            NavItem("History", Icons.Default.Favorite),
                            NavItem("Settings", Icons.Default.Settings)
                        )
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            ) { padding ->
                AnimatedContent(setupMode to tab, modifier = Modifier.padding(padding), label = "page") { (editing, selected) ->
                    if (editing) SetupScreen(
                        state = setup,
                        onBack = { setupMode = false },
                        onEdits = viewModel::updateEdits,
                        onPage = viewModel::showPdfPage,
                        onInterval = { value ->
                            val millis = when (value) {
                                "3s" -> 3_000L
                                "5s" -> 5_000L
                                "10s" -> 10_000L
                                "30s" -> 30_000L
                                "1m" -> 60_000L
                                "5m" -> 300_000L
                                else -> 600_000L
                            }
                            viewModel.setInterval(millis)
                        },
                        onTarget = viewModel::setTarget,
                        onApply = viewModel::applyCurrent,
                        onOverlay = {
                            if (Settings.canDrawOverlays(context)) {
                                ContextCompat.startForegroundService(context, Intent(context, com.motivawall.app.service.PdfLockScreenDialogService::class.java))
                            } else context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        }
                    ) else when (selected) {
                        0 -> HomeScreen(history, schedules, onImage = { imagePicker.launch(arrayOf("image/*")) }, onPdf = { pdfPicker.launch(arrayOf("application/pdf")) }, onSetup = { setupMode = true })
                        1 -> PdfLanding(onPick = { pdfPicker.launch(arrayOf("application/pdf")) })
                        2 -> HistoryScreen(
                            history,
                            viewModel::toggleFavorite,
                            viewModel::delete,
                            viewModel::clearHistory,
                            { exportPicker.launch("motivawall-history.json") },
                            { importPicker.launch(arrayOf("application/json", "text/plain")) }
                        )
                        else -> SettingsScreen(schedules, history, viewModel::saveSchedule, viewModel::deleteSchedule)
                    }
                }
            }
        }
    }
}

private fun rememberLocalUri(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Some providers do not offer persistable grants; the current session still works.
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun HomeScreen(history: List<WallpaperHistory>, schedules: List<com.motivawall.app.data.WallpaperSchedule>, onImage: () -> Unit, onPdf: () -> Unit, onSetup: () -> Unit) {
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column {
                Text("MOTIVAWALL", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your screen.\nYour momentum.", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                Spacer(Modifier.height(8.dp))
                Text("Transform local images and PDFs into daily inspiration.", color = Color(0xFFB6B4C6), fontSize = 15.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Purple)
                        Spacer(Modifier.width(10.dp))
                        Text("Ready for your next reset?", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("Image", Icons.Default.Image, onImage, Modifier.weight(1f))
                        QuickAction("PDF", Icons.Default.Description, onPdf, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Text("At a glance", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("${history.size}", "Wallpapers", Modifier.weight(1f))
                StatCard("${history.count { it.isFavorite }}", "Favorites", Modifier.weight(1f))
                StatCard("${schedules.count { it.isActive }}", "Active plans", Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24213F)), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Purple.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, null, tint = Purple)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fine-tune a wallpaper", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Crop, color, quote, then set it.", color = Color(0xFFB6B4C6), fontSize = 13.sp)
                    }
                    IconButton(onClick = onSetup) { Icon(Icons.Default.Add, "Start setup", tint = Coral) }
                }
            }
        }
        if (history.isNotEmpty()) item {
            Text("Recently set", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                history.take(5).forEach { item ->
                    val bitmap = remember(item.thumbnailPath) { android.graphics.BitmapFactory.decodeFile(item.thumbnailPath) }
                    bitmap?.let { Image(it.asImageBitmap(), null, Modifier.size(110.dp, 150.dp).clip(RoundedCornerShape(18.dp))) }
                }
            }
        }
    }
}

@Composable
private fun PdfLanding(onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(Purple.copy(.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Description, null, tint = Purple, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("PDF wallpaper", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Turn a local PDF into a page-by-page lock screen.", color = Color(0xFFB6B4C6), fontSize = 15.sp)
        Spacer(Modifier.height(26.dp))
        Button(onClick = onPick, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("Choose a local PDF") }
    }
}

@Composable
private fun SetupScreen(
    state: SetupState,
    onBack: () -> Unit,
    onEdits: (ImageEdits) -> Unit,
    onPage: (Int) -> Unit,
    onInterval: (String) -> Unit,
    onTarget: (WallpaperTarget) -> Unit,
    onApply: () -> Unit,
    onOverlay: () -> Unit
) {
    val image = state.bitmap
    var edits by remember(state.source) { mutableStateOf(state.edits) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)) {
        item {
            TopAppBar(
                title = { Text(if (state.isPdf) "PDF setup" else "Wallpaper setup", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Midnight)
            )
        }
        item {
            Box(Modifier.fillMaxWidth().height(390.dp).padding(horizontal = 20.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF24213F)), contentAlignment = Alignment.Center) {
                image?.let { Image(it.asImageBitmap(), "Wallpaper preview", Modifier.fillMaxSize()) }
                    ?: Text(if (state.isPdf) "Loading PDF…" else "Choose an image to begin", color = Color(0xFFB6B4C6))
                if (state.isPdf && state.pdfPages > 0) {
                    Text("Page ${state.pdfPage + 1} / ${state.pdfPages}", Modifier.align(Alignment.BottomCenter).padding(14.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (state.isPdf && state.pdfPages > 0) item {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onPage(state.pdfPage - 1) }, enabled = state.pdfPage > 0) { Text("Previous") }
                LinearProgressIndicator((state.pdfPage + 1f) / state.pdfPages, Modifier.weight(1f).padding(horizontal = 8.dp), color = Purple)
                TextButton(onClick = { onPage(state.pdfPage + 1) }, enabled = state.pdfPage < state.pdfPages - 1) { Text("Next") }
            }
        }
        if (state.isPdf && state.pdfPages > 0) item {
            SectionTitle("Auto-rotation")
            ChipRow(listOf("3s", "5s", "10s", "30s", "1m", "5m", "10m"), when (state.intervalMs) {
                3_000L -> "3s"; 5_000L -> "5s"; 30_000L -> "30s"; 60_000L -> "1m"; 300_000L -> "5m"; 600_000L -> "10m"; else -> "10s"
            }, onInterval)
            SectionTitle("Page rotation")
            ChipRow(listOf("0°", "90°", "180°", "270°"), "${state.edits.rotation}°") {
                onEdits(state.edits.copy(rotation = it.removeSuffix("°").toInt()))
            }
        }
        if (!state.isPdf) {
            item { SectionTitle("Crop & transform") }
            item {
                ChipRow(listOf("Free", "16:9", "18:9", "20:9", "4:3", "1:1"), edits.ratio) {
                    edits = edits.copy(ratio = it); onEdits(edits)
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { edits = edits.copy(rotation = (edits.rotation + 90) % 360); onEdits(edits) }) { Text("Rotate") }
                    Button(onClick = { edits = edits.copy(flipX = !edits.flipX); onEdits(edits) }) { Text("Flip") }
                }
            }
            item { SectionTitle("Light & color") }
            item { AdjustSlider("Brightness", edits.brightness) { edits = edits.copy(brightness = it); onEdits(edits) } }
            item { AdjustSlider("Contrast", edits.contrast) { edits = edits.copy(contrast = it); onEdits(edits) } }
            item { AdjustSlider("Saturation", edits.saturation) { edits = edits.copy(saturation = it); onEdits(edits) } }
            item { AdjustSlider("Vignette", edits.vignette) { edits = edits.copy(vignette = it); onEdits(edits) } }
            item { SectionTitle("Quote overlay") }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedTextField(edits.quote, { edits = edits.copy(quote = it); onEdits(edits) }, Modifier.fillMaxWidth(), label = { Text("Quote") })
                    androidx.compose.material3.OutlinedTextField(edits.author, { edits = edits.copy(author = it); onEdits(edits) }, Modifier.fillMaxWidth(), label = { Text("Author") })
                    ChipRow(listOf("Top", "Center", "Bottom"), edits.textPosition) { edits = edits.copy(textPosition = it); onEdits(edits) }
                    ChipRow(listOf("Small", "Medium", "Large"), edits.textSize) { edits = edits.copy(textSize = it); onEdits(edits) }
                }
            }
        }
        item { SectionTitle("Apply to") }
        item {
            ChipRow(listOf("Home", "Lock", "Both"), when (state.target) { WallpaperTarget.HOME -> "Home"; WallpaperTarget.LOCK -> "Lock"; WallpaperTarget.BOTH -> "Both" }) {
                onTarget(when (it) { "Home" -> WallpaperTarget.HOME; "Lock" -> WallpaperTarget.LOCK; else -> WallpaperTarget.BOTH })
            }
        }
        item {
            Button(onClick = onApply, Modifier.fillMaxWidth().padding(20.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)) {
                Text("Set wallpaper", fontWeight = FontWeight.Bold)
            }
        }
        if (state.isPdf) item {
            TextButton(onClick = onOverlay, Modifier.fillMaxWidth()) { Text("Open lock-screen PDF controls") }
        }
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
@Composable private fun ChipRow(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(selected = selected == it, onClick = { onSelected(it) }, label = { Text(it) }) }
    }
}
@Composable private fun AdjustSlider(label: String, value: Int, onValue: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color(0xFFB6B4C6)); Text("$value%", color = Purple) }
        Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = 0f..100f)
    }
}
@Composable private fun QuickAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick, modifier.height(78.dp).then(modifier), colors = ButtonDefaults.buttonColors(containerColor = Purple.copy(.25f)), shape = RoundedCornerShape(18.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null); Text(text) }
    }
}
@Composable private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) { Text(value, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(label, color = Color(0xFFB6B4C6), fontSize = 11.sp) }
    }
}

@Composable
private fun HistoryScreen(
    history: List<WallpaperHistory>,
    onFavorite: (WallpaperHistory) -> Unit,
    onDelete: (WallpaperHistory) -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val visible = history.filter { filter == "All" || (filter == "PDF") == it.isPdf }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("History", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("${history.size} of 50 recent sets", color = Color(0xFFB6B4C6)) }
                Row {
                    TextButton(onClick = onImport) { Text("Import") }
                    TextButton(onClick = onExport) { Text("Export") }
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }
        item { ChipRow(listOf("All", "Image", "PDF"), filter) { filter = it } }
        items(visible, key = { it.id }) { item ->
            HistoryRow(item, onFavorite, onDelete)
        }
        if (visible.isEmpty()) item { Text("Nothing here yet. Set a local image or PDF to start your collection.", color = Color(0xFFB6B4C6), modifier = Modifier.padding(top = 40.dp)) }
    }
}

@Composable private fun HistoryRow(item: WallpaperHistory, onFavorite: (WallpaperHistory) -> Unit, onDelete: (WallpaperHistory) -> Unit) {
    val bitmap = remember(item.thumbnailPath) { android.graphics.BitmapFactory.decodeFile(item.thumbnailPath) }
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (item.isPdf) "PDF · page ${item.pdfPageNumber ?: 1}" else "Image wallpaper", color = Color.White, fontWeight = FontWeight.Bold)
                Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.dateSet)), color = Color(0xFFB6B4C6), fontSize = 12.sp)
            }
            IconButton(onClick = { onFavorite(item) }) { Icon(if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = Coral) }
            IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFB6B4C6)) }
        }
    }
}

@Composable
private fun SettingsScreen(
    schedules: List<com.motivawall.app.data.WallpaperSchedule>,
    history: List<WallpaperHistory>,
    onAddSchedule: (String, String, Long, String) -> Unit,
    onDeleteSchedule: (com.motivawall.app.data.WallpaperSchedule) -> Unit
) {
    var time by remember { mutableStateOf("08:00") }
    var days by remember { mutableStateOf("Daily") }
    var label by remember { mutableStateOf("Morning reset") }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Make the experience yours.", color = Color(0xFFB6B4C6)) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wallpaper schedule", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Schedules pause automatically below 15% battery.", color = Color(0xFFB6B4C6), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedTextField(time, { time = it.take(5) }, Modifier.weight(1f), label = { Text("Time") })
                        androidx.compose.material3.OutlinedTextField(days, { days = it }, Modifier.weight(1f), label = { Text("Days") })
                    }
                    androidx.compose.material3.OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Label") })
                    Button(
                        onClick = { history.firstOrNull()?.let { onAddSchedule(time, days, it.id, label) } },
                        enabled = history.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) { Text(if (history.isEmpty()) "Set a wallpaper first" else "Add schedule") }
                    schedules.forEach { schedule ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${schedule.time} · ${schedule.days}", color = Color.White)
                                Text(schedule.label, color = Color(0xFFB6B4C6), fontSize = 12.sp)
                            }
                            TextButton(onClick = { onDeleteSchedule(schedule) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
        item { SettingCard("Theme", "Dark midnight with dynamic accent", Icons.Default.Brightness6) }
        item { SettingCard("Schedules", "Wallpaper moments from your storage", Icons.Default.Tune) }
        item { SettingCard("Storage", "All source files stay on this device", Icons.Default.Home) }
        item { SettingCard("About MotivaWall", "Version 1.0.0 · No ads · No downloads", Icons.Default.AutoAwesome) }
    }
}
@Composable private fun SettingCard(title: String, subtitle: String, icon: ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(14.dp))
            Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFFB6B4C6), fontSize = 13.sp) }
        }
    }
}