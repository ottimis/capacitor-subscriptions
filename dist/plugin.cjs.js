'use strict';

var core = require('@capacitor/core');

exports.AndroidProductType = void 0;
(function (AndroidProductType) {
    AndroidProductType["SUBS"] = "subs";
    AndroidProductType["INAPP"] = "inapp";
})(exports.AndroidProductType || (exports.AndroidProductType = {}));
/** `Purchase.PurchaseState` of Google Play Billing. */
exports.AndroidPurchaseState = void 0;
(function (AndroidPurchaseState) {
    AndroidPurchaseState[AndroidPurchaseState["UNSPECIFIED_STATE"] = 0] = "UNSPECIFIED_STATE";
    AndroidPurchaseState[AndroidPurchaseState["PURCHASED"] = 1] = "PURCHASED";
    AndroidPurchaseState[AndroidPurchaseState["PENDING"] = 2] = "PENDING";
})(exports.AndroidPurchaseState || (exports.AndroidPurchaseState = {}));
/** `UnfetchedProduct.StatusCode` of Google Play Billing. */
exports.AndroidUnfetchedStatusCode = void 0;
(function (AndroidUnfetchedStatusCode) {
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["UNKNOWN"] = 0] = "UNKNOWN";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["INVALID_PRODUCT_ID_FORMAT"] = 2] = "INVALID_PRODUCT_ID_FORMAT";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["PRODUCT_NOT_FOUND"] = 3] = "PRODUCT_NOT_FOUND";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["NO_ELIGIBLE_OFFER"] = 4] = "NO_ELIGIBLE_OFFER";
})(exports.AndroidUnfetchedStatusCode || (exports.AndroidUnfetchedStatusCode = {}));

const Subscriptions = core.registerPlugin('Subscriptions', {
    web: () => Promise.resolve().then(function () { return web; }).then(m => new m.SubscriptionsWeb()),
});

class SubscriptionsWeb extends core.WebPlugin {
    /** @deprecated Does nothing. */
    async setGoogleVerificationDetails(options) {
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async getProductDetails(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async purchaseProduct(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async getCurrentEntitlements(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
            data: []
        };
    }
    async getLatestTransaction(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async refundLatestTransaction(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async acknowledgePurchase(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    async consumePurchase(options) {
        return {
            responseCode: -1,
            responseMessage: 'Incompatible with web',
        };
    }
    manageSubscriptions(options) {
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    SubscriptionsWeb: SubscriptionsWeb
});

exports.Subscriptions = Subscriptions;
//# sourceMappingURL=plugin.cjs.js.map
