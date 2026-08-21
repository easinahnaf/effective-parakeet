package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PuckPrimary

data class PromptTemplate(
    val title: String,
    val category: String,
    val text: String
)

val SAMPLE_TEMPLATES = listOf(
    PromptTemplate(
        title = "Puck Introduction (পরিচিতি)",
        category = "Intro",
        text = "Hello! I am Puck, an expressive voice powered by Google. I can narrate your stories, read your articles, and convert any script into crystal clear speech."
    ),
    PromptTemplate(
        title = "Storytelling (গল্প পাঠ)",
        category = "Story",
        text = "Once upon a time, beneath a canopy of whispering pines and starlit skies, a gentle breeze carried the echoes of an ancient forgotten journey."
    ),
    PromptTemplate(
        title = "Bengali Announcement (ঘোষণা)",
        category = "Bengali",
        text = "সুপ্রিয় শ্রোতাবৃন্দ, গুগল টেক্সট টু অডিও অ্যাপ্লিকেশনে আপনাদের স্বাগতম। আপনি যেকোনো লেখা সহজেই অডিও ফাইলে রূপান্তর করে ফোনের স্টোরেজে সংরক্ষণ করতে পারবেন।"
    ),
    PromptTemplate(
        title = "Motivational Speech (অনুপ্রেরণা)",
        category = "Motivation",
        text = "Every morning brings a new beginning. Focus on the possibilities of today, stay persistent, and make every single step count toward your goals."
    ),
    PromptTemplate(
        title = "Tech & Science Update (প্রযুক্তি)",
        category = "Technology",
        text = "Artificial intelligence and modern neural text-to-speech models are transforming how we communicate, learn, and create audio content seamlessly."
    )
)

@Composable
fun TemplateDialog(
    onSelectTemplate: (text: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PuckPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sample Scripts & Templates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SAMPLE_TEMPLATES) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTemplate(template.text, template.title) }
                            .testTag("template_${template.category}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = template.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PuckPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.text,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
