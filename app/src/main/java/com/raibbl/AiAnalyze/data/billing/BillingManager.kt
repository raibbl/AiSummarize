package com.raibbl.AiAnalyze.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SubscriptionState {
    data object Loading : SubscriptionState()
    data object Free : SubscriptionState()
    data object Pro : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID = "briefly_pro_monthly"
    }

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private var billingClient: BillingClient = buildClient()

    private fun buildClient(): BillingClient {
        return BillingClient.newBuilder(context)
            .setListener(this)
            // PendingPurchasesParams requires at least one product type to be
            // enabled; one-time products must be enabled even though this app
            // currently only sells a subscription.
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    fun startConnection() {
        // Recreate if previous instance was closed
        if (!billingClient.isReady) {
            billingClient = buildClient()
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingSubscription()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _subscriptionState.value = SubscriptionState.Free
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // Don't set error state — the client will retry automatically
            }
        })
    }

    private fun checkExistingSubscription() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasActiveSub) {
                    // Acknowledge any unacknowledged purchases
                    purchases.filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }.forEach { acknowledgePurchase(it) }

                    _subscriptionState.value = SubscriptionState.Pro
                } else {
                    _subscriptionState.value = SubscriptionState.Free
                }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                _subscriptionState.value = SubscriptionState.Free
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity) {
        Log.d(TAG, "launchSubscriptionFlow called, billingClient.isReady=${billingClient.isReady}")

        if (!billingClient.isReady) {
            Log.w(TAG, "Billing client not ready, reconnecting...")
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                return@queryProductDetailsAsync
            }

            val productDetails = queryProductDetailsResult.productDetailsList.firstOrNull() ?: run {
                Log.e(TAG, "Product $PRODUCT_ID not found")
                return@queryProductDetailsAsync
            }

            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()?.offerToken ?: run {
                Log.e(TAG, "No offer token found for $PRODUCT_ID")
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            Log.d(TAG, "launchBillingFlow result: ${result.responseCode} - ${result.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                        _subscriptionState.value = SubscriptionState.Pro
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
