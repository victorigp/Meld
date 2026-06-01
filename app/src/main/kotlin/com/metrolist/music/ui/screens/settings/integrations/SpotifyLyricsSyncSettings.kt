/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.EnableSpotifyLyricsSyncKey
import com.metrolist.music.constants.SpotifyLyricsSyncIdKey
import com.metrolist.music.constants.SpotifyLyricsSyncUrlKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLyricsSyncSettings(
    navController: NavController,
) {
    val (syncEnabled, onSyncEnabledChange) = rememberPreference(
        key = EnableSpotifyLyricsSyncKey,
        defaultValue = false,
    )

    var syncId by rememberPreference(SpotifyLyricsSyncIdKey, "")
    var syncUrl by rememberPreference(
        SpotifyLyricsSyncUrlKey,
        "https://spotify-lyrics-three.vercel.app/api/meld-sync",
    )

    // Local state for the text fields to provide smooth editing
    var editingSyncId by remember(syncId) { mutableStateOf(syncId) }
    var editingSyncUrl by remember(syncUrl) { mutableStateOf(syncUrl) }

    val showIdWarning = syncEnabled && editingSyncId.isBlank()

    Column(
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top,
                ),
            ),
        )

        // Enable / Disable toggle
        Material3SettingsGroup(
            title = stringResource(R.string.options),
            items = listOf(
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.spotifylyrics_sync_enable)) },
                    description = { stringResource(R.string.spotifylyrics_sync_enable_desc) },
                    trailingContent = {
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && editingSyncId.isBlank()) {
                                    // Don't enable if sync ID is empty
                                    return@Switch
                                }
                                onSyncEnabledChange(enabled)
                            },
                        )
                    },
                    icon = painterResource(R.drawable.sync),
                ),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Sync ID field
        Material3SettingsGroup(
            title = stringResource(R.string.spotifylyrics_sync_id),
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Column {
                            OutlinedTextField(
                                value = editingSyncId,
                                onValueChange = { value ->
                                    editingSyncId = value
                                    syncId = value
                                },
                                label = { Text(stringResource(R.string.spotifylyrics_sync_id_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (showIdWarning) {
                                Text(
                                    text = stringResource(R.string.spotifylyrics_sync_id_empty_warning),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    },
                    icon = painterResource(R.drawable.person),
                ),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Custom endpoint URL
        Material3SettingsGroup(
            title = stringResource(R.string.spotifylyrics_sync_url),
            items = listOf(
                Material3SettingsItem(
                    title = {
                        OutlinedTextField(
                            value = editingSyncUrl,
                            onValueChange = { value ->
                                editingSyncUrl = value
                                syncUrl = value
                            },
                            label = { Text(stringResource(R.string.spotifylyrics_sync_url_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    description = { stringResource(R.string.spotifylyrics_sync_url_desc) },
                    icon = painterResource(R.drawable.link),
                ),
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Status indicator
        Material3SettingsGroup(
            title = stringResource(R.string.information),
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Text(
                            text = if (syncEnabled && editingSyncId.isNotBlank()) {
                                stringResource(R.string.spotifylyrics_sync_status_active)
                            } else {
                                stringResource(R.string.spotifylyrics_sync_status_inactive)
                            },
                        )
                    },
                    description = {
                        if (syncEnabled && editingSyncId.isNotBlank()) {
                            "Sync ID: $editingSyncId"
                        } else {
                            stringResource(R.string.spotifylyrics_sync_enable_desc)
                        }
                    },
                    icon = painterResource(R.drawable.info),
                ),
            ),
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.spotifylyrics_sync_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
