import { WebPlugin } from '@capacitor/core';
export class SubscriptionsWeb extends WebPlugin {
    /** @deprecated Does nothing. */
    async setGoogleVerificationDetails(options) {
        options;
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async getProductDetails(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async purchaseProduct(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async getCurrentEntitlements(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
            data: []
        };
    }
    async getLatestTransaction(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async refundLatestTransaction(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async acknowledgePurchase(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async consumePurchase(options) {
        options;
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    manageSubscriptions(options) {
        options;
    }
}
//# sourceMappingURL=web.js.map