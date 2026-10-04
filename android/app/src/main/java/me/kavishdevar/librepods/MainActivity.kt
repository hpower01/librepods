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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.play.core.review.ReviewManagerFactory
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.ControlCommandRepository
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

@Composable
fun HPowerWelcomeDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // שם האפליקציה (מתחילים ישר מכאן ללא תמונה למניעת שגיאות הידור)
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )

                // גרסה
                Text(
                    text = "V${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // טקסט קרדיט "Mod By HPower"
                Text(
                    text = "Mod By HPower",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // סימן זכויות יוצרים
                Text(
                    text = "©",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Cursive,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // בועת יצירת קשר
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // כופה משמאל לימין כדי שהאייקונים והטקסט באנגלית ייראו טוב גם כשמערכת בעברית
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            // שורת האתר
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri("https://kobiamos001.github.io") }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Website",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "hpower01.github.io",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // שורת המייל
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uriHandler.openUri("mailto:kobiamos001@gmail.com") }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "hpower.cf@gmail.com",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // כפתור אישור לסגירת החלונית
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.close_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
