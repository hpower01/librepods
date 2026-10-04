package me.kavishdevar.librepods.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val TAG = "PlayBillingProvider"

private const val PREMIUM_PRODUCT_ID = "librepods.advanced_features.v2"

class PlayBillingProvider(
    context: Context
) : BillingProvider, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // הגדרה קבועה של הפרימיום כ-true
    private val _isPremium = MutableStateFlow(true)
    override val isPremium: StateFlow<Boolean> = _isPremium

    private val _price = MutableStateFlow("Free")
    override val price: StateFlow<String> = _price


    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                connect()
            }
        })
    }

    private suspend fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            productDetails = result.productDetailsList?.firstOrNull()
            Log.d(TAG, "Product loaded: ${productDetails?.name}")
            val priceString = productDetails
                ?.oneTimePurchaseOfferDetails
                ?.formattedPrice

            if (priceString != null) {
                _price.value = priceString
            }
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
        }
    }

    private suspend fun queryExistingPurchases() {
        // מניעת שינוי המצב ל-false בקריאה אסינכרונית מהרשת
        _isPremium.value = true
    }

    override fun purchase(activity: Activity) {
        // אין צורך לפתוח את תהליך הרכישה האמיתי מאחר שהפרימיום כבר מופעל
        _isPremium.value = true
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        // שמירה על הסטטוס כפעיל בכל מקרה
        _isPremium.value = true
    }

    private fun processPurchases(purchases: List<Purchase>) {
        // דריסת הבדיקה המקורית והגדרת הערך ל-true תמיד
        _isPremium.value = true

        scope.launch {
            purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                .forEach { acknowledge(it) }
        }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = billingClient.acknowledgePurchase(params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Acknowledgement failed: ${result.debugMessage}")
        }
    }

    override fun queryPurchases() {
        _isPremium.value = true
    }

    override fun restorePurchases() {
        _isPremium.value = true
    }
}
