package app.brainbox.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.data.player.PlayerRepository
import app.brainbox.data.player.ProfileError
import app.brainbox.domain.repository.Language
import kotlinx.coroutines.launch

val BrainBoxBackground = Brush.radialGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2), Color(0xFF2E1A47)),
    radius = 1500f
)

/** [setup] = écran de bienvenue du premier lancement (pas de retour, bouton "continuer"). */
@Composable
fun ProfileScreen(repository: PlayerRepository, setup: Boolean, onDone: () -> Unit) {
    val texts = remember { PlayerTexts(Language.fromDeviceLocale()) }
    val profile by repository.profile.collectAsState()
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var avatar by rememberSaveable { mutableIntStateOf(profile.avatar) }
    var error by remember { mutableStateOf<ProfileError?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val valid = PlayerRepository.isValidName(name)
    val changed = name.trim() != profile.name || avatar != profile.avatar

    fun close() {
        if (setup) repository.completeSetup()
        onDone()
    }
    BackHandler(enabled = !saving) { close() }

    Box(modifier = Modifier.fillMaxSize().background(BrainBoxBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!setup) {
                    IconButton(
                        onClick = { close() },
                        enabled = !saving,
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = if (setup) texts.welcomeTitle else texts.profileTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (setup) texts.welcomeText else texts.profileText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            PlayerAvatar(avatar, 96.dp, Modifier.border(3.dp, Color(0xFFFFD93D), CircleShape))
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24); error = null },
                label = { Text(texts.nameLabel) },
                singleLine = true,
                enabled = !saving,
                isError = !valid || error != null,
                supportingText = { Text(error?.let(texts::error) ?: texts.nameHelp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFD93D),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Color(0xFFFFD93D),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedSupportingTextColor = Color.White.copy(alpha = 0.7f),
                    unfocusedSupportingTextColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color(0xFFFFD93D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = texts.chooseAvatar,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            )

            val gridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = AvatarGallery.displayOrder.indexOf(avatar).coerceAtLeast(0)
            )
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 64.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AvatarGallery.displayOrder.size, key = { AvatarGallery.displayOrder[it] }) { position ->
                    val index = AvatarGallery.displayOrder[position]
                    val selected = avatar == index
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(if (selected) 3.dp else 0.dp, if (selected) Color(0xFFFFD93D) else Color.Transparent, CircleShape)
                            .selectable(selected = selected, enabled = !saving, role = Role.RadioButton) {
                                avatar = index
                                error = null
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayerAvatar(index, 56.dp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!changed) {
                        close()
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        try {
                            error = repository.saveProfile(name, avatar)
                            if (error == null) onDone()
                        } finally {
                            saving = false
                        }
                    }
                },
                enabled = valid && !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(if (saving) texts.saving else texts.save, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (setup) {
                TextButton(onClick = { close() }, enabled = !saving) {
                    Text(texts.keepDefault, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Text(
                text = texts.guestNote,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
