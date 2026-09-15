package ir.devtrader.investor.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.devtrader.investor.BuildConfig
import ir.devtrader.investor.R
import ir.devtrader.investor.ui.common.SectionCard
import ir.devtrader.investor.ui.theme.SplashBackground
import java.util.Calendar

/**
 * Placeholder/template content — every string here is meant to be replaced later (see the
 * about_* entries in strings.xml / values-fa/strings.xml, the actual source of truth for the
 * copy). Kept as a plain Composable rather than a ViewModel since there's nothing dynamic to load.
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val contactEmail = stringResource(R.string.about_contact_email)
    val contactWebsite = stringResource(R.string.about_contact_website)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            // Reuses the same brand mark as the splash screen — kept related/on-brand rather than
            // a generic stock photo, and avoids any licensing questions since it's our own asset.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(SplashBackground, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_splash_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(96.dp),
                )
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item {
            SectionCard {
                Text(
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.about_contact_title), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = { uriHandler.openUri("mailto:$contactEmail") }) {
                        Text(contactEmail)
                    }
                    TextButton(onClick = { uriHandler.openUri(contactWebsite) }) {
                        Text(contactWebsite)
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.about_copyright_format, currentYear),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
