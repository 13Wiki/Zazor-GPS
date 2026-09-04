package com.gps.zazor.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.gps.zazor.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Billing behind [ProStatus].
 *
 * The entitlement is also mirrored into preferences, so the app knows it is Pro while offline or
 * while the billing service is still connecting - without that, ads would flash back on every cold
 * start until Play answered. The store remains the authority: a refund clears the mirror on the
 * next successful query.
 */
class PlayProStatus(
    context: Context,
    private val prefs: AppPreferences
) : ProStatus, PurchasesUpdatedListener {

    companion object {

        /** Must match the in-app product id created in Play Console. */
        const val PRODUCT_REMOVE_ADS = "zazor_pro_remove_ads"
    }

    private val proState = MutableStateFlow(prefs.isPro())
    private val priceState = MutableStateFlow<String?>(null)

    override val isPro: StateFlow<Boolean> = proState.asStateFlow()
    override val priceLabel: StateFlow<String?> = priceState.asStateFlow()

    private var details: ProductDetails? = null

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    override fun refresh() {
        if (client.isReady) {
            queryEverything()
            return
        }
        client.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) queryEverything()
            }

            // A dropped connection is normal; the next refresh reconnects.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Opens the purchase flow. Does nothing when the product has not loaded yet. */
    fun purchase(activity: Activity) {
        val product = details ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases?.forEach(::handlePurchase)
    }

    private fun queryEverything() {
        queryProduct()
        queryPurchases()
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, products ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = products.firstOrNull()
            priceState.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any {
                it.products.contains(PRODUCT_REMOVE_ADS) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            setPro(owned)
            purchases.forEach(::handlePurchase)
        }
    }

    /**
     * Play keeps refunding an unacknowledged purchase after three days, so acknowledging is not
     * optional - skipping it takes the money back from under a paying user.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_REMOVE_ADS)) return
        setPro(true)
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { }
    }

    private fun setPro(owned: Boolean) {
        prefs.setPro(owned)
        proState.value = owned
    }
}
