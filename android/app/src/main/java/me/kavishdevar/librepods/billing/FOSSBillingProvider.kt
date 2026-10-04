package me.kavishdevar.librepods.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R

class FOSSBillingProvider(context: Context): BillingProvider {
    // שינוי ל-true כברירת מחדל קבועה
    private val _isPremium = MutableStateFlow(true)
    override val isPremium: StateFlow<Boolean> = _isPremium

    private val _price = MutableStateFlow(context.getString(R.string.name_your_own_price))
    override val price: StateFlow<String> = _price

    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var purchaseJob: Job? = null

    init {
        queryPurchases()
    }

    override fun purchase(activity: Activity) {
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/kavishdevar".toUri())
        )

        purchaseJob?.cancel()

        purchaseJob = scope.launch {
            delay(5_000)
            _isPremium.value = true
            sharedPreferences.edit { putBoolean("foss_upgraded", true) }
        }
    }

    override fun queryPurchases() {
        // שמירה על סטטוס פרימיום פעיל תמיד
        _isPremium.value = true
        sharedPreferences.edit { putBoolean("foss_upgraded", true) }
    }

    override fun restorePurchases() {
        _isPremium.value = true
        sharedPreferences.edit { putBoolean("foss_upgraded", true) }
    }
}
