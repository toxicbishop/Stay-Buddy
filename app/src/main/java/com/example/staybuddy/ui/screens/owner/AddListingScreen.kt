package com.example.staybuddy.ui.screens.owner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.staybuddy.ui.components.mouseWheelScroll
import com.mapbox.geojson.Point

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    onNavigateBack: () -> Unit,
    onListingAdded: () -> Unit,
    viewModel: AddListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onListingAdded()
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            if (uiState.currentStep == 1) onNavigateBack() else viewModel.previousStep() 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isEditing) "Edit Property" else "List Property",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Premium Progress Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 1..5) {
                            val isActive = i <= uiState.currentStep
                            val isCurrent = i == uiState.currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    .animateContentSize()
                            )
                        }
                    }
                    
                    Text(
                        text = "Step ${uiState.currentStep} of 5",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.currentStep > 1) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            modifier = Modifier.weight(0.4f).height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (uiState.currentStep < 5) viewModel.nextStep() else viewModel.submitListing() 
                        },
                        modifier = Modifier.weight(if (uiState.currentStep > 1) 0.6f else 1f).height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !uiState.isLoading,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                        } else {
                            Text(
                                if (uiState.currentStep < 5) "Continue" else if (uiState.isEditing) "Save Changes" else "Publish Listing",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
                .mouseWheelScroll(scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (uiState.currentStep) {
                1 -> Step1BasicInfo(uiState, viewModel)
                2 -> Step2PricingDetails(uiState, viewModel)
                3 -> Step3Amenities(uiState, viewModel)
                4 -> Step4Photos(uiState, viewModel)
                5 -> Step5Location(uiState, viewModel)
            }
        }
    }
}

@Composable
fun Step1BasicInfo(uiState: AddListingUiState, viewModel: AddListingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "Tell us about your space",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { viewModel.updateField("name", it) },
            label = { Text("Property Title") },
            placeholder = { Text("E.g. Modern Studio near University") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )
        
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateField("description", it) },
            label = { Text("Property Description") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            minLines = 5,
            placeholder = { Text("Describe the surroundings, rules, and what makes it great...") }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step2PricingDetails(uiState: AddListingUiState, viewModel: AddListingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "Pricing & Audience",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.monthlyRent,
                        onValueChange = { viewModel.updateField("monthlyRent", it) },
                        label = { Text("Monthly Rent (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = uiState.depositAmount,
                        onValueChange = { viewModel.updateField("depositAmount", it) },
                        label = { Text("Deposit (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Property Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val types = listOf("Private Room", "Shared Room", "Entire Flat")
                types.forEach { type ->
                    val isSelected = uiState.roomType == type
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateField("roomType", type) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = type,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Preferred Tenant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val genders = listOf("Boys", "Girls", "Any")
                genders.forEach { gender ->
                    val isSelected = uiState.genderAllowed == gender
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateField("genderAllowed", gender) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = gender,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3Amenities(uiState: AddListingUiState, viewModel: AddListingViewModel) {
    val categories = remember {
        listOf(
            "Essentials & Comfort" to listOf(
                "WiFi", "AC", "Power Backup", "Food Included", "Laundry Service", "Daily Cleaning"
            ),
            "Room & Appliances" to listOf(
                "Attached Washroom", "Geyser / Hot Water", "Smart TV", "Refrigerator", "Water Purifier", "Washing Machine"
            ),
            "Building & Safety" to listOf(
                "CCTV Security", "Parking", "Elevator / Lift", "Gym / Fitness", "24x7 Warden", "Self Cooking Kitchen"
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Amenities & Perks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Select all facilities available at your PG. Listings with 6+ amenities receive 3x more inquiry leads.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        categories.forEach { (categoryName, amenityList) ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    amenityList.forEach { amenity ->
                        val isSelected = uiState.amenities.contains(amenity)
                        val icon = getAmenityIcon(amenity)

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleAmenity(amenity) },
                            label = {
                                Text(
                                    text = amenity,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun getAmenityIcon(amenity: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = amenity.lowercase().trim()
    return when {
        lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") -> Icons.Default.Wifi
        lower.contains("power") || lower.contains("backup") || lower.contains("generator") -> Icons.Default.ElectricBolt
        lower.contains("washroom") || lower.contains("bathroom") || lower.contains("bath") || lower.contains("toilet") -> Icons.Default.Bathtub
        lower.contains("clean") || lower.contains("housekeeping") || lower.contains("maid") -> Icons.Default.CleaningServices
        lower.contains("food") || lower.contains("mess") || lower.contains("meal") || lower.contains("breakfast") || lower.contains("dinner") -> Icons.Default.Restaurant
        lower.contains("laundry") || lower.contains("washing") -> Icons.Default.LocalLaundryService
        lower.contains("geyser") || lower.contains("hot") || lower.contains("heater") -> Icons.Default.HotTub
        lower.contains("fridge") || lower.contains("refrigerator") -> Icons.Default.Kitchen
        lower.contains("water") || lower.contains("purifier") || lower.contains("ro") -> Icons.Default.WaterDrop
        lower.contains("cctv") || lower.contains("security") || lower.contains("warden") || lower.contains("guard") -> Icons.Default.Security
        lower.contains("parking") -> Icons.Default.DirectionsCar
        lower.contains("lift") || lower.contains("elevator") -> Icons.Default.Elevator
        lower.contains("gym") || lower.contains("fitness") -> Icons.Default.FitnessCenter
        lower.contains("kitchen") -> Icons.Default.SoupKitchen
        Regex("\\b(ac|air conditioner|air conditioning)\\b").containsMatchIn(lower) -> Icons.Default.AcUnit
        Regex("\\b(tv|television)\\b").containsMatchIn(lower) -> Icons.Default.Tv
        else -> Icons.Default.CheckCircle
    }
}

@Composable
fun Step4Photos(uiState: AddListingUiState, viewModel: AddListingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Property Showcase",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Add at least 3 high-quality photos. Better photos lead to 2x more inquiries.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            viewModel.addImages(uris)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable { launcher.launch("image/*") },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Upload Photos", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // 3-Column LazyVerticalGrid with 2D Drag & Drop Reordering (Left, Right, Up, Down)
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        if (uiState.imageUris.isNotEmpty() || uiState.existingImageUrls.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Selected Gallery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "First photo is automatically the Main Cover Photo. Hold & drag in any direction to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val allImages = buildList<Any> {
                addAll(uiState.existingImageUrls)
                addAll(uiState.imageUris)
            }
            
            var draggingIndex by remember { mutableStateOf<Int?>(null) }
            var accumulatedDx by remember { mutableStateOf(0f) }
            var accumulatedDy by remember { mutableStateOf(0f) }
            val swapThresholdPx = 90f

            val gridHeight = ((allImages.size + 2) / 3 * 125).dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(
                    items = allImages,
                    key = { index, item -> item.toString() }
                ) { globalIndex, item ->
                    val isBeingDragged = draggingIndex == globalIndex
                    val isCoverPhoto = globalIndex == 0
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isBeingDragged) 1.08f else 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        ),
                        label = "imageScale"
                    )
                    val elevation = if (isBeingDragged) 12.dp else if (isCoverPhoto) 4.dp else 2.dp

                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .scale(scale)
                            .pointerInput(globalIndex, allImages.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingIndex = globalIndex
                                        accumulatedDx = 0f
                                        accumulatedDy = 0f
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        accumulatedDx = 0f
                                        accumulatedDy = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        accumulatedDx = 0f
                                        accumulatedDy = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                        val total = allImages.size

                                        accumulatedDx += dragAmount.x
                                        accumulatedDy += dragAmount.y

                                        var targetIndex = currentIndex

                                        // Horizontal Swaps (±1)
                                        if (accumulatedDx > swapThresholdPx) {
                                            if (currentIndex < total - 1) {
                                                targetIndex = currentIndex + 1
                                                accumulatedDx -= swapThresholdPx
                                            }
                                        } else if (accumulatedDx < -swapThresholdPx) {
                                            if (currentIndex > 0) {
                                                targetIndex = currentIndex - 1
                                                accumulatedDx += swapThresholdPx
                                            }
                                        }

                                        // Vertical Swaps (±3 in 3-column grid)
                                        if (accumulatedDy > swapThresholdPx) {
                                            if (currentIndex + 3 < total) {
                                                targetIndex = currentIndex + 3
                                                accumulatedDy -= swapThresholdPx
                                            }
                                        } else if (accumulatedDy < -swapThresholdPx) {
                                            if (currentIndex - 3 >= 0) {
                                                targetIndex = currentIndex - 3
                                                accumulatedDy += swapThresholdPx
                                            }
                                        }

                                        if (targetIndex != currentIndex) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.swapImages(currentIndex, targetIndex)
                                            draggingIndex = targetIndex
                                        }
                                    }
                                )
                            },
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = elevation,
                        border = when {
                            isBeingDragged -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                            isCoverPhoto -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                            else -> null
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Property Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Automatic COVER PHOTO Badge for #1 item
                            if (isCoverPhoto) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "COVER PHOTO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Delete Image Icon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clickable { 
                                        if (item is Uri) viewModel.removeImage(item)
                                        else if (item is String) viewModel.removeExistingImage(item)
                                    }
                                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
                                    .size(26.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Remove", 
                                    tint = Color.White, 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step5Location(uiState: AddListingUiState, viewModel: AddListingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Final Step: Location",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Pin the exact location so students can find you easily.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp, max = 450.dp)
                .height((configuration.screenHeightDp * 0.45f).dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 4.dp
        ) {
            com.example.staybuddy.ui.components.LocationPicker(
                initialLocation = if (uiState.latitude != 0.0) com.mapbox.geojson.Point.fromLngLat(uiState.longitude, uiState.latitude) else null,
                onLocationSelected = { point ->
                    viewModel.updateLocation(point.latitude(), point.longitude())
                }
            )
        }

        if (uiState.latitude != 0.0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Location Pinned Successfully!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    // Show auto-detected city and area
                    if (uiState.city.isNotBlank() || uiState.area.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Address details auto-detected. Please verify and complete.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = uiState.city,
                onValueChange = { viewModel.updateField("city", it) },
                label = { Text("City") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.area,
                onValueChange = { viewModel.updateField("area", it) },
                label = { Text("Area/Locality") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
        }

        OutlinedTextField(
            value = uiState.address,
            onValueChange = { viewModel.updateField("address", it) },
            label = { Text("Complete Address") },
            placeholder = { Text("Flat/House No, Building, Street") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            minLines = 2
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}
