package me.kavishdevar.librepods.billing

import android.content.Context

object BillingProviderFactory {

    fun create(context: Context): BillingProvider {
        // מעקף: החזרת ספק ה-FOSS באופן קבוע כדי להימנע מדרישות תשלום של Google Play
        return FOSSBillingProvider(context)
    }
}
