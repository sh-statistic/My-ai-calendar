package com.example.ui.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.feedback.FeedbackSender
import com.example.ui.AppStrings
import kotlinx.coroutines.launch

/**
 * In-app feedback form: the user writes a comment (optionally with a contact
 * and one photo) and it is relayed to the developer — no account, no sign-up,
 * nothing leaves the app except the message itself. The draft is kept on the
 * device until a send succeeds, so a failed send never loses the text.
 */
@Composable
fun FeedbackDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember { FeedbackSender.loadDraft(context) }
    var text by remember { mutableStateOf(draft.first) }
    var contact by remember { mutableStateOf(draft.second) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var sending by remember { mutableStateOf(false) }

    val photoBitmap = remember(photoBytes) {
        photoBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val prepared = FeedbackSender.prepareFeedbackPhoto(context, uri)
                if (prepared == null) {
                    Toast.makeText(context, strings.feedbackPhotoFailed, Toast.LENGTH_SHORT).show()
                } else {
                    photoBytes = prepared
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(strings.feedbackTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    enabled = !sending,
                    onValueChange = {
                        text = it
                        FeedbackSender.saveDraft(context, it, contact)
                    },
                    placeholder = { Text(strings.feedbackHint) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = contact,
                    enabled = !sending,
                    onValueChange = {
                        contact = it
                        FeedbackSender.saveDraft(context, text, it)
                    },
                    placeholder = { Text(strings.feedbackContactHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (photoBytes == null) {
                    TextButton(onClick = { photoLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.feedbackAttachPhoto)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Text(
                            strings.feedbackPhotoAttached,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { photoBytes = null }) {
                            Text(strings.feedbackRemovePhoto)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = {
                    if (text.isBlank()) {
                        Toast.makeText(context, strings.feedbackEmpty, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    sending = true
                    scope.launch {
                        val ok = FeedbackSender.send(context, text, contact, photoBytes)
                        sending = false
                        if (ok) {
                            FeedbackSender.clearDraft(context)
                            Toast.makeText(context, strings.feedbackSent, Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            FeedbackSender.saveDraft(context, text, contact)
                            Toast.makeText(context, strings.feedbackFailed, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            ) {
                Text(if (sending) strings.feedbackSending else strings.feedbackSend)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!sending) onDismiss() }) {
                Text(strings.cancel)
            }
        }
    )
}
