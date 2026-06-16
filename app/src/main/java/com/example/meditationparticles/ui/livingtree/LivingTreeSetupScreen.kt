package com.example.meditationparticles.ui.livingtree

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.DuplicateLivingTreeNameException
import com.example.meditationparticles.data.LivingTreeRepository
import com.example.meditationparticles.data.local.LivingTreePersonEntity
import com.example.meditationparticles.data.local.LivingTreePersonWithTags
import com.example.meditationparticles.data.local.LivingTreeTagEntity
import com.example.meditationparticles.domain.livingtree.LivingTreeColor
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import com.example.meditationparticles.domain.livingtree.LivingTreePersonNames
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class LivingTreeSetupUiState(
    val tags: List<LivingTreeTagEntity> = emptyList(),
    val people: List<LivingTreePersonWithTags> = emptyList(),
    val errorMessage: String? = null,
    val confirmDeleteTag: LivingTreeTagEntity? = null,
    val confirmDeletePerson: LivingTreePersonEntity? = null,
)

class LivingTreeSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LivingTreeRepository = AppGraph.livingTree(application)

    private val _uiState = MutableStateFlow(LivingTreeSetupUiState())
    val uiState: StateFlow<LivingTreeSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultTagsIfEmpty()
        }
        viewModelScope.launch {
            combine(repository.tags, repository.peopleWithTags) { tags, people ->
                _uiState.value.copy(tags = tags, people = people, errorMessage = null)
            }.collect { next ->
                _uiState.value = next
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun requestDeleteTag(tag: LivingTreeTagEntity) {
        _uiState.value = _uiState.value.copy(confirmDeleteTag = tag)
    }

    fun requestDeletePerson(person: LivingTreePersonEntity) {
        _uiState.value = _uiState.value.copy(confirmDeletePerson = person)
    }

    fun dismissDeleteConfirmations() {
        _uiState.value = _uiState.value.copy(
            confirmDeleteTag = null,
            confirmDeletePerson = null,
        )
    }

    fun confirmDeleteTag() {
        val tag = _uiState.value.confirmDeleteTag ?: return
        viewModelScope.launch {
            repository.deleteTag(tag)
            dismissDeleteConfirmations()
        }
    }

    fun confirmDeletePerson() {
        val person = _uiState.value.confirmDeletePerson ?: return
        viewModelScope.launch {
            repository.deletePerson(person)
            dismissDeleteConfirmations()
        }
    }

    fun saveTag(name: String, colorArgb: Int, existing: LivingTreeTagEntity? = null) {
        viewModelScope.launch {
            runCatching {
                if (existing == null) {
                    repository.createTag(name, colorArgb)
                } else {
                    repository.updateTag(existing.copy(name = name, colorArgb = colorArgb))
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = duplicateMessage(error))
            }
        }
    }

    fun savePerson(
        name: String,
        notes: String,
        tagIds: Set<Long>,
        existing: LivingTreePersonEntity? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                if (existing == null) {
                    repository.createPerson(name, notes, tagIds)
                } else {
                    repository.updatePerson(existing.copy(name = name, notes = notes), tagIds)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = duplicateMessage(error))
            }
        }
    }

    fun savePeople(namesInput: String, notes: String, tagIds: Set<Long>) {
        val names = LivingTreePersonNames.parse(namesInput)
        if (names.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                if (names.size == 1) {
                    repository.createPerson(names.single(), notes, tagIds)
                } else {
                    val result = repository.createPeople(names, tagIds = tagIds)
                    if (result.createdCount == 0) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = bulkSkipMessage(result.skippedDuplicates),
                        )
                    } else if (result.skippedDuplicates.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = bulkPartialMessage(result.createdCount, result.skippedDuplicates),
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = duplicateMessage(error))
            }
        }
    }

    private fun bulkSkipMessage(skipped: List<String>): String {
        val names = skipped.joinToString(", ") { "\"$it\"" }
        return if (skipped.size == 1) {
            "Skipped duplicate name: $names."
        } else {
            "Skipped duplicate names: $names."
        }
    }

    private fun bulkPartialMessage(created: Int, skipped: List<String>): String {
        val names = skipped.joinToString(", ") { "\"$it\"" }
        val personWord = if (created == 1) "person" else "people"
        return "Added $created $personWord. Skipped duplicates: $names."
    }

    private fun duplicateMessage(error: Throwable): String = when (error) {
        is DuplicateLivingTreeNameException -> when (error.kind) {
            DuplicateLivingTreeNameException.Kind.Person ->
                "A person with that name already exists."
            DuplicateLivingTreeNameException.Kind.Tag ->
                "A tag with that name already exists."
        }
        else -> error.message ?: "Something went wrong."
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LivingTreeSetupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LivingTreeSetupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var editingTag by remember { mutableStateOf<LivingTreeTagEntity?>(null) }
    var editingPerson by remember { mutableStateOf<LivingTreePersonWithTags?>(null) }
    var showNewTag by remember { mutableStateOf(false) }
    var showNewPerson by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Living Flower Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
        ) {
            state.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            SetupSectionHeader(
                title = "People",
                addContentDescription = "New person or people",
                onAdd = { showNewPerson = true },
            )
            state.people.forEach { entry ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingPerson = entry },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = entry.person.name, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { viewModel.requestDeletePerson(entry.person) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete person")
                            }
                        }
                        if (entry.tags.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                entry.tags.forEach { tag ->
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(tag.colorArgb),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SetupSectionHeader(title = "Tags", onAdd = { showNewTag = true })
            state.tags.forEach { tag ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingTag = tag },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(tag.colorArgb)),
                            )
                            Text(text = tag.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { viewModel.requestDeleteTag(tag) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete tag")
                        }
                    }
                }
            }
        }
    }

    if (showNewTag || editingTag != null) {
        TagEditorDialog(
            existing = editingTag,
            onDismiss = {
                showNewTag = false
                editingTag = null
                viewModel.clearError()
            },
            onSave = { name, color ->
                viewModel.saveTag(name, color, editingTag)
                showNewTag = false
                editingTag = null
            },
        )
    }

    if (showNewPerson || editingPerson != null) {
        PersonEditorDialog(
            existing = editingPerson,
            tags = state.tags,
            onDismiss = {
                showNewPerson = false
                editingPerson = null
                viewModel.clearError()
            },
            onSave = { namesInput, notes, tagIds ->
                if (editingPerson == null) {
                    viewModel.savePeople(namesInput, notes, tagIds)
                } else {
                    viewModel.savePerson(namesInput, notes, tagIds, editingPerson?.person)
                }
                showNewPerson = false
                editingPerson = null
            },
        )
    }

    state.confirmDeleteTag?.let { tag ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmations,
            title = { Text("Delete tag?") },
            text = { Text("Remove \"${tag.name}\" from Living Flower? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteTag) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmations) { Text("Cancel") }
            },
        )
    }

    state.confirmDeletePerson?.let { person ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmations,
            title = { Text("Delete person?") },
            text = { Text("Remove \"${person.name}\" from Living Flower? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeletePerson) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmations) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SetupSectionHeader(
    title: String,
    onAdd: () -> Unit,
    addContentDescription: String = "Add $title",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = addContentDescription)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    existing: LivingTreeTagEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var selectedColor by remember(existing) {
        mutableStateOf(existing?.colorArgb ?: LivingTreeDefaults.presetColors.first())
    }
    var showRgbPicker by remember(existing) { mutableStateOf(false) }
    val isCustomSelected = selectedColor !in LivingTreeDefaults.presetColors

    if (showRgbPicker) {
        RgbColorPickerDialog(
            initialColorArgb = selectedColor,
            onDismiss = { showRgbPicker = false },
            onConfirm = { colorArgb ->
                selectedColor = colorArgb
                showRgbPicker = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New tag" else "Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LivingTreeDefaults.presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (color == selectedColor) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = color },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCustomSelected) Color(selectedColor)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            .border(
                                width = if (isCustomSelected) 2.dp else 1.dp,
                                color = if (isCustomSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .clickable { showRgbPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!isCustomSelected) {
                            Text(
                                text = "C",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { showRgbPicker = true },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedColor) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RgbColorPickerDialog(
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var red by remember(initialColorArgb) {
        mutableStateOf(LivingTreeColor.redFromArgb(initialColorArgb).toFloat())
    }
    var green by remember(initialColorArgb) {
        mutableStateOf(LivingTreeColor.greenFromArgb(initialColorArgb).toFloat())
    }
    var blue by remember(initialColorArgb) {
        mutableStateOf(LivingTreeColor.blueFromArgb(initialColorArgb).toFloat())
    }
    val previewColor = Color(
        LivingTreeColor.colorArgb(
            red = red.toInt(),
            green = green.toInt(),
            blue = blue.toInt(),
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                        ),
                )
                RgbSliderRow(label = "Red", value = red, onValueChange = { red = it })
                RgbSliderRow(label = "Green", value = green, onValueChange = { green = it })
                RgbSliderRow(label = "Blue", value = blue, onValueChange = { blue = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        LivingTreeColor.colorArgb(
                            red = red.toInt(),
                            green = green.toInt(),
                            blue = blue.toInt(),
                        ),
                    )
                },
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RgbSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            steps = 254,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonEditorDialog(
    existing: LivingTreePersonWithTags?,
    tags: List<LivingTreeTagEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Set<Long>) -> Unit,
) {
    val isNew = existing == null
    var name by remember(existing) { mutableStateOf(existing?.person?.name ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.person?.notes ?: "") }
    var selectedTagIds by remember(existing) {
        mutableStateOf(existing?.tags?.map { it.id }?.toSet() ?: emptySet())
    }
    val parsedNames = remember(name) { LivingTreePersonNames.parse(name) }
    val isBulkAdd = isNew && parsedNames.size > 1
    val notesEnabled = !isBulkAdd

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isNew && parsedNames.size > 1 -> "New people"
                    isNew -> "New person or people"
                    else -> "Edit person"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isNew) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Names") },
                        placeholder = { Text("Alex, Jordan, Sam") },
                        supportingText = {
                            Text("Separate names with commas. Each name becomes a person with the selected tags.")
                        },
                        minLines = 3,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (notesEnabled) notes = it },
                    label = { Text("Notes (optional)") },
                    enabled = notesEnabled,
                    supportingText = if (isBulkAdd) {
                        { Text("Notes apply to single-person add only.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "Tags", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        val selected = tag.id in selectedTagIds
                        Text(
                            text = tag.name,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) Color(tag.colorArgb).copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                                .clickable {
                                    selectedTagIds = selectedTagIds.toMutableSet().apply {
                                        if (selected) remove(tag.id) else add(tag.id)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (selected) Color(tag.colorArgb) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, notes, selectedTagIds) },
                enabled = parsedNames.isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
