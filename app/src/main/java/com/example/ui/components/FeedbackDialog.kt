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
import com.example.feedback.FeedbackOutbox
import com.example.feedback.FeedbackSender
import com.example.ui.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-app feedback form: the user writes a comment (optionally with a contact
 * and one photo) and it is relayed to the developer — no account, no sign-up,
 * nothing leaves the app except the message itself. Tapping Send only needs
 * a connection at that moment: the feedback is queued on the device and
 * delivered in the background, retrying on its own until it goes through, so
 * the user is never shown a failure or asked to resend. The typing draft is
 * kept until the user taps Send.
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
        onDismissRequest = onDismiss,
        title = { Text(strings.feedbackTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
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
                onClick = {
                    if (text.isBlank()) {
                        Toast.makeText(context, strings.feedbackEmpty, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // The one moment the user hears about connectivity: with
                    // no internet at all there is nothing to queue behind, so
                    // the form stays open with the text intact.
                    if (!FeedbackOutbox.isOnline(context)) {
                        Toast.makeText(context, strings.feedbackNeedInternet, Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    // From here the user is done: the feedback is queued on
                    // the device and delivered in the background, retrying on
                    // its own until the relay confirms it. The queue write
                    // (photo file + prefs commit) runs off the main thread.
                    val queuedText = text
                    val queuedContact = contact
                    val queuedPhoto = photoBytes
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            FeedbackOutbox.enqueue(context, queuedText, queuedContact, queuedPhoto)
                            FeedbackOutbox.scheduleFlush(context)
                        }
                        FeedbackSender.clearDraft(context)
                        Toast.makeText(context, strings.feedbackThanks, Toast.LENGTH_LONG).show()
                        onDismiss()
                    }
                }
            ) {
                Text(strings.feedbackSend)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
