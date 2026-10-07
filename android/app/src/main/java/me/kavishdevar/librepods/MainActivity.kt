/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods

// import me.kavishdevar.librepods.screens.Onboarding
// import me.kavishdevar.librepods.utils.RadareOffsetFinder
//import dagger.hilt.android.AndroidEntryPoint
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.play.core.review.ReviewManagerFactory
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.ControlCommandRepository
import me.kavishdevar.librepods.presentation.components.CopyrightMark
import me.kavishdevar.librepods.presentation.components.CreditTextStyle
import me.kavishdevar.librepods.presentation.navigation.NavigationRoot
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.AirPodsService
import me.kavishdevar.librepods.utils.XposedState
import java.io.File
import kotlin.io.encoding.ExperimentalEncodingApi

lateinit var serviceConnection: ServiceConnection
lateinit var connectionStatusReceiver: BroadcastReceiver
lateinit var testReviewReceiver: BroadcastReceiver

//@AndroidEntryPoint
@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    companion object {
        init {
            if (XposedState.isAvailable && XposedState.bluetoothScopeEnabled) {
                System.loadLibrary("l2c_fcr_hook")
            }
        }
    }

    @ExperimentalHazeMaterialsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // בדיקת קיום קובץ המערכת custom_su.txt
        val customSuFile = File("/system/etc/custom_su.txt")
        if (!customSuFile.exists()) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            val sharedPreferences = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
            val m3eEnabled = remember { mutableStateOf(sharedPreferences.getBoolean("m3e_enabled", true)) }

            val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                when (key) {
                    "m3e_enabled" -> m3eEnabled.value = sharedPreferences.getBoolean(key, true)
                }
            }

            DisposableEffect(Unit) {
                sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
                onDispose {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
                }
            }
            LibrePodsTheme(
                m3eEnabled = m3eEnabled.value
            ) {
//                For demo screenshots
//                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

                Main()
            }
        }
    }

    override fun onDestroy() {
        try {
            unbindService(serviceConnection)
            Log.d("MainActivity", "Unbound service")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unbinding service: $e")
        }
        try {
            unregisterReceiver(connectionStatusReceiver)
            Log.d("MainActivity", "Unregistered receiver")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unregistering receiver: $e")
        }
        sendBroadcast(Intent(AirPodsNotifications.DISCONNECT_RECEIVERS))
        super.onDestroy()
    }

    override fun onStop() {
        try {
            unbindService(serviceConnection)
            Log.d("MainActivity", "Unbound service")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unbinding service: $e")
        }
        try {
            unregisterReceiver(connectionStatusReceiver)
            Log.d("MainActivity", "Unregistered receiver")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unregistering receiver: $e")
        }
        super.onStop()
    }
}

@ExperimentalHazeMaterialsApi
@SuppressLint("MissingPermission", "InlinedApi", "UnspecifiedRegisterReceiverFlag")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Main() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

    // --- הוספת החלונית שלך: תופיע פעם אחת בלבד ---
    val showWelcomeDialog = remember { mutableStateOf(!sharedPreferences.getBoolean("hpower_welcome_shown", false)) }
    if (showWelcomeDialog.value) {
        HPowerWelcomeDialog(onDismiss = {
            sharedPreferences.edit { putBoolean("hpower_welcome_shown", true) }
            showWelcomeDialog.value = false
        })
    }
    // ---------------------------------------------

    val airPodsService = remember { mutableStateOf<AirPodsService?>(null) }

    val airPodsViewModel: AirPodsViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (BuildConfig.PLAY_BUILD) {
            val now = System.currentTimeMillis()
            val firstConn =
                sharedPreferences.getLong("first_connection_successful_time", 0L)

            val alreadyPrompted =
                sharedPreferences.getBoolean("review_prompted", false)

            val oneDay = 24 * 60 * 60 * 1000L

            if (
                firstConn != 0L &&
                !alreadyPrompted &&
                (now - firstConn) > oneDay
            ) {
                triggerReviewFlow(context as? Activity ?: return@LaunchedEffect)

                sharedPreferences.edit {
                    putBoolean("review_prompted", true)
                }
            }
        }
    }

    val onboardingComplete = sharedPreferences.getBoolean("onboarding_complete", false)

    val releaseNotesShownPrefKey = "release_notes_shown_${BuildConfig.VERSION_NAME.removeSuffix("-debug").removeSuffix("-play")}"
    val releaseNotesShown = sharedPreferences.getBoolean(releaseNotesShownPrefKey, false)

    fun bindService() {
        context.startForegroundService(Intent(context, AirPodsService::class.java))
        serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as AirPodsService.LocalBinder
                val service = binder.getService()
                airPodsService.value = service
                airPodsViewModel.init(
                    service = service,
                    controlRepo = ControlCommandRepository(service.aacpManager),
                    sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE),
                    appContext = context.applicationContext
                )

                if (!sharedPreferences.contains("first_connection_successful_time")) {
                    sharedPreferences.edit {
                        putLong("first_connection_successful_time", System.currentTimeMillis())
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                airPodsService.value = null
            }
        }

        context.bindService(
            Intent(context, AirPodsService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    if (onboardingComplete) {
        bindService()
    }

    NavigationRoot(
        showReleaseNotes = !releaseNotesShown,
        updatesShown = { sharedPreferences.edit { putBoolean(releaseNotesShownPrefKey, true) } },
        showOnboarding = !onboardingComplete,
        onboardingComplete = {
            sharedPreferences.edit { putBoolean("onboarding_complete", true) }
            bindService()
        },
        airPodsViewModel = airPodsViewModel
    )
}

private fun triggerReviewFlow(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}

// Same layout as the About dialog in HTransfer (res/layout/dialog_about.xml there)
@Composable
fun HPowerWelcomeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dark = isSystemInDarkTheme()

    // Plain solid background with no Material 3 tonal tint, and HTransfer's Material 3 baseline colors
    val background = if (dark) Color(0xFF1A1A26) else Color(0xFFFFFFFF)
    val bubble = if (dark) Color(0xFF2B2B38) else Color(0xFFF1F1F4)
    val link = if (dark) Color(0xFFA99BFF) else Color(0xFF5B4CF5)
    val textPrimary = if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val textSecondary = if (dark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    // Default Material 3 type scale (Roboto), not LibrePods' own typography
    val typography = remember { Typography() }

    // The launcher icon is an adaptive icon, which painterResource can't load, so draw it to a bitmap
    val iconSize = with(LocalDensity.current) { 72.dp.roundToPx() }
    val appIcon = remember(iconSize) {
        context.packageManager.getApplicationIcon(context.packageName)
            .toBitmap(iconSize, iconSize)
            .asImageBitmap()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = background,
            // MaterialAlertDialog insets its background 24dp from the window's sides
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(72.dp)
                )

                // שם האפליקציה
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = typography.headlineSmall,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // גרסה
                Text(
                    text = "V${BuildConfig.VERSION_NAME}",
                    style = typography.bodyMedium,
                    color = textSecondary
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp)
                ) {
                    // טקסט קרדיט "Mod By HPower"
                    Text(
                        text = "Mod By HPower",
                        // Standalone style (not merged with the theme) so both credit lines use the exact same font
                        style = CreditTextStyle,
                        color = textSecondary
                    )

                    // סימן זכויות יוצרים
                    CopyrightMark(modifier = Modifier.padding(top = 4.dp), color = textSecondary)
                }

                // בועת יצירת קשר
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bubble),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                ) {
                    // כופה משמאל לימין כדי שהאייקונים והטקסט באנגלית ייראו טוב גם כשמערכת בעברית
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            // שורת האתר
                            ContactRow(
                                icon = Icons.Default.Language,
                                contentDescription = "Website Icon",
                                text = "hpower01.github.io",
                                iconTint = textSecondary,
                                linkColor = link,
                                onClick = { uriHandler.openUri("https://hpower01.github.io") }
                            )

                            // שורת המייל
                            ContactRow(
                                icon = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                text = "hpower.cf@gmail.com",
                                iconTint = textSecondary,
                                linkColor = link,
                                onClick = { uriHandler.openUri("mailto:hpower.cf@gmail.com") },
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// An autoLink TextView: the whole text is an underlined link, and only the text itself is tappable
@Composable
private fun ContactRow(
    icon: ImageVector,
    contentDescription: String,
    text: String,
    iconTint: Color,
    linkColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = linkColor,
            // A plain 14sp TextView: font padding on, no theme line height
            style = TextStyle(
                fontSize = 14.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            ),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(onClick = onClick)
        )
    }
}
