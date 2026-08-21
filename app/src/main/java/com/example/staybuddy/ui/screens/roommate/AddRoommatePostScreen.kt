package com.example.staybuddy.ui.screens.roommate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.surfaceColorAtElevation
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.staybuddy.data.model.RoommatePostType
import com.example.staybuddy.ui.components.LocationPicker
import com.mapbox.geojson.Point
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.MyLocation
import com.example.staybuddy.ui.components.mouseWheelScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoommatePostScreen(
    onNavigateBack: () -> Unit,
    onPostAdded: () -> Unit,
    viewModel: AddRoommatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPostAdded()
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = if (uiState.isEditing) "Edit Post" else "Create Post",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isSeekerModeEnabled) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = uiState.postType == RoommatePostType.OFFER,
                        onClick = { viewModel.onPostTypeChanged(RoommatePostType.OFFER) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Has Room", fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = uiState.postType == RoommatePostType.SEEK,
                        onClick = { viewModel.onPostTypeChanged(RoommatePostType.SEEK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Needs Room", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = if (uiState.postType == RoommatePostType.OFFER) 
                    "Have a spare room? Post the details below so potential roommates can find you."
                else
                    "Looking for a room/PG? Post your budget & target location so flat owners & flatmates can reach you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Custom Form Surface for better separation
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (uiState.postType == RoommatePostType.OFFER) {
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 250.dp, max = 450.dp)
                                .height((configuration.screenHeightDp * 0.45f).dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            LocationPicker(
                                initialLocation = if (uiState.latitude != null) com.mapbox.geojson.Point.fromLngLat(uiState.longitude!!, uiState.latitude!!) else null,
                                onLocationSelected = { point ->
                                    viewModel.updateLocation(point.latitude(), point.longitude(), uiState.address)
                                }
                            )
                        }

                        if (uiState.latitude != null) {
                            Text(
                                text = "Address details auto-detected. Please verify and complete.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = { viewModel.updateField("city", it) },
                        label = { Text("Target City") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = { viewModel.updateField("location", it) },
                        label = { 
                            Text(if (uiState.postType == RoommatePostType.OFFER) "Locality / Area" else "Preferred Area / Institute (e.g. Parul University)") 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    if (uiState.postType == RoommatePostType.OFFER) {
                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = { viewModel.updateField("address", it) },
                            label = { Text("Complete Flat/House Address") },
                            placeholder = { Text("Flat/House No, Building, Street") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.roomType,
                        onValueChange = { viewModel.updateField("roomType", it) },
                        label = { 
                            Text(if (uiState.postType == RoommatePostType.OFFER) "Room Type (e.g. Shared, Single)" else "Preferred Room Type (e.g. Single, Shared, 1BHK)") 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateField("description", it) },
                        label = { 
                            Text(if (uiState.postType == RoommatePostType.OFFER) "Tell us about the room & flatmates..." else "Tell us about yourself, university, habits...") 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.priceShare,
                            onValueChange = { viewModel.updateField("priceShare", it) },
                            label = { 
                                Text(if (uiState.postType == RoommatePostType.OFFER) "Monthly Share (₹)" else "Max Budget (₹/month)") 
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            prefix = { Text("₹") }
                        )
                        
                        if (uiState.postType == RoommatePostType.OFFER) {
                            OutlinedTextField(
                                value = uiState.availableBeds,
                                onValueChange = { viewModel.updateField("availableBeds", it) },
                                label = { Text("Beds Left") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }

                    if (uiState.postType == RoommatePostType.OFFER) {
                        OutlinedTextField(
                            value = uiState.totalBeds,
                            onValueChange = { viewModel.updateField("totalBeds", it) },
                            label = { Text("Total Beds in Flat") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
            
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp)
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreferenceCheckbox(
                        label = "Gender Preference",
                        checkedText = "Female Only",
                        uncheckedText = "Any",
                        isChecked = uiState.genderPreference == "Female",
                        onCheckedChange = { checked -> 
                            viewModel.updateField("genderPreference", if (checked) "Female" else "Any")
                        }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceCheckbox(
                        label = "Dietary Preference",
                        checkedText = "Vegetarian Only",
                        uncheckedText = "Any",
                        isChecked = uiState.foodPreference == "Vegetarian",
                        onCheckedChange = { checked -> 
                            viewModel.updateField("foodPreference", if (checked) "Vegetarian" else "Any")
                        }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    PreferenceCheckbox(
                        label = "Smoking Preference",
                        checkedText = "Non-Smoker Only",
                        uncheckedText = "Any",
                        isChecked = uiState.smokingDrinking == "Non-Smoker",
                        onCheckedChange = { checked -> 
                            viewModel.updateField("smokingDrinking", if (checked) "Non-Smoker" else "Any")
                        }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    PreferenceCheckbox(
                        label = "Pet Friendly",
                        checkedText = "No Pets Allowed",
                        uncheckedText = "Any",
                        isChecked = uiState.petFriendly == "No Pets Please",
                        onCheckedChange = { checked -> 
                            viewModel.updateField("petFriendly", if (checked) "No Pets Please" else "Any")
                        }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(text = "Guests & Visitors", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "How do you feel about guests/visitors?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = uiState.guestsVisitors,
                            onValueChange = { viewModel.updateField("guestsVisitors", it) },
                            placeholder = { Text("e.g. Occasional guests allowed, No overnight stay...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.submitPost() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isLoading,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                } else {
                    Text(
                        if (uiState.isEditing) "Update My Post" else "Post My Room",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        }
    }
}

@Composable
fun PreferenceCheckbox(
    label: String,
    checkedText: String,
    uncheckedText: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (isChecked) checkedText else uncheckedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
