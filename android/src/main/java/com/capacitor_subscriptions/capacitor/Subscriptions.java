package com.capacitor_subscriptions.capacitor;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.UnfetchedProduct;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PluginCall;

import java.util.ArrayList;
import java.util.List;

public class Subscriptions {

    static final String TAG = "Subscriptions";

    /** The connection to Google Play Billing could not be set up (e.g. Play Store missing, no Google account). */
    static final int RESPONSE_SETUP_FAILED = 500;
    static final String MESSAGE_SETUP_FAILED = "Android: BillingClient failed to initialise";

    /** Google Play Billing answered with a response code other than OK: the outcome is unknown, not "nothing found". */
    static final int RESPONSE_BILLING_ERROR = 502;
    static final String MESSAGE_BILLING_ERROR = "Android: Google Play Billing returned an error";

    private final SubscriptionsPlugin plugin;
    private final BillingClient billingClient;

    // Calls waiting for the connection to Google Play Billing. Guarded by connectionLock.
    private final Object connectionLock = new Object();
    private final List<PendingRequest> pendingRequests = new ArrayList<>();
    private boolean connecting = false;

    private static final class PendingRequest {
        final PluginCall call;
        final Runnable action;

        PendingRequest(PluginCall call, Runnable action) {
            this.call = call;
            this.action = action;
        }
    }

    public Subscriptions(SubscriptionsPlugin plugin, BillingClient billingClient) {
        this.plugin = plugin;
        this.billingClient = billingClient;
        // Connect eagerly, so that the first call usually finds the client ready.
        startConnection();
    }

    public String echo(String value) {
        Logger.debug(TAG, "echo: " + value);
        return value;
    }

    // ---------------------------------------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------------------------------------

    /**
     * Runs {@code action} once the BillingClient is connected. Calls made before the connection is
     * ready wait for it; if the setup fails they are resolved with {@link #RESPONSE_SETUP_FAILED}.
     * A failed or dropped connection is retried by the next call.
     */
    private void ensureConnected(PluginCall call, Runnable action) {
        boolean runNow;
        synchronized (connectionLock) {
            runNow = !connecting && billingClient.isReady();
            if (!runNow) {
                pendingRequests.add(new PendingRequest(call, action));
            }
        }
        if (runNow) {
            runAction(call, action);
        } else {
            startConnection();
        }
    }

    private void startConnection() {
        synchronized (connectionLock) {
            if (connecting) {
                return;
            }
            connecting = true;
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                onConnectionResult(billingResult.getResponseCode(), billingResult.getDebugMessage());
            }

            @Override
            public void onBillingServiceDisconnected() {
                // With enableAutoServiceReconnection() the library reconnects by itself on the next API call;
                // requests still waiting for the setup are answered here, so that none is left hanging.
                Logger.warn(TAG, "Disconnected from the Google Play Billing service");
                onConnectionResult(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, "Billing service disconnected");
            }
        });
    }

    private void onConnectionResult(int responseCode, String debugMessage) {
        List<PendingRequest> requests;
        synchronized (connectionLock) {
            connecting = false;
            requests = new ArrayList<>(pendingRequests);
            pendingRequests.clear();
        }

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Logger.warn(TAG, "BillingClient setup failed with response code " + responseCode);
        }

        for (PendingRequest request : requests) {
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                runAction(request.call, request.action);
            } else {
                JSObject response = new JSObject();
                response.put("responseCode", RESPONSE_SETUP_FAILED);
                response.put("responseMessage", MESSAGE_SETUP_FAILED);
                response.put("billingResponseCode", responseCode);
                response.put("billingDebugMessage", debugMessage);
                request.call.resolve(response);
            }
        }
    }

    private void runAction(PluginCall call, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Logger.error(TAG, "Unexpected error while calling Google Play Billing", e);
            call.reject("Unexpected error while calling Google Play Billing", e);
        }
    }

    private static boolean isOk(BillingResult billingResult) {
        return billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    private static void resolveBillingError(PluginCall call, BillingResult billingResult) {
        Logger.warn(TAG, call.getMethodName() + ": Google Play Billing response code " + billingResult.getResponseCode());
        JSObject response = new JSObject();
        response.put("responseCode", RESPONSE_BILLING_ERROR);
        response.put("responseMessage", MESSAGE_BILLING_ERROR);
        response.put("billingResponseCode", billingResult.getResponseCode());
        response.put("billingDebugMessage", billingResult.getDebugMessage());
        call.resolve(response);
    }

    // ---------------------------------------------------------------------------------------------
    // Products
    // ---------------------------------------------------------------------------------------------

    private static ProductDetails findProduct(QueryProductDetailsResult result, String productIdentifier) {
        List<ProductDetails> list = result != null ? result.getProductDetailsList() : null;
        if (list != null) {
            for (ProductDetails details : list) {
                if (productIdentifier.equals(details.getProductId())) {
                    return details;
                }
            }
        }
        return null;
    }

    /** Status of the product in the unfetched list, or {@code PRODUCT_NOT_FOUND} if it is in neither list. */
    private static int unfetchedStatusCode(QueryProductDetailsResult result, String productIdentifier) {
        List<UnfetchedProduct> list = result != null ? result.getUnfetchedProductList() : null;
        if (list != null) {
            for (UnfetchedProduct product : list) {
                if (productIdentifier.equals(product.getProductId())) {
                    return product.getStatusCode();
                }
            }
        }
        return UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND;
    }

    private static boolean productDoesNotExist(int unfetchedStatusCode) {
        return unfetchedStatusCode == UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND
                || unfetchedStatusCode == UnfetchedProduct.StatusCode.INVALID_PRODUCT_ID_FORMAT;
    }

    /**
     * The subscription offer bought by {@link #purchaseProduct}: the first one returned by Play.
     * Unchanged since the first version of the plugin.
     */
    private static ProductDetails.SubscriptionOfferDetails purchasedOffer(ProductDetails productDetails) {
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        return offers.get(0);
    }

    public void getProductDetails(String productIdentifier, String productType, PluginCall call) {
        ensureConnected(call, () -> {
            QueryProductDetailsParams.Product productToFind = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productIdentifier)
                    .setProductType(productType)
                    .build();

            QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(List.of(productToFind))
                    .build();

            billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, queryProductDetailsResult) -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }

                JSObject response = new JSObject();
                ProductDetails productDetails = findProduct(queryProductDetailsResult, productIdentifier);
                JSObject data = productDetails != null ? productData(productDetails) : null;

                if (data != null) {
                    response.put("responseCode", 0);
                    response.put("responseMessage", "Successfully found the product details for given productIdentifier");
                    response.put("data", data);
                } else {
                    int status = productDetails != null
                            ? UnfetchedProduct.StatusCode.NO_ELIGIBLE_OFFER
                            : unfetchedStatusCode(queryProductDetailsResult, productIdentifier);
                    if (productDoesNotExist(status)) {
                        response.put("responseCode", 1);
                        response.put("responseMessage", "Could not find a product matching the given productIdentifier");
                    } else {
                        response.put("responseCode", 2);
                        response.put("responseMessage", "The product exists but its details could not be retrieved");
                    }
                    response.put("unfetchedStatusCode", status);
                }

                call.resolve(response);
            });
        });
    }

    /** Product data, or {@code null} if Play returned no offer or price for it. */
    private static JSObject productData(ProductDetails productDetails) {
        JSObject data = new JSObject();
        data.put("productIdentifier", productDetails.getProductId());
        data.put("displayName", productDetails.getTitle());
        data.put("description", productDetails.getDescription());
        data.put("productType", productDetails.getProductType());

        if (BillingClient.ProductType.INAPP.equals(productDetails.getProductType())) {
            ProductDetails.OneTimePurchaseOfferDetails offer = productDetails.getOneTimePurchaseOfferDetails();
            if (offer == null) {
                return null;
            }
            data.put("price", offer.getFormattedPrice());
            data.put("priceAmountMicros", offer.getPriceAmountMicros());
            data.put("priceCurrencyCode", offer.getPriceCurrencyCode());
            return data;
        }

        ProductDetails.SubscriptionOfferDetails offer = purchasedOffer(productDetails);
        if (offer == null) {
            return null;
        }
        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
        if (phases == null || phases.isEmpty()) {
            return null;
        }

        // `price` keeps its historical meaning: the first pricing phase of the offer that purchaseProduct buys.
        ProductDetails.PricingPhase firstPhase = phases.get(0);
        data.put("price", firstPhase.getFormattedPrice());
        data.put("priceAmountMicros", firstPhase.getPriceAmountMicros());
        data.put("priceCurrencyCode", firstPhase.getPriceCurrencyCode());
        data.put("basePlanId", offer.getBasePlanId());
        if (offer.getOfferId() != null) {
            data.put("offerId", offer.getOfferId());
        }

        JSArray pricingPhases = new JSArray();
        for (ProductDetails.PricingPhase phase : phases) {
            JSObject p = new JSObject();
            p.put("formattedPrice", phase.getFormattedPrice());
            p.put("priceAmountMicros", phase.getPriceAmountMicros());
            p.put("priceCurrencyCode", phase.getPriceCurrencyCode());
            p.put("billingPeriod", phase.getBillingPeriod());
            p.put("billingCycleCount", phase.getBillingCycleCount());
            p.put("recurrenceMode", phase.getRecurrenceMode());
            pricingPhases.put(p);
        }
        data.put("pricingPhases", pricingPhases);
        return data;
    }

    // ---------------------------------------------------------------------------------------------
    // Purchases
    // ---------------------------------------------------------------------------------------------

    /** Fields describing a purchase, shared by entitlements, latest transaction and the purchase event. */
    static JSObject purchaseData(Purchase purchase) {
        JSObject data = new JSObject();
        List<String> products = purchase.getProducts();
        if (!products.isEmpty()) {
            data.put("productId", products.get(0));
        }
        data.put("productIds", new JSArray(products));
        data.put("purchaseToken", purchase.getPurchaseToken());
        data.put("isAcknowledged", purchase.isAcknowledged());
        data.put("purchaseState", purchase.getPurchaseState());
        return data;
    }

    public void getLatestTransaction(String productIdentifier, PluginCall call) {
        ensureConnected(call, () -> {
            // Billing 8 removed queryPurchaseHistoryAsync: only purchases the user currently owns are returned,
            // expired ones are no longer found. Suspended subscriptions (account hold, paused) are included
            // and flagged with isSuspended.
            QueryPurchasesParams queryPurchasesParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .includeSuspendedSubscriptions(true)
                    .build();

            billingClient.queryPurchasesAsync(queryPurchasesParams, (billingResult, list) -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }

                JSObject response = new JSObject();
                for (Purchase purchase : list) {
                    if (purchase.getProducts().contains(productIdentifier)) {
                        JSObject data = purchaseData(purchase);
                        data.put("productId", productIdentifier);
                        data.put("isSuspended", purchase.isSuspended());

                        response.put("responseCode", 0);
                        response.put("responseMessage", "Successfully found the latest transaction matching given productIdentifier");
                        response.put("data", data);
                        call.resolve(response);
                        return;
                    }
                }

                response.put("responseCode", 3);
                response.put("responseMessage", "No transaction for given productIdentifier, or it could not be verified");
                call.resolve(response);
            });
        });
    }

    public void getCurrentEntitlements(String productType, PluginCall call) {
        ensureConnected(call, () -> {
            QueryPurchasesParams queryPurchasesParams = QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build();

            billingClient.queryPurchasesAsync(queryPurchasesParams, (billingResult, purchaseList) -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }

                JSObject response = new JSObject();
                if (purchaseList.isEmpty()) {
                    response.put("responseCode", 1);
                    response.put("responseMessage", "No entitlements were found");
                } else {
                    JSArray entitlements = new JSArray();
                    for (Purchase purchase : purchaseList) {
                        entitlements.put(purchaseData(purchase));
                    }
                    response.put("responseCode", 0);
                    response.put("responseMessage", "Successfully found all entitlements across all product types");
                    response.put("data", entitlements);
                }
                call.resolve(response);
            });
        });
    }

    public void purchaseProduct(String productIdentifier, String accountId, String productType, PluginCall call) {
        ensureConnected(call, () -> {
            QueryProductDetailsParams.Product productToFind = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productIdentifier)
                    .setProductType(productType)
                    .build();

            QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(List.of(productToFind))
                    .build();

            billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, queryProductDetailsResult) -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }

                ProductDetails productDetails = findProduct(queryProductDetailsResult, productIdentifier);
                if (productDetails == null) {
                    JSObject response = failedToOpen();
                    response.put("unfetchedStatusCode", unfetchedStatusCode(queryProductDetailsResult, productIdentifier));
                    call.resolve(response);
                    return;
                }

                BillingFlowParams.ProductDetailsParams.Builder productParams =
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails);
                ProductDetails.SubscriptionOfferDetails offer = purchasedOffer(productDetails);
                if (offer != null) {
                    // Subscriptions: the first offer, as in every previous version of the plugin.
                    productParams.setOfferToken(offer.getOfferToken());
                }

                BillingFlowParams.Builder builder = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(List.of(productParams.build()));
                if (accountId != null) {
                    builder.setObfuscatedAccountId(accountId);
                }
                BillingFlowParams billingFlowParams = builder.build();

                Activity activity = plugin.getActivity();
                if (activity == null) {
                    call.resolve(failedToOpen());
                    return;
                }

                // launchBillingFlow must run on the UI thread.
                activity.runOnUiThread(() -> {
                    BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
                    if (isOk(result)) {
                        JSObject response = new JSObject();
                        response.put("responseCode", 0);
                        response.put("responseMessage", "Successfully opened native popover");
                        call.resolve(response);
                    } else {
                        Logger.warn(TAG, "launchBillingFlow failed with response code " + result.getResponseCode());
                        JSObject response = failedToOpen();
                        response.put("billingResponseCode", result.getResponseCode());
                        response.put("billingDebugMessage", result.getDebugMessage());
                        call.resolve(response);
                    }
                });
            });
        });
    }

    private static JSObject failedToOpen() {
        JSObject response = new JSObject();
        response.put("responseCode", 1);
        response.put("responseMessage", "Failed to open native popover");
        return response;
    }

    public void acknowledgePurchase(String purchaseToken, PluginCall call) {
        ensureConnected(call, () -> {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build();
            billingClient.acknowledgePurchase(params, billingResult -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }
                JSObject response = new JSObject();
                response.put("responseCode", 0);
                response.put("responseMessage", "Successfully acknowledged the purchase");
                call.resolve(response);
            });
        });
    }

    public void consumePurchase(String purchaseToken, PluginCall call) {
        ensureConnected(call, () -> {
            ConsumeParams params = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build();
            billingClient.consumeAsync(params, (billingResult, token) -> {
                if (!isOk(billingResult)) {
                    resolveBillingError(call, billingResult);
                    return;
                }
                JSObject response = new JSObject();
                response.put("responseCode", 0);
                response.put("responseMessage", "Successfully consumed the purchase");
                call.resolve(response);
            });
        });
    }
}
