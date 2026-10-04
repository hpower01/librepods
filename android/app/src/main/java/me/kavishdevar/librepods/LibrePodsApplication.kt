package me.kavishdevar.librepods

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import me.kavishdevar.librepods.billing.BillingManager
import me.kavishdevar.librepods.billing.BillingProviderFactory
import me.kavishdevar.librepods.utils.XposedServiceHolder
import me.kavishdevar.librepods.utils.XposedState

class LibrePodsApplication: Application(), XposedServiceHelper.OnServiceListener, DefaultLifecycleObserver {

    override fun onCreate() {
        XposedServiceHelper.registerListener(this)
        BillingManager.provider = BillingProviderFactory.create(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        super<Application>.onCreate()

        // מעקף: הגדרת מצב Xposed כפעיל וזמין מיד עם הפעלת האפליקציה
        XposedState.isAvailable = true
        XposedState.bluetoothScopeEnabled = true
    }

    override fun onResume(owner: LifecycleOwner) {
        BillingManager.provider.queryPurchases()
        // שמירה על מצב פעיל בעת חזרה לאפליקציה
        XposedState.isAvailable = true
        XposedState.bluetoothScopeEnabled = true
    }

    override fun onServiceBind(service: XposedService) {
        XposedServiceHolder.service = service
        XposedState.isAvailable = true
        XposedState.bluetoothScopeEnabled = true
    }

    override fun onServiceDied(p0: XposedService) {
        XposedServiceHolder.service = null
        // מניעת שינוי המצב ל-false במידה והשירות קורס או אינו קיים
        XposedState.isAvailable = true
        XposedState.bluetoothScopeEnabled = true
    }
}
