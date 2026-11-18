import type { PluginListenerHandle } from "@capacitor/core";
export interface SubscriptionsPlugin {
    /**
     * A test method which just returns what is passed in
     */
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    /**
     * Receives a product ID and returns the product details.
     *
     * On Android `productType` selects subscriptions (default) or one-time products. For subscriptions,
     * `price` is the first pricing phase of the offer that `purchaseProduct` buys, and `pricingPhases`
     * lists all the phases of that offer.
     */
    getProductDetails(options: {
        productIdentifier: string;
        productType?: AndroidProductType;
    }): Promise<ProductDetailsResponse>;
    /**
     * Starts the purchase of a product.
     *
     * On iOS the promise resolves with the transaction. On Android it resolves as soon as the native popover
     * is open: the outcome arrives with the `ANDROID-PURCHASE-RESPONSE` event.
     *
     * `accountId` (optional): on iOS it must be a UUID and becomes the `appAccountToken` (otherwise the
     * call resolves with `1` "Invalid accountId"); on Android it becomes the `obfuscatedAccountId`.
     *
     * `acknowledgePurchases` (Android, default `true`): when `false` the plugin does not acknowledge the
     * purchase, and the app or its backend must do it within 3 days (see `acknowledgePurchase`).
     */
    purchaseProduct(options: {
        productIdentifier: string;
        accountId?: string;
        acknowledgePurchases?: boolean;
        productType?: AndroidProductType;
    }): Promise<PurchaseProductResponse>;
    /**
     * Returns the purchases the user currently owns.
     *
     * On Android each call returns one product type, `productType` (default `subs`); on iOS all the current
     * entitlements are returned. On Android each entry has `isAcknowledged` and `purchaseState`, so that purchases still waiting to be
     * acknowledged can be recovered.
     */
    getCurrentEntitlements(options: {
        productType?: AndroidProductType;
        sync?: boolean;
    }): Promise<CurrentEntitlementsResponse>;
    /**
     * Returns the latest transaction of a product.
     *
     * On Android (Billing 8) only subscriptions the user currently owns are found, including suspended
     * ones (account hold, paused), flagged with `isSuspended`. Expired subscriptions are not returned.
     */
    getLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<LatestTransactionResponse>;
    /**
     * Opens the refund request sheet for the latest transaction of a product. iOS only: on Android it
     * resolves with `-1`.
     */
    refundLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<RefundLatestTransactionResponse>;
    /**
     * Opens the store page where the user manages subscriptions.
     * On Android `productIdentifier` and `bid` (the application id) are required.
     */
    manageSubscriptions(options?: {
        productIdentifier?: string;
        bid?: string;
    }): any;
    /**
     * Acknowledges an Android purchase. Needed when `purchaseProduct` was called with
     * `acknowledgePurchases: false` and the backend does not acknowledge it: Google refunds purchases
     * not acknowledged within 3 days. On iOS it resolves with `-1`.
     *
     * @since 8.0.0
     */
    acknowledgePurchase(options: {
        purchaseToken: string;
    }): Promise<AcknowledgePurchaseResponse>;
    /**
     * Consumes an Android one-time purchase, so that it can be bought again. Consuming also
     * acknowledges it. On iOS it resolves with `-1`: consumables are finished by the plugin.
     *
     * @since 8.0.0
     */
    consumePurchase(options: {
        purchaseToken: string;
    }): Promise<ConsumePurchaseResponse>;
    /**
     * **Deprecated**: does nothing and resolves immediately, on every platform. Remove the call.
     *
     * @deprecated The server-side verification it configured is no longer used. It will be removed in a
     * future major version.
     */
    setGoogleVerificationDetails(options: {
        googleVerifyEndpoint: string;
        bid: string;
    }): Promise<void>;
    /**
     * Android: outcome of a purchase. Fired when the billing flow started by `purchaseProduct` ends, and for
     * purchases made outside the app (promo codes, Play Store) while it is running.
     */
    addListener(eventName: 'ANDROID-PURCHASE-RESPONSE', listenerFunc: (response: AndroidPurchasedTrigger) => void): Promise<PluginListenerHandle>;
}
/**
 * Android-only response codes, possible on every method that talks to Google Play:
 * - `500`: the connection to Google Play Billing could not be set up (`billingResponseCode` says why);
 * - `502`: Google Play Billing returned an error. The outcome is unknown: it does not mean "nothing found".
 */
export type AndroidBillingErrorResponseCode = 500 | 502;
export type AndroidBillingErrorResponseMessage = "Android: BillingClient failed to initialise" | "Android: Google Play Billing returned an error";
/**
 * Extra fields of Android responses with code `500` or `502`, and of failed purchases.
 */
export interface AndroidBillingErrorDetails {
    /** `BillingClient.BillingResponseCode` returned by Google Play. */
    billingResponseCode?: number;
    /** Debug message returned by Google Play. */
    billingDebugMessage?: string;
}
export interface Product {
    productIdentifier: string;
    /** Formatted price. On Android subscriptions: the first pricing phase of the offer bought by `purchaseProduct`. */
    price: string;
    displayName: string;
    description: string;
    /** Android: price of `price` in micro-units. */
    priceAmountMicros?: number;
    /** Android: ISO 4217 currency code of `price`. */
    priceCurrencyCode?: string;
    /** Android: `subs` or `inapp`. */
    productType?: AndroidProductType;
    /** Android subscriptions: base plan of the offer bought by `purchaseProduct`. */
    basePlanId?: string;
    /** Android subscriptions: offer bought by `purchaseProduct`; absent when it is the base plan. */
    offerId?: string;
    /** Android subscriptions: all the pricing phases of the offer bought by `purchaseProduct`. */
    pricingPhases?: AndroidPricingPhase[];
}
export interface AndroidPricingPhase {
    formattedPrice: string;
    priceAmountMicros: number;
    priceCurrencyCode: string;
    /** ISO 8601 period, e.g. `P1M`. */
    billingPeriod: string;
    billingCycleCount: number;
    /** 1 infinite recurring, 2 finite recurring, 3 non recurring. */
    recurrenceMode: number;
}
export interface Transaction {
    /** Product of the transaction. On Android, the first product of the purchase. */
    productId: string;
    jws?: string;
    purchaseToken?: string;
    /** Android: all the products of the purchase. */
    productIds?: string[];
    /** Android: whether the purchase has been acknowledged. */
    isAcknowledged?: boolean;
    /** Android: state of the purchase. */
    purchaseState?: AndroidPurchaseState;
    /** Android, `getLatestTransaction` only: subscription in account hold or paused. */
    isSuspended?: boolean;
}
export interface LatestTransactionResponse extends AndroidBillingErrorDetails {
    responseCode: LatestTransactionResponseCode;
    responseMessage: LatestTransactionResponseMessage;
    data?: Transaction;
}
export type LatestTransactionResponseCode = -1 | 0 | 1 | 2 | 3 | AndroidBillingErrorResponseCode;
export type LatestTransactionResponseMessage = "Incompatible with web" | "Successfully found the latest transaction matching given productIdentifier" | "Latest transaction found" | "Could not find a product matching the given productIdentifier" | "No transaction for given productIdentifier, or it could not be verified" | "Unknown problem trying to retrieve latest transaction" | AndroidBillingErrorResponseMessage;
export interface RefundLatestTransactionResponse {
    responseCode: RefundLatestTransactionResponseCode;
    responseMessage: RefundLatestTransactionResponseMessage;
}
export type RefundLatestTransactionResponseCode = -1 | 0 | 1 | 2 | 3 | 4;
export type RefundLatestTransactionResponseMessage = "Incompatible with web" | "Not supported on Android" | "Refund request submitted" | "User closed the refund request sheet" | "Refund request sheet closed" | "Successfully found the latest transaction matching given productIdentifier" | "Could not find a product matching the given productIdentifier" | "No transaction for given productIdentifier, or it could not be verified" | "Unknown problem trying to refund latest transaction" | "Problem getting UIScene";
export interface CurrentEntitlementsResponse extends AndroidBillingErrorDetails {
    responseCode: CurrentEntitlementsResponseCode;
    responseMessage: CurrentEntitlementsResponseMessage;
    data: Transaction[];
}
export type CurrentEntitlementsResponseCode = -1 | 0 | 1 | 2 | AndroidBillingErrorResponseCode;
export type CurrentEntitlementsResponseMessage = "Incompatible with web" | "Successfully found all entitlements across all product types" | "No entitlements were found" | "Unknown problem trying to retrieve entitlements" | AndroidBillingErrorResponseMessage;
export interface PurchaseProductResponse extends AndroidBillingErrorDetails {
    responseCode: PurchaseProductIOSResponseCode | PurchaseProductAndroidResponseCode;
    responseMessage: PurchaseProductIOSResponseMessage | PurchaseProductAndroidResponseMessage;
    data?: Transaction;
    /** Android: `UnfetchedProduct.StatusCode` when the product could not be retrieved. */
    unfetchedStatusCode?: AndroidUnfetchedStatusCode;
}
export type PurchaseProductIOSResponseCode = -1 | 0 | 1 | 2 | 3 | 4 | 5;
export type PurchaseProductIOSResponseMessage = "Incompatible with web" | "Successfully purchased product" | "Could not find a product matching the given productIdentifier" | "Invalid accountId" | "Product seems to have been purchased but the transaction failed verification" | "User closed the native popover before purchasing" | "Product request made but is currently pending - likely due to parental restrictions" | "An unknown error occurred whilst in the purchasing process";
/**
 * `1` also when the popover could not be opened (`billingResponseCode` says why, e.g. `7` ITEM_ALREADY_OWNED).
 */
export type PurchaseProductAndroidResponseCode = -1 | 0 | 1 | AndroidBillingErrorResponseCode;
export type PurchaseProductAndroidResponseMessage = "Incompatible with web" | "Successfully opened native popover" | "Failed to open native popover" | AndroidBillingErrorResponseMessage;
export interface ProductDetailsResponse extends AndroidBillingErrorDetails {
    responseCode: ProductDetailsResponseCode;
    responseMessage: ProductDetailsResponseMessage;
    data?: Product;
    /** Android: `UnfetchedProduct.StatusCode` when the product could not be retrieved. */
    unfetchedStatusCode?: AndroidUnfetchedStatusCode;
}
/**
 * `1`: the product does not exist. `2` (Android): it exists but could not be retrieved, e.g. no offer the
 * user is eligible for (see `unfetchedStatusCode`).
 */
export type ProductDetailsResponseCode = -1 | 0 | 1 | 2 | AndroidBillingErrorResponseCode;
export type ProductDetailsResponseMessage = "Incompatible with web" | "Successfully found the product details for given productIdentifier" | "Could not find a product matching the given productIdentifier" | "The product exists but its details could not be retrieved" | AndroidBillingErrorResponseMessage;
export interface AcknowledgePurchaseResponse extends AndroidBillingErrorDetails {
    responseCode: AcknowledgePurchaseResponseCode;
    responseMessage: AcknowledgePurchaseResponseMessage;
}
export type AcknowledgePurchaseResponseCode = -1 | 0 | AndroidBillingErrorResponseCode;
export type AcknowledgePurchaseResponseMessage = "Incompatible with web" | "Not supported on iOS" | "Successfully acknowledged the purchase" | AndroidBillingErrorResponseMessage;
export interface ConsumePurchaseResponse extends AndroidBillingErrorDetails {
    responseCode: ConsumePurchaseResponseCode;
    responseMessage: ConsumePurchaseResponseMessage;
}
export type ConsumePurchaseResponseCode = -1 | 0 | AndroidBillingErrorResponseCode;
export type ConsumePurchaseResponseMessage = "Incompatible with web" | "Not supported on iOS" | "Successfully consumed the purchase" | AndroidBillingErrorResponseMessage;
export interface AndroidPurchasedTrigger {
    /**
     * `true` when the purchase is paid and, if the plugin had to acknowledge it, the acknowledge succeeded.
     * `false` for cancelled, failed or pending purchases, and when the acknowledge failed.
     */
    successful: boolean;
    /** Present whenever the event refers to a purchase, also when `successful` is `false`. */
    purchaseToken: string;
    /** First product of the purchase. */
    productId?: string;
    productIds?: string[];
    isAcknowledged?: boolean;
    purchaseState?: AndroidPurchaseState;
    /** Google Play response code of the purchase, or of the acknowledge when it failed. */
    billingResponseCode?: number;
}
export declare enum AndroidProductType {
    SUBS = "subs",
    INAPP = "inapp"
}
/** `Purchase.PurchaseState` of Google Play Billing. */
export declare enum AndroidPurchaseState {
    UNSPECIFIED_STATE = 0,
    PURCHASED = 1,
    PENDING = 2
}
/** `UnfetchedProduct.StatusCode` of Google Play Billing. */
export declare enum AndroidUnfetchedStatusCode {
    UNKNOWN = 0,
    INVALID_PRODUCT_ID_FORMAT = 2,
    PRODUCT_NOT_FOUND = 3,
    NO_ELIGIBLE_OFFER = 4
}
