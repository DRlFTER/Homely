package com.homely.smarthome.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.homely.smarthome.core.model.AlertType
import com.homely.smarthome.core.model.Device
import com.homely.smarthome.core.model.DeviceSchedule
import com.homely.smarthome.core.model.DeviceStatus
import com.homely.smarthome.core.model.DeviceType
import com.homely.smarthome.core.model.Floor
import com.homely.smarthome.core.model.FloorValidation
import com.homely.smarthome.core.model.HomeAlert
import com.homely.smarthome.core.model.UsageEndReason
import com.homely.smarthome.core.model.UsageRecord
import com.homely.smarthome.core.model.isOn
import com.homely.smarthome.core.model.numberCapability
import com.homely.smarthome.core.model.switches
import com.homely.smarthome.core.model.timestampCapability
import com.homely.smarthome.core.ui.theme.HomelyTheme
import java.text.DateFormat
import kotlinx.coroutines.delay

private enum class Destination(val route: String, val label: String) {
    Home("home", "Home"),
    Schedules("schedules", "Schedules"),
    Reports("reports", "Reports"),
    Alerts("alerts", "Alerts"),
}

@Composable
fun HomelyApp(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.confirmationMessage) {
        val message = state.errorMessage ?: state.confirmationMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    HomelyTheme {
        if (state.loading) {
            LoadingScreen(state.errorMessage)
            return@HomelyTheme
        }

        BoxWithConstraints {
            val expandedNavigation = maxWidth >= 840.dp
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!expandedNavigation) {
                        AppNavigationBar(
                            navController = navController,
                            alertCount = state.unacknowledgedAlertCount,
                        )
                    }
                },
            ) { contentPadding ->
                Row(Modifier.fillMaxSize().padding(contentPadding)) {
                    if (expandedNavigation) {
                        AppNavigationRail(
                            navController = navController,
                            alertCount = state.unacknowledgedAlertCount,
                        )
                    }
                    NavHost(
                        navController = navController,
                        startDestination = Destination.Home.route,
                        modifier = Modifier.weight(1f),
                    ) {
                composable(Destination.Home.route) {
                    DashboardScreen(
                        state = state,
                        onFloorSelected = viewModel::selectFloor,
                        onDeviceSelected = viewModel::selectDevice,
                        onCameraOpen = { deviceId -> navController.navigate("camera/$deviceId") },
                        onPowerChanged = viewModel::setDevicePower,
                        onSwitchChanged = viewModel::setSwitch,
                        onSafetyDurationChanged = viewModel::setSafetyDuration,
                        onFloorSaved = viewModel::saveFloor,
                        onFloorDeleted = viewModel::deleteFloor,
                    )
                }
                composable(Destination.Schedules.route) {
                    SchedulesScreen(state = state, onSave = viewModel::saveSchedule)
                }
                composable(Destination.Reports.route) {
                    ReportsScreen(devices = state.devices, usage = state.usage)
                }
                composable(Destination.Alerts.route) {
                    AlertsScreen(
                        alerts = state.alerts,
                        devices = state.devices,
                        pendingIds = state.pendingIds,
                        onAcknowledge = viewModel::acknowledgeAlert,
                    )
                }
                composable("camera/{deviceId}") { entry ->
                    val deviceId = entry.arguments?.getString("deviceId").orEmpty()
                    CameraScreen(
                        device = state.devices.firstOrNull { it.id == deviceId },
                        onBack = navController::popBackStack,
                    )
                }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(error: String?) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (error == null) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Connecting to your home", style = MaterialTheme.typography.titleLarge)
                Text("Signing in and opening realtime device streams")
            } else {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Text("Unable to connect", style = MaterialTheme.typography.titleLarge)
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AppNavigationBar(navController: NavHostController, alertCount: Int) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Default.Info, Icons.Default.Notifications)
    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
        Destination.entries.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box {
                        Icon(icons[index], contentDescription = null)
                        if (destination == Destination.Alerts && alertCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-5).dp),
                            ) {
                                Text(
                                    text = alertCount.coerceAtMost(9).toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(navController: NavHostController, alertCount: Int) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Default.Info, Icons.Default.Notifications)
    NavigationRail(modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()) {
        Spacer(Modifier.height(12.dp))
        Destination.entries.forEachIndexed { index, destination ->
            NavigationRailItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box {
                        Icon(icons[index], contentDescription = null)
                        if (destination == Destination.Alerts && alertCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-5).dp),
                            ) {
                                Text(
                                    text = alertCount.coerceAtMost(9).toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: HomeUiState,
    onFloorSelected: (String) -> Unit,
    onDeviceSelected: (String?) -> Unit,
    onCameraOpen: (String) -> Unit,
    onPowerChanged: (Device, Boolean) -> Unit,
    onSwitchChanged: (Device, String, Boolean) -> Unit,
    onSafetyDurationChanged: (Device, Int) -> Unit,
    onFloorSaved: (Floor) -> Unit,
    onFloorDeleted: (String) -> Unit,
) {
    var showFloorManager by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Homely Demo House", style = MaterialTheme.typography.headlineMedium)
            Text("${state.devices.count { it.status == DeviceStatus.ON }} devices active · realtime")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.floors.forEach { floor ->
                        FilterChip(
                            selected = floor.id == state.selectedFloor?.id,
                            onClick = { onFloorSelected(floor.id) },
                            label = { Text(floor.name) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDeviceSelected(null)
                        showFloorManager = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage floors")
                }
            }
        }
        item {
            state.selectedFloor?.let { floor ->
                FloorPlan(
                    floor = floor,
                    devices = state.floorDevices,
                    selectedDeviceId = state.selectedDeviceId,
                    onDeviceSelected = onDeviceSelected,
                )
            }
        }
        item {
            HomeStatusStrip(
                online = state.devices.count { it.status != DeviceStatus.DISCONNECTED },
                alerts = state.unacknowledgedAlertCount,
                schedules = state.schedules.count { it.enabled },
            )
        }
    }

    if (showFloorManager) {
        ModalBottomSheet(onDismissRequest = { showFloorManager = false }) {
            FloorManagementSheet(
                floors = state.floors,
                devices = state.devices,
                pendingIds = state.pendingIds,
                onSave = onFloorSaved,
                onDelete = onFloorDeleted,
                onClose = { showFloorManager = false },
            )
        }
    }

    state.selectedDevice?.let { device ->
        ModalBottomSheet(onDismissRequest = { onDeviceSelected(null) }) {
            DeviceDetailSheet(
                device = device,
                pending = device.id in state.pendingIds,
                schedule = state.schedules.firstOrNull { it.deviceId == device.id },
                onCameraOpen = onCameraOpen,
                onPowerChanged = onPowerChanged,
                onSwitchChanged = onSwitchChanged,
                onSafetyDurationChanged = onSafetyDurationChanged,
            )
        }
    }
}

private const val NewFloorEditorId = "__new_floor__"

@Composable
private fun FloorManagementSheet(
    floors: List<Floor>,
    devices: List<Device>,
    pendingIds: Set<String>,
    onSave: (Floor) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<Floor?>(null) }
    val editingFloor = floors.firstOrNull { it.id == editingId }
    val isNewFloor = editingId == NewFloorEditorId

    if (editingId == null) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Manage floors", style = MaterialTheme.typography.headlineSmall)
                Text("Add a floor plan or update its grid before placing devices.")
            }
            item {
                Button(
                    onClick = { editingId = NewFloorEditorId },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add floor plan")
                }
            }
            if (floors.isEmpty()) {
                item {
                    Card {
                        Text(
                            "No floor plans yet. Add one to create the dashboard grid.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }
            items(floors, key = { it.id }) { floor ->
                val deviceCount = devices.count { it.floorId == floor.id }
                val canDelete = floors.size > 1 && deviceCount == 0 && floor.id !in pendingIds
                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(floor.name, style = MaterialTheme.typography.titleMedium)
                                Text("${floor.gridColumns} columns × ${floor.gridRows} rows")
                                if (floor.imageUrl.isNotBlank()) {
                                    Text("Background image configured", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            OutlinedButton(onClick = { editingId = floor.id }) {
                                Text("Edit")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    deviceCount > 0 -> "$deviceCount device${if (deviceCount == 1) "" else "s"} assigned"
                                    floors.size == 1 -> "Keep at least one floor"
                                    else -> "No devices assigned"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                enabled = canDelete,
                                onClick = { deleteCandidate = floor },
                            ) {
                                Text(if (canDelete) "Delete" else "Cannot delete")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    } else {
        FloorEditorForm(
            initial = editingFloor ?: Floor(
                id = if (isNewFloor) "" else editingId.orEmpty(),
                sortOrder = floors.size,
            ),
            pending = pendingIds.contains(
                FloorValidation.operationId(
                    editingFloor ?: Floor(id = if (isNewFloor) "" else editingId.orEmpty()),
                ),
            ),
            onBack = { editingId = null },
            onSave = { floor ->
                onSave(floor)
                editingId = null
            },
        )
    }

    deleteCandidate?.let { floor ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete ${floor.name}?") },
            text = { Text("This removes the floor plan from the home. Devices must be moved first.") },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(floor.id)
                        deleteCandidate = null
                    },
                ) {
                    Text("Delete")
                }
            },
        )
    }
}

@Composable
private fun FloorEditorForm(
    initial: Floor,
    pending: Boolean,
    onBack: () -> Unit,
    onSave: (Floor) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var imageUrl by remember(initial.id) { mutableStateOf(initial.imageUrl) }
    var rowsText by remember(initial.id) { mutableStateOf(initial.gridRows.toString()) }
    var columnsText by remember(initial.id) { mutableStateOf(initial.gridColumns.toString()) }
    var orderText by remember(initial.id) { mutableStateOf(initial.sortOrder.toString()) }

    val draft = Floor(
        id = initial.id,
        name = name,
        imageUrl = imageUrl,
        gridRows = rowsText.toIntOrNull() ?: -1,
        gridColumns = columnsText.toIntOrNull() ?: -1,
        sortOrder = orderText.toIntOrNull() ?: -1,
    )
    val validationError = FloorValidation.errorFor(draft)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    if (initial.id.isBlank()) "Add floor plan" else "Edit floor plan",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text("The grid controls where devices appear on this floor.")
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(FloorValidation.MAX_NAME_LENGTH + 1) },
                label = { Text("Floor name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it.take(FloorValidation.MAX_IMAGE_URL_LENGTH + 1) },
                label = { Text("Background image URL (optional)") },
                supportingText = { Text("Leave empty to use the built-in abstract plan.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = columnsText,
                    onValueChange = { columnsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Columns") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = rowsText,
                    onValueChange = { rowsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Rows") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it.filter(Char::isDigit).take(3) },
                label = { Text("Display order") },
                supportingText = { Text("Lower numbers appear first.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        validationError?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = { onSave(draft) },
                enabled = validationError == null && !pending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (pending) "Saving…" else "Save floor")
            }
        }
    }
}

@Composable
private fun FloorPlan(
    floor: Floor,
    devices: List<Device>,
    selectedDeviceId: String?,
    onDeviceSelected: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.82f).padding(12.dp),
        ) {
            val background = MaterialTheme.colorScheme.surfaceVariant
            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(background)) {
                drawFloorLayout(floor.imageUrl, lineColor)
                devices.filter(Device::isOn).forEach { device ->
                    val center = Offset(
                        x = size.width * ((device.gridX - .5f) / floor.gridColumns),
                        y = size.height * ((device.gridY - .5f) / floor.gridRows),
                    )
                    drawCircle(
                        color = Color(0xFFFFC45A).copy(alpha = 0.18f),
                        radius = size.minDimension * 0.16f,
                        center = center,
                    )
                }
                drawPlanGrid(floor.gridRows, floor.gridColumns, lineColor)
            }
            devices.forEach { device ->
                val x = maxWidth * ((device.gridX - .5f) / floor.gridColumns) - 24.dp
                val y = maxHeight * ((device.gridY - .5f) / floor.gridRows) - 24.dp
                DeviceMarker(
                    device = device,
                    selected = device.id == selectedDeviceId,
                    modifier = Modifier.offset(x = x, y = y),
                    onClick = { onDeviceSelected(device.id) },
                )
            }
        }
    }
}

private fun DrawScope.drawPlanGrid(rows: Int, columns: Int, lineColor: Color) {
    repeat(columns + 1) { column ->
        val x = size.width * column / columns
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    }
    repeat(rows + 1) { row ->
        val y = size.height * row / rows
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
}

/**
 * Draws the authored sample layout selected by the Firestore image URL.
 *
 * The Android client intentionally keeps these plans as vector geometry rather
 * than downloading a web-only SVG path. This keeps the dashboard useful while
 * offline and makes the grid/device overlay deterministic at every screen size.
 * Unknown or empty URLs retain the editable abstract plan fallback.
 */
private fun DrawScope.drawFloorLayout(imageUrl: String, outline: Color) {
    val scaleX = size.width / 1_200f
    val scaleY = size.height / 800f
    val strokeScale = (scaleX + scaleY) / 2f
    val wall = Color(0xFF756F63)
    val room = Color(0xFFF7F3EA)
    val background = if (imageUrl.contains("first-floor", ignoreCase = true)) {
        Color(0xFFE7E1D6)
    } else {
        Color(0xFFE9E3D7)
    }

    drawRect(background, size = size)

    fun rect(left: Float, top: Float, right: Float, bottom: Float) {
        val topLeft = Offset(left * scaleX, top * scaleY)
        val rectSize = Size((right - left) * scaleX, (bottom - top) * scaleY)
        drawRect(room, topLeft = topLeft, size = rectSize)
        drawRect(
            color = wall,
            topLeft = topLeft,
            size = rectSize,
            style = Stroke(width = 14f * strokeScale),
        )
    }

    if (imageUrl.contains("ground-floor", ignoreCase = true)) {
        rect(80f, 80f, 1_120f, 720f)
        rect(80f, 80f, 510f, 430f)
        rect(510f, 80f, 1_120f, 310f)
        rect(510f, 310f, 810f, 720f)
        rect(810f, 310f, 1_120f, 720f)
    } else if (imageUrl.contains("first-floor", ignoreCase = true)) {
        rect(80f, 80f, 1_120f, 720f)
        rect(80f, 80f, 590f, 480f)
        rect(590f, 80f, 1_120f, 480f)
        rect(80f, 480f, 440f, 720f)
        rect(440f, 480f, 1_120f, 720f)
    } else {
        drawAbstractPlan(outline)
    }
}

private fun DrawScope.drawAbstractPlan(outline: Color) {
    val wall = outline.copy(alpha = 0.78f)
    drawLine(wall, Offset(size.width * .08f, size.height * .08f), Offset(size.width * .92f, size.height * .08f), 8f)
    drawLine(wall, Offset(size.width * .08f, size.height * .08f), Offset(size.width * .08f, size.height * .92f), 8f)
    drawLine(wall, Offset(size.width * .92f, size.height * .08f), Offset(size.width * .92f, size.height * .92f), 8f)
    drawLine(wall, Offset(size.width * .08f, size.height * .92f), Offset(size.width * .92f, size.height * .92f), 8f)
    drawLine(wall, Offset(size.width * .50f, size.height * .08f), Offset(size.width * .50f, size.height * .70f), 7f)
    drawLine(wall, Offset(size.width * .08f, size.height * .52f), Offset(size.width * .35f, size.height * .52f), 7f)
    drawLine(wall, Offset(size.width * .65f, size.height * .43f), Offset(size.width * .92f, size.height * .43f), 7f)
}

@Composable
private fun DeviceMarker(device: Device, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val container = when (device.status) {
        DeviceStatus.ON -> MaterialTheme.colorScheme.primary
        DeviceStatus.ERROR -> MaterialTheme.colorScheme.error
        DeviceStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
        DeviceStatus.OFF -> MaterialTheme.colorScheme.surface
    }
    val content = when (device.status) {
        DeviceStatus.OFF, DeviceStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }
    val statusSymbol = when (device.status) {
        DeviceStatus.ERROR -> "!"
        DeviceStatus.DISCONNECTED -> "–"
        else -> device.type.marker
    }
    Surface(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = device.name
                stateDescription = device.status.name.lowercase().replaceFirstChar(Char::uppercase)
            }
            .clickable(onClick = onClick),
        color = container,
        contentColor = content,
        shape = CircleShape,
        tonalElevation = if (selected) 9.dp else 3.dp,
        border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.secondary)
            device.status == DeviceStatus.OFF -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            device.status == DeviceStatus.DISCONNECTED -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(statusSymbol, fontWeight = FontWeight.Black)
        }
    }
}

private val DeviceType.marker: String
    get() = when (this) {
        DeviceType.OUTLET -> "O"
        DeviceType.MULTI_SWITCH -> "M"
        DeviceType.LIGHT -> "L"
        DeviceType.SAFETY_OUTLET -> "S"
        DeviceType.CAMERA -> "C"
    }

@Composable
private fun HomeStatusStrip(online: Int, alerts: Int, schedules: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = "$online online  ·  $alerts alerts  ·  $schedules schedules active",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun DeviceDetailSheet(
    device: Device,
    pending: Boolean,
    schedule: DeviceSchedule?,
    onCameraOpen: (String) -> Unit,
    onPowerChanged: (Device, Boolean) -> Unit,
    onSwitchChanged: (Device, String, Boolean) -> Unit,
    onSafetyDurationChanged: (Device, Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text(device.type.marker, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleLarge)
                Text(device.type.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase))
            }
            AssistChip(onClick = {}, label = { Text(device.status.name) })
        }

        if (pending) LinearProgressIndicator(Modifier.fillMaxWidth())

        when (device.type) {
            DeviceType.CAMERA -> Button(onClick = { onCameraOpen(device.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open camera snapshot")
            }
            DeviceType.MULTI_SWITCH -> device.switches.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.label, modifier = Modifier.weight(1f))
                    Switch(
                        checked = item.isOn,
                        enabled = !pending,
                        onCheckedChange = { isOn ->
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            onSwitchChanged(device, item.id, isOn)
                        },
                    )
                }
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Power", style = MaterialTheme.typography.titleMedium)
                    Text(if (device.isOn) "Currently on" else "Currently off")
                }
                Switch(
                    checked = device.isOn,
                    enabled = !pending && device.status != DeviceStatus.DISCONNECTED,
                    onCheckedChange = { isOn ->
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onPowerChanged(device, isOn)
                    },
                )
            }
        }

        if (device.type == DeviceType.SAFETY_OUTLET) {
            SafetyControls(device, pending, onSafetyDurationChanged)
        }

        HorizontalDivider()
        DetailRow("Last update", device.updatedAt?.toDate()?.let(DateFormat.getDateTimeInstance()::format) ?: "Waiting")
        DetailRow("Updated by", device.updatedBy.ifBlank { "Unknown" })
        DetailRow("Automation", if (schedule?.enabled == true) "${schedule.startTime}–${schedule.endTime}" else "No active schedule")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SafetyControls(device: Device, pending: Boolean, onSafetyDurationChanged: (Device, Int) -> Unit) {
    var minutes by remember(device.id, device.numberCapability("maxOnDurationMinutes", 2)) {
        mutableIntStateOf(device.numberCapability("maxOnDurationMinutes", 2))
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Server-enforced cutoff", style = MaterialTheme.typography.titleMedium)
            SafetyCountdown(device)
            Text("Maximum duration: $minutes minutes")
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = it.toInt() },
                onValueChangeFinished = { onSafetyDurationChanged(device, minutes) },
                valueRange = 1f..30f,
                steps = 28,
                enabled = !pending,
            )
        }
    }
}

@Composable
private fun SafetyCountdown(device: Device) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(device.isOn) {
        while (device.isOn) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val startedAt = device.timestampCapability("onSince")?.toDate()?.time
    val maxMinutes = device.numberCapability("maxOnDurationMinutes", 2)
    val remaining = if (device.isOn && startedAt != null) {
        ((startedAt + maxMinutes * 60_000L - now) / 1_000L).coerceAtLeast(0)
    } else null
    Text(
        text = remaining?.let { "Cutoff in ${it / 60}:${(it % 60).toString().padStart(2, '0')}" }
            ?: "Timer starts when the outlet turns on",
        color = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(value, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SchedulesScreen(state: HomeUiState, onSave: (DeviceSchedule) -> Unit) {
    val schedulableDevices = state.devices.filter {
        it.type in setOf(DeviceType.LIGHT, DeviceType.OUTLET, DeviceType.SAFETY_OUTLET)
    }
    var selectedDeviceId by rememberSaveable { mutableStateOf("") }
    val selectedDevice = schedulableDevices.firstOrNull { it.id == selectedDeviceId }
        ?: schedulableDevices.firstOrNull()
    val existing = state.schedules.firstOrNull { it.deviceId == selectedDevice?.id }
    var enabled by remember(selectedDevice?.id, existing?.id) { mutableStateOf(existing?.enabled ?: false) }
    var startTime by remember(selectedDevice?.id, existing?.id) { mutableStateOf(existing?.startTime ?: "18:00") }
    var endTime by remember(selectedDevice?.id, existing?.id) { mutableStateOf(existing?.endTime ?: "22:00") }
    var selectedDays by remember(selectedDevice?.id, existing?.id) {
        mutableStateOf(existing?.daysOfWeek?.toSet() ?: setOf(1, 2, 3, 4, 5))
    }
    var safetyMinutes by remember(selectedDevice?.id, existing?.id) {
        mutableIntStateOf(existing?.maxOnDurationMinutes ?: 2)
    }
    val validTime = remember(startTime, endTime) {
        Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(startTime)
            && Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(endTime)
            && startTime != endTime
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("Schedules", style = MaterialTheme.typography.headlineMedium)
            Text("Automation runs in the home's Asia/Colombo time zone.")
        }
        item {
            Text("Device", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                schedulableDevices.forEach { device ->
                    FilterChip(
                        selected = device.id == selectedDevice?.id,
                        onClick = { selectedDeviceId = device.id },
                        label = { Text(device.name) },
                    )
                }
            }
        }
        selectedDevice?.let { device ->
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Run automatically", style = MaterialTheme.typography.titleMedium)
                                Text(if (enabled) "This schedule is active" else "Saved but disabled")
                            }
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it.take(5) },
                                label = { Text("Start (HH:mm)") },
                                isError = startTime.length == 5 && !Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(startTime),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it.take(5) },
                                label = { Text("End (HH:mm)") },
                                isError = endTime.length == 5 && !Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(endTime),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text("Days", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, day ->
                                val value = index + 1
                                FilterChip(
                                    selected = value in selectedDays,
                                    onClick = {
                                        selectedDays = if (value in selectedDays) selectedDays - value else selectedDays + value
                                    },
                                    label = { Text(day) },
                                )
                            }
                        }
                        if (device.type == DeviceType.SAFETY_OUTLET) {
                            Text("Safety cutoff: $safetyMinutes minutes", style = MaterialTheme.typography.titleSmall)
                            Slider(
                                value = safetyMinutes.toFloat(),
                                onValueChange = { safetyMinutes = it.toInt() },
                                valueRange = 1f..30f,
                                steps = 28,
                            )
                        }
                        Button(
                            onClick = {
                                onSave(
                                    DeviceSchedule(
                                        id = existing?.id.orEmpty(),
                                        deviceId = device.id,
                                        enabled = enabled,
                                        startTime = startTime,
                                        endTime = endTime,
                                        daysOfWeek = selectedDays.sorted(),
                                        maxOnDurationMinutes = safetyMinutes.takeIf { device.type == DeviceType.SAFETY_OUTLET },
                                    ),
                                )
                            },
                            enabled = validTime && selectedDays.isNotEmpty() && (existing?.id ?: device.id) !in state.pendingIds,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if ((existing?.id ?: device.id) in state.pendingIds) "Saving…" else "Save schedule")
                        }
                    }
                }
            }
        } ?: item {
            Card { Text("No schedulable devices are available.", modifier = Modifier.padding(20.dp)) }
        }
    }
}

@Composable
private fun ReportsScreen(devices: List<Device>, usage: List<UsageRecord>) {
    val totals = remember(usage) {
        usage.groupBy { it.deviceId }.mapValues { (_, records) -> records.sumOf { it.durationSeconds ?: 0 } }
    }
    val maximum = totals.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val completedSessions = usage.count { it.endedAt != null }
    val safetyCutoffs = usage.count { it.endedReason == UsageEndReason.SAFETY_CUTOFF }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Usage reports", style = MaterialTheme.typography.headlineMedium)
            Text("Completed sessions recorded by the backend")
        }
        item {
            Text(
                "$completedSessions completed sessions  ·  $safetyCutoffs safety stops",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Text("Active time by device", style = MaterialTheme.typography.titleLarge) }
        if (totals.isEmpty()) {
            item { Text("No completed usage sessions yet. Turn a device on and off to create the first record.") }
        }
        items(totals.entries.toList(), key = { it.key }) { (deviceId, seconds) ->
            val device = devices.firstOrNull { it.id == deviceId }
            UsageBar(
                name = device?.name ?: deviceId,
                seconds = seconds,
                fraction = seconds.toFloat() / maximum,
            )
        }
        item { Text("Recent sessions", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        items(usage.take(12), key = { it.id }) { record ->
            UsageRecordRow(record, devices.firstOrNull { it.id == record.deviceId }?.name ?: record.deviceId)
        }
    }
}

@Composable
private fun UsageBar(name: String, seconds: Int, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row {
            Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(durationLabel(seconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "usage")
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun UsageRecordRow(record: UsageRecord, deviceName: String) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = when (record.endedReason) {
                UsageEndReason.SAFETY_CUTOFF -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(deviceName, fontWeight = FontWeight.SemiBold)
                Text(record.endedReason?.name?.replace('_', ' ') ?: "IN PROGRESS")
            }
            Text(record.durationSeconds?.let(::durationLabel) ?: "Active")
        }
    }
}

@Composable
private fun AlertsScreen(
    alerts: List<HomeAlert>,
    devices: List<Device>,
    pendingIds: Set<String>,
    onAcknowledge: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Alerts", style = MaterialTheme.typography.headlineMedium)
            Text("Safety events remain here until you acknowledge them.")
        }
        if (alerts.isEmpty()) {
            item {
                Card { Text("No alerts yet. Your safety history will appear here.", modifier = Modifier.padding(20.dp)) }
            }
        }
        items(alerts, key = { it.id }) { alert ->
            AlertCard(
                alert = alert,
                deviceName = devices.firstOrNull { it.id == alert.deviceId }?.name,
                pending = alert.id in pendingIds,
                onAcknowledge = { onAcknowledge(alert.id) },
            )
        }
    }
}

@Composable
private fun AlertCard(alert: HomeAlert, deviceName: String?, pending: Boolean, onAcknowledge: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alert.acknowledged) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (alert.type == AlertType.SAFETY_CUTOFF) "Safety cutoff" else "Device error",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    deviceName?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                }
                if (alert.acknowledged) Text("Acknowledged", style = MaterialTheme.typography.labelSmall)
            }
            Text(alert.message)
            Text(
                alert.createdAt?.toDate()?.let(DateFormat.getDateTimeInstance()::format) ?: "Just now",
                style = MaterialTheme.typography.labelSmall,
            )
            if (!alert.acknowledged) {
                OutlinedButton(onClick = onAcknowledge, enabled = !pending) {
                    Text(if (pending) "Saving…" else "Acknowledge")
                }
            }
        }
    }
}

@Composable
private fun CameraScreen(device: Device?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(device?.name ?: "Camera", style = MaterialTheme.typography.titleLarge)
        }
        if (device == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Camera not found") }
            return@Column
        }
        Card(Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.45f).background(Color(0xFF9FB8B7))) {
                drawRect(Color(0xFF243B3A), topLeft = Offset(size.width * .12f, size.height * .35f), size = androidx.compose.ui.geometry.Size(size.width * .32f, size.height * .65f))
                drawRect(Color(0xFFE5DED0), topLeft = Offset(size.width * .48f, size.height * .25f), size = androidx.compose.ui.geometry.Size(size.width * .40f, size.height * .75f))
                drawLine(Color(0xFF596E50), Offset(0f, size.height * .82f), Offset(size.width, size.height * .76f), strokeWidth = size.height * .2f, cap = StrokeCap.Square)
                drawCircle(Color(0xFFE85A4F), radius = 8f, center = Offset(size.width - 28f, 28f))
            }
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mock snapshot", style = MaterialTheme.typography.headlineSmall)
            Text("This static frame represents the camera feed used in the university demonstration.")
            DetailRow(
                "Freshness",
                device.timestampCapability("lastSnapshotAt")?.toDate()?.let(DateFormat.getDateTimeInstance()::format)
                    ?: "Waiting for snapshot",
            )
            DetailRow("Stream", device.capabilities["streamUrl"] as? String ?: "Not available")
        }
    }
}

private fun durationLabel(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else -> "${seconds / 60}m ${seconds % 60}s"
}
