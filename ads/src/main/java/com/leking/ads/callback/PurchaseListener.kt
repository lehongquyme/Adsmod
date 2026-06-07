package com.leking.ads.callback
interface PurchaseListener {
    fun onProductPurchased(productId: String, transactionDetails: String)
    fun displayErrorMessage(errorMsg: String)
    fun onUserCancelBilling()
}
