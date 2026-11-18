import { WebPlugin } from '@capacitor/core';
import type { SubscriptionsPlugin, ProductDetailsResponse, PurchaseProductResponse, CurrentEntitlementsResponse, LatestTransactionResponse, RefundLatestTransactionResponse, AcknowledgePurchaseResponse, ConsumePurchaseResponse, AndroidProductType } from './definitions';
export declare class SubscriptionsWeb extends WebPlugin implements SubscriptionsPlugin {
    /** @deprecated Does nothing. */
    setGoogleVerificationDetails(options: {
        googleVerifyEndpoint: string;
        bid: string;
    }): Promise<void>;
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    getProductDetails(options: {
        productIdentifier: string;
        productType?: AndroidProductType;
    }): Promise<ProductDetailsResponse>;
    purchaseProduct(options: {
        productIdentifier: string;
        accountId?: string;
        acknowledgePurchases?: boolean;
        productType?: AndroidProductType;
    }): Promise<PurchaseProductResponse>;
    getCurrentEntitlements(options: {
        sync?: boolean;
        productType?: AndroidProductType;
    }): Promise<CurrentEntitlementsResponse>;
    getLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<LatestTransactionResponse>;
    refundLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<RefundLatestTransactionResponse>;
    acknowledgePurchase(options: {
        purchaseToken: string;
    }): Promise<AcknowledgePurchaseResponse>;
    consumePurchase(options: {
        purchaseToken: string;
    }): Promise<ConsumePurchaseResponse>;
    manageSubscriptions(options?: {
        productIdentifier?: string;
        bid?: string;
    }): void;
}
