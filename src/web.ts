import { WebPlugin } from '@capacitor/core';

import {
  SubscriptionsPlugin,
  ProductDetailsResponse,
  PurchaseProductResponse,
  CurrentEntitlementsResponse,
  LatestTransactionResponse,
  RefundLatestTransactionResponse, AndroidProductType
} from './definitions';

export class SubscriptionsWeb extends WebPlugin implements SubscriptionsPlugin {
  setGoogleVerificationDetails(options: { googleVerifyEndpoint: string, bid: string }): void {
    options;
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
  async getProductDetails(options: { productIdentifier: string }): Promise< ProductDetailsResponse > {
    options;
    return {
      responseCode: -1,
      responseMessage: 'Incompatible with web',
    }
  }
  async purchaseProduct(options: { productIdentifier: string, accountId?: string, acknowledgePurchases?: boolean, productType?: AndroidProductType }): Promise< PurchaseProductResponse > {
    options;
    return {
      responseCode: -1,
      responseMessage: 'Incompatible with web',
    }
  }
  async getCurrentEntitlements(options: { sync: boolean, productType?: AndroidProductType }): Promise< CurrentEntitlementsResponse > {
    options;
    return {
      responseCode: -1,
      responseMessage: 'Incompatible with web',
      data: []
    }
  }
  async getLatestTransaction(options: {productIdentifier: string}): Promise< LatestTransactionResponse > {
    options;
    return {
      responseCode: -1,
      responseMessage: 'Incompatible with web',
    }
  }

  async refundLatestTransaction(options: {productIdentifier: string}): Promise< RefundLatestTransactionResponse > {
    options;
    return {
      responseCode: -1,
      responseMessage: 'Incompatible with web',
    }
  }
  manageSubscriptions(): void {}
}
