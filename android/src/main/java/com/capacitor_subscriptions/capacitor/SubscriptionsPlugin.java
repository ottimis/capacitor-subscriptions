package com.capacitor_subscriptions.capacitor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CapacitorPlugin(name = "Subscriptions")
public class SubscriptionsPlugin extends Plugin {

    static final String PURCHASE_EVENT = "ANDROID-PURCHASE-RESPONSE";

    private Subscriptions implementation;

    private BillingClient billingClient;

    // `acknowledgePurchases` as passed to the last purchaseProduct call for each product, and to the last call
    // overall. Purchases not started by purchaseProduct (promo codes, purchases made from the Play Store,
    // pending purchases completed later) fall back to the last value, as in the previous versions.
    private final Map<String, Boolean> acknowledgeByProduct = new ConcurrentHashMap<>();
    private volatile boolean lastAcknowledgePurchases = true;

    // Fired when the billing flow ends, and for purchases made outside the app (promo codes, Play Store).
    // A purchase must be acknowledged within 3 days, otherwise Google refunds and revokes it.
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        int responseCode = billingResult.getResponseCode();
        if (responseCode != BillingClient.BillingResponseCode.OK || purchases == null || purchases.isEmpty()) {
            JSObject response = new JSObject();
            response.put("successful", false);
            response.put("billingResponseCode", responseCode);
            notifyListeners(PURCHASE_EVENT, response);
            return;
        }

        for (Purchase purchase : purchases) {
            handlePurchase(purchase);
        }
    };

    private void handlePurchase(Purchase purchase) {
        Logger.debug(Subscriptions.TAG, "Purchase update for " + purchase.getProducts() + ", state " + purchase.getPurchaseState());

        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            // Pending (or unspecified): not paid yet. A pending purchase that completes later is delivered again.
            notifyListeners(PURCHASE_EVENT, purchaseEvent(purchase, false));
            return;
        }

        if (purchase.isAcknowledged() || !shouldAcknowledge(purchase)) {
            notifyListeners(PURCHASE_EVENT, purchaseEvent(purchase, true));
            return;
        }

        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, ackResult -> {
            boolean acknowledged = ackResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
            // One JSObject per purchase: the acknowledge is asynchronous.
            JSObject response = purchaseEvent(purchase, acknowledged);
            response.put("isAcknowledged", acknowledged);
            if (!acknowledged) {
                Logger.warn(Subscriptions.TAG, "Acknowledge failed with response code " + ackResult.getResponseCode());
                response.put("billingResponseCode", ackResult.getResponseCode());
            }
            notifyListeners(PURCHASE_EVENT, response);
        });
    }

    private boolean shouldAcknowledge(Purchase purchase) {
        for (String productId : purchase.getProducts()) {
            Boolean value = acknowledgeByProduct.get(productId);
            if (value != null) {
                return value;
            }
        }
        return lastAcknowledgePurchases;
    }

    private static JSObject purchaseEvent(Purchase purchase, boolean successful) {
        JSObject response = Subscriptions.purchaseData(purchase);
        response.put("successful", successful);
        return response;
    }

    @Override
    public void load() {
        this.billingClient = BillingClient.newBuilder(getContext())
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        implementation = new Subscriptions(this, billingClient);
    }

    /**
     * @deprecated Does nothing: the server-side verification it configured is no longer used. Kept so that
     * existing apps keep working; it resolves immediately.
     */
    @Deprecated
    @PluginMethod
    public void setGoogleVerificationDetails(PluginCall call) {
        call.resolve();
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void getProductDetails(PluginCall call) {
        String productIdentifier = call.getString("productIdentifier");
        String productType = call.getString("productType", BillingClient.ProductType.SUBS);

        if (productIdentifier == null) {
            call.reject("Must provide a productID");
            return;
        }

        implementation.getProductDetails(productIdentifier, productType, call);
    }

    @PluginMethod
    public void purchaseProduct(PluginCall call) {
        String productIdentifier = call.getString("productIdentifier");
        String accountId = call.getString("accountId");
        String productType = call.getString("productType", BillingClient.ProductType.SUBS);
        boolean acknowledgePurchases = call.getBoolean("acknowledgePurchases", Boolean.TRUE);

        if (productIdentifier == null) {
            call.reject("Must provide a productID");
            return;
        }

        acknowledgeByProduct.put(productIdentifier, acknowledgePurchases);
        lastAcknowledgePurchases = acknowledgePurchases;

        implementation.purchaseProduct(productIdentifier, accountId, productType, call);
    }

    @PluginMethod
    public void getLatestTransaction(PluginCall call) {
        String productIdentifier = call.getString("productIdentifier");

        if (productIdentifier == null) {
            call.reject("Must provide a productID");
            return;
        }

        implementation.getLatestTransaction(productIdentifier, call);
    }

    @PluginMethod
    public void getCurrentEntitlements(PluginCall call) {
        String productType = call.getString("productType", BillingClient.ProductType.SUBS);

        implementation.getCurrentEntitlements(productType, call);
    }

    @PluginMethod
    public void acknowledgePurchase(PluginCall call) {
        String purchaseToken = call.getString("purchaseToken");

        if (purchaseToken == null) {
            call.reject("Must provide a purchaseToken");
            return;
        }

        implementation.acknowledgePurchase(purchaseToken, call);
    }

    @PluginMethod
    public void consumePurchase(PluginCall call) {
        String purchaseToken = call.getString("purchaseToken");

        if (purchaseToken == null) {
            call.reject("Must provide a purchaseToken");
            return;
        }

        implementation.consumePurchase(purchaseToken, call);
    }

    @PluginMethod
    public void refundLatestTransaction(PluginCall call) {
        JSObject response = new JSObject();
        response.put("responseCode", -1);
        response.put("responseMessage", "Not supported on Android");
        call.resolve(response);
    }

    @PluginMethod
    public void manageSubscriptions(PluginCall call) {
        String productIdentifier = call.getString("productIdentifier");
        String bid = call.getString("bid");

        if (productIdentifier == null) {
            call.reject("Must provide a productID");
            return;
        }

        if (bid == null) {
            call.reject("Must provide a bundleID");
            return;
        }

        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("play.google.com")
                .path("store/account/subscriptions")
                .appendQueryParameter("sku", productIdentifier)
                .appendQueryParameter("package", bid)
                .build();

        Activity activity = getActivity();
        if (activity == null) {
            call.reject("Could not open the subscriptions page");
            return;
        }

        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            call.reject("Could not open the subscriptions page", e);
            return;
        }

        JSObject ret = new JSObject();
        ret.put("Success", "Opened");
        call.resolve(ret);
    }
}
