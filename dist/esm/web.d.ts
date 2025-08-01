import { WebPlugin } from '@capacitor/core';
import { SubscriptionsPlugin, ProductDetailsResponse, PurchaseProductResponse, CurrentEntitlementsResponse, LatestTransactionResponse, RefundLatestTransactionResponse, AndroidProductType } from './definitions';
export declare class SubscriptionsWeb extends WebPlugin implements SubscriptionsPlugin {
    setGoogleVerificationDetails(options: {
        googleVerifyEndpoint: string;
        bid: string;
    }): void;
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    getProductDetails(options: {
        productIdentifier: string;
    }): Promise<ProductDetailsResponse>;
    purchaseProduct(options: {
        productIdentifier: string;
        accountId?: string;
        acknowledgePurchases?: boolean;
        productType?: AndroidProductType;
    }): Promise<PurchaseProductResponse>;
    getCurrentEntitlements(options: {
        sync: boolean;
        productType?: AndroidProductType;
    }): Promise<CurrentEntitlementsResponse>;
    getLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<LatestTransactionResponse>;
    refundLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<RefundLatestTransactionResponse>;
    manageSubscriptions(): void;
}
