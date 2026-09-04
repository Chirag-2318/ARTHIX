package com.chirag.arthix.ui.screen.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.chirag.arthix.ui.components.PrimaryButton
import com.chirag.arthix.ui.screen.account.AccountViewModel
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.HeadlineLg

@Composable
fun CreateProfileScreen(
    onComplete: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val colors = ArthixTheme.colors
    val spacing = ArthixTheme.spacing
    val shapes = ArthixTheme.shapes

    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var selectedCustomUri by remember { mutableStateOf<Uri?>(null) }

    val presets = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if it fails, some providers don't support it
            }
            selectedCustomUri = uri
            selectedPreset = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .padding(horizontal = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.xxl))
        
        Text(
            text = "Pick a face for your account",
            style = HeadlineLg,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = "You can always change this later.",
            fontSize = 15.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xxl))

        // Option A: Upload Custom Photo
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(shapes.avatarShape)
                .background(colors.secondaryContainer)
                .border(
                    BorderStroke(
                        if (selectedCustomUri != null) 3.dp else 1.dp,
                        if (selectedCustomUri != null) colors.accent else colors.border
                    ), 
                    shapes.avatarShape
                )
                .clickable { launcher.launch(arrayOf("image/*")) },
            contentAlignment = Alignment.Center
        ) {
            if (selectedCustomUri != null) {
                AsyncImage(
                    model = selectedCustomUri,
                    contentDescription = "Custom Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add photo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = "Or choose a preset",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(16.dp))

        // Option B: Presets Grid
        val context = LocalContext.current
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(presets) { preset ->
                val isSelected = preset == selectedPreset
                val resId = context.resources.getIdentifier(preset, "drawable", context.packageName)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shapes.avatarShape)
                        .background(colors.surface)
                        .border(
                            BorderStroke(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) colors.accent else colors.border
                            ), 
                            shapes.avatarShape
                        )
                        .clickable {
                            selectedPreset = preset
                            selectedCustomUri = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (resId != 0) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = resId),
                            contentDescription = "Preset \$preset",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Bottom Actions
        val canContinue = selectedPreset != null || selectedCustomUri != null

        val scope = rememberCoroutineScope()

        PrimaryButton(
            text = "Continue",
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val avatarStr = if (selectedPreset != null) {
                        "${selectedPreset}.png"
                    } else if (selectedCustomUri != null) {
                        try {
                            val file = java.io.File(context.filesDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
                            context.contentResolver.openInputStream(selectedCustomUri!!)?.use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            file.absolutePath
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    withContext(Dispatchers.Main) {
                        viewModel.updateAvatar(avatarStr, onComplete = onComplete)
                    }
                }
            },
            enabled = canContinue,
        )

        Spacer(Modifier.height(spacing.sm))

        TextButton(
            onClick = onComplete,
            modifier = Modifier.padding(bottom = spacing.xl)
        ) {
            Text("Skip for now", color = colors.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
