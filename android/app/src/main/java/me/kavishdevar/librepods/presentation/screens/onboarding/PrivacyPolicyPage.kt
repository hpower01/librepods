package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R

@Composable
fun PrivacyPolicyPage(
    onForward: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(42.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.pp_last_updated),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.pp_overview),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.pp_overview_1),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_overview_2),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_third_party),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.pp_third_party_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_email),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.pp_email_1),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_email_2),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_discord),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.pp_discord_1),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_discord_2),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_github_issues),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.pp_github_1),
                style = MaterialTheme.typography.bodyMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    stringResource(R.string.pp_github_item_version),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.pp_github_item_device),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.pp_github_item_build),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.pp_github_item_source),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = stringResource(R.string.pp_github_2),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.pp_payments), style = MaterialTheme.typography.titleLarge
            )

            if (BuildConfig.PLAY_BUILD) {
                Text(
                    text = stringResource(R.string.pp_google_play), style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.pp_play_1),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.pp_play_2),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.pp_github_sponsors), style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.pp_sponsors_1),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.pp_sponsors_2),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = stringResource(R.string.pp_contact), style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.pp_contact_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onForward,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.i_agree),
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
