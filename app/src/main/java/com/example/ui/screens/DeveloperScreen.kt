package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.KoofkeetFooterQuote
import com.example.ui.components.SectionHeader

@Composable
fun DeveloperScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_developer_badge),
                            contentDescription = "Fayyeeraa Taaddasaa Developer Badge",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Fayyeeraa Taaddasaa",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Barsiisaa • Ogeessa Ijaarsa Weebsaayitii fi Mobaayil Appilikeeshinii",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Ani maqaankoo Fayyeeraa Taaddasaan jedhama. Hojii idileekoo Barsiisummaan alatti, Ogeessa ijaarsa Weebsaayitii fi Ogeessa ijaarsa mobaayil Appilikeeshinii ti. Application Mana Barumsaa Koofkeet kun hanqina qunnamtii maatii barattootaa, barsiisotaa fi bulchiinsa mana barumsaa gidduu jiru furuun yeroo fi humna hedduu ni qusata.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Hojiiwwan Duraanii",
                    subtitle = "Hojiiwwan Waaqayyo na gargaarraan ani duraan hojjedhe"
                )
            }

            item {
                ProjectShowcaseCard(
                    number = "1",
                    title = "Koofkeet Translator (Mobile App)",
                    description = "Appilikeeshinii mobaayilaa Afaan 223 ol walitti hiiku.",
                    icon = Icons.Filled.Translate
                )
            }

            item {
                ProjectShowcaseCard(
                    number = "2",
                    title = "Kofket Digital Library (Website)",
                    description = "Kuusaa Kitaabilee Dijiitaalaa (PDF) Mana Barumsaa Koofkeet.",
                    icon = Icons.Filled.LocalLibrary
                )
            }

            item {
                ProjectShowcaseCard(
                    number = "3",
                    title = "Haadhacare (Mobile App)",
                    description = "Appilikeeshinii fayyaa dubartoota ulfaa to'atu: haala fayyaa isaanii, haala fayyaa daa'imaa fi haala nyaata dubartoota ulfaa hamma da'umsaatti hordofu.",
                    icon = Icons.Filled.Favorite
                )
            }

            item {
                SectionHeader(
                    title = "Toorra Quunnamtii (Contact)",
                    subtitle = "Nu qunnamuuf geessituuwwan armaan gadii tuqaa"
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { openUrl("https://t.me/fayeratadassa") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_telegram_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Teelegraamii: t.me/fayeratadassa")
                        }
                        OutlinedButton(
                            onClick = { openUrl("https://www.facebook.com/fayera.tadada.75") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_facebook_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Feesbuukii: facebook.com/fayera.tadada.75")
                        }
                        OutlinedButton(
                            onClick = { openUrl("https://www.instagram.com/faye_west") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("contact_instagram_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instagram: instagram.com/faye_west")
                        }
                    }
                }
            }

            item {
                KoofkeetFooterQuote()
            }
        }
    }
}

@Composable
private fun ProjectShowcaseCard(
    number: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
