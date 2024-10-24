import { WebPlugin } from '@capacitor/core';
import type { SubscriptionsPlugin, ProductDetailsResponse, PurchaseProductResponse, CurrentEntitlementsResponse, LatestTransactionResponse, RefundLatestTransactionResponse } from './definitions';
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
    }): Promise<PurchaseProductResponse>;
    getCurrentEntitlements(options: {
        sync: boolean;
    }): Promise<CurrentEntitlementsResponse>;
    getLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<LatestTransactionResponse>;
    refundLatestTransaction(options: {
        productIdentifier: string;
    }): Promise<RefundLatestTransactionResponse>;
    manageSubscriptions(): void;
}
