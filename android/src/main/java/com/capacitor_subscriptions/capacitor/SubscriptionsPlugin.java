package com.capacitor_subscriptions.capacitor;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginResult;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

@CapacitorPlugin(name = "Subscriptions")
public class SubscriptionsPlugin extends Plugin {

    private Subscriptions implementation;

    private BillingClient billingClient;

    private Boolean acknowledgePurchases = true;

    public SubscriptionsPlugin () {

    }

    // This listener is fired upon completing the billing flow, it is vital to call the acknowledgePurchase
    // method on the billingClient, with the purchase token otherwise Google will automatically cancel the subscription
    // shortly after the purchase
    private PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {

        JSObject response = new JSObject();

        if(purchases != null) {
            for (int i = 0; i < purchases.size(); i++) {

                Purchase currentPurchase = purchases.get(i);
                if (this.acknowledgePurchases && !currentPurchase.isAcknowledged() && billingResult.getResponseCode() == 0 && currentPurchase.getPurchaseState() != 2) {

                    AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(currentPurchase.getPurchaseToken())
                            .build();

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                        Log.i("Purchase ack", currentPurchase.getOriginalJson());
                        billingResult1.getResponseCode();

                        response.put("successful", true);
                        response.put("purchaseToken", currentPurchase.getPurchaseToken());

                        notifyListeners("ANDROID-PURCHASE-RESPONSE", response);
                    });
                } else if (!this.acknowledgePurchases && billingResult.getResponseCode() == 0 && currentPurchase.getPurchaseState() != 2) {
                    response.put("successful", true);
                    response.put("purchaseToken", currentPurchase.getPurchaseToken());
                    notifyListeners("ANDROID-PURCHASE-RESPONSE", response);
                } else if (billingResult.getResponseCode() == 0 && currentPurchase.getPurchaseState() != 2) {
                    response.put("successful", false);
                    notifyListeners("ANDROID-PURCHASE-RESPONSE", response);
                } else {
                    response.put("successful", false);
                    notifyListeners("ANDROID-PURCHASE-RESPONSE", response);
                }

            }
        } else {
            response.put("successful", false);
            notifyListeners("ANDROID-PURCHASE-RESPONSE", response);
        }

    };

    @Override
    public void load() {

        this.billingClient = BillingClient.newBuilder(getContext())
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        implementation = new Subscriptions(this, billingClient);

    }

    @PluginMethod
    public void setGoogleVerificationDetails(PluginCall call) {
        String googleVerifyEndpoint = call.getString("googleVerifyEndpoint");
        String bid = call.getString("bid");

        if(googleVerifyEndpoint != null && bid != null) {
            implementation.setGoogleVerificationDetails(googleVerifyEndpoint, bid);
        } else {
            call.reject("Missing required parameters");
        }
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

        if (productIdentifier == null) {
            call.reject("Must provide a productID");
        }

        implementation.getProductDetails(productIdentifier, call);

    }

    @PluginMethod
    public void purchaseProduct(PluginCall call) {

        String productIdentifier = call.getString("productIdentifier");
        String accountId = call.getString("accountId");
        String productType = call.getString("productType", "subs"); // Default to subscriptions if not provided

        this.acknowledgePurchases = call.getBoolean("acknowledgePurchases") != null ? call.getBoolean("acknowledgePurchases") : Boolean.TRUE;

        if(productIdentifier == null) {
            call.reject("Must provide a productID");
        }

        implementation.purchaseProduct(productIdentifier, accountId, productType, call);

    }

    @PluginMethod
    public void getLatestTransaction(PluginCall call) {

        String productIdentifier = call.getString("productIdentifier");

        if(productIdentifier == null) {
            call.reject("Must provide a productID");
        }

        implementation.getLatestTransaction(productIdentifier, call);

    }

    @PluginMethod
    public void getCurrentEntitlements(PluginCall call) {
        String productType = call.getString("productType", "subs"); // Default to subscriptions if not provided

        implementation.getCurrentEntitlements(productType, call);

    }

    @PluginMethod
    public void manageSubscriptions(PluginCall call) {

        String productIdentifier = call.getString("productIdentifier");
        String bid = call.getString("bid");

        if(productIdentifier == null) {
            call.reject("Must provide a productID");
        }

        if(bid == null) {
            call.reject("Must provide a bundleID");
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?sku=" + productIdentifier + "&package=" + bid));
        getActivity().startActivity(browserIntent);
    }

}

