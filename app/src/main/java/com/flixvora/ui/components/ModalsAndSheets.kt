package com.flixvora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flixvora.data.MediaTitle
import com.flixvora.ui.theme.*

@Composable
fun PinLockDialog(
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val accent = LocalAccentTheme.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(accent.surface)
                .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = accent.primary, modifier = Modifier.size(28.dp))
            Text("Unlock Adult (18+) Profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Enter 4-digit Parental PIN (default: 1234)", color = TextSecondary, fontSize = 12.sp)
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) { pin = it.filter(Char::isDigit); error = false } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = error
            )
            if (error) {
                Text("Incorrect PIN. Try 1234.", color = LiveCrimson, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = { if (!onVerify(pin)) error = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WatchPartyDialog(
    title: MediaTitle,
    onStartWatchParty: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalAccentTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(accent.surface)
                .border(1.dp, BorderHairline, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Watch Party Room · FX-8492", color = accent.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Sync playback of \"${title.title}\" with friends.", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Subtitles & playback timestamps sync automatically across participants.", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Close", color = TextSecondary)
                }
                Button(
                    onClick = onStartWatchParty,
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary, contentColor = accent.onPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Sync", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
