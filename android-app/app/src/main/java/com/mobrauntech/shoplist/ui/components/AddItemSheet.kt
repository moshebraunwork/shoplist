package com.mobrauntech.shoplist.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mobrauntech.shoplist.data.remote.ItemDto
import com.mobrauntech.shoplist.ui.SheetState
import com.mobrauntech.shoplist.ui.theme.Accent
import com.mobrauntech.shoplist.ui.theme.Bg
import com.mobrauntech.shoplist.ui.theme.Card
import com.mobrauntech.shoplist.ui.theme.Divider
import com.mobrauntech.shoplist.ui.theme.TextPrimary
import com.mobrauntech.shoplist.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    state: SheetState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCountChange: (Int) -> Unit,
    onChooseImage: (String?) -> Unit,
    onUpload: (android.net.Uri) -> Unit,
    onApplyReuse: (ItemDto) -> Unit,
    onEditDuplicate: (ItemDto) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onUpload(uri) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (state.isEditing) "Update item" else "Add item",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // ---- Image candidates + upload ----
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val candidates = state.candidates.take(2)
                candidates.forEach { url ->
                    ImageCandidate(
                        url = url,
                        selected = state.imageUrl == url,
                        onClick = { onChooseImage(if (state.imageUrl == url) null else url) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - candidates.size) {
                    Box(modifier = Modifier.weight(1f).height(120.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Card))
                }
                // Upload box
                Box(
                    modifier = Modifier
                        .size(48.dp, 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Card)
                        .clickable {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.uploading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = Accent)
                    } else {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Upload", tint = TextSecondary)
                    }
                }
            }

            // ---- Duplicate warning ----
            state.duplicates.firstOrNull()?.let { dup ->
                Surface(
                    color = Color(0xFF3A2E12),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEditDuplicate(dup) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE0B341))
                        Column {
                            Text("Already on the list", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("\"${dup.name}\" (x${dup.count}). Tap to update it instead.",
                                color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ---- Name + count row ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SheetField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = "Item name",
                    modifier = Modifier.weight(1f)
                )
                CountScroller(count = state.count, onChange = onCountChange, modifier = Modifier.width(74.dp))
            }

            // ---- Reuse suggestions ----
            if (state.reuse.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Used before", color = TextSecondary, fontSize = 12.sp)
                    state.reuse.take(3).forEach { item ->
                        Surface(
                            color = Card,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onApplyReuse(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!item.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column {
                                    Text(item.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    if (item.description.isNotBlank())
                                        Text(item.description, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // ---- Description ----
            SheetField(
                value = state.description,
                onValueChange = onDescriptionChange,
                placeholder = "Item description",
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Submit ----
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Card,
                    contentColor = TextPrimary,
                    disabledContainerColor = Card.copy(alpha = 0.5f),
                    disabledContentColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(
                    text = if (state.isEditing) "Update" else "Add",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ImageCandidate(
    url: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Accent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        // checkbox overlay (top-left), matching the mockup
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) Accent else Color(0xCCFFFFFF))
                .border(1.dp, Color(0xFF888888), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Bg, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Card,
            unfocusedContainerColor = Card,
            focusedBorderColor = Divider,
            unfocusedBorderColor = Divider,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Accent
        ),
        modifier = modifier.heightIn(min = 56.dp)
    )
}
