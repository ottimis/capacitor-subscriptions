export var AndroidProductType;
(function (AndroidProductType) {
    AndroidProductType["SUBS"] = "subs";
    AndroidProductType["INAPP"] = "inapp";
})(AndroidProductType || (AndroidProductType = {}));
/** `Purchase.PurchaseState` of Google Play Billing. */
export var AndroidPurchaseState;
(function (AndroidPurchaseState) {
    AndroidPurchaseState[AndroidPurchaseState["UNSPECIFIED_STATE"] = 0] = "UNSPECIFIED_STATE";
    AndroidPurchaseState[AndroidPurchaseState["PURCHASED"] = 1] = "PURCHASED";
    AndroidPurchaseState[AndroidPurchaseState["PENDING"] = 2] = "PENDING";
})(AndroidPurchaseState || (AndroidPurchaseState = {}));
/** `UnfetchedProduct.StatusCode` of Google Play Billing. */
export var AndroidUnfetchedStatusCode;
(function (AndroidUnfetchedStatusCode) {
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["UNKNOWN"] = 0] = "UNKNOWN";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["INVALID_PRODUCT_ID_FORMAT"] = 2] = "INVALID_PRODUCT_ID_FORMAT";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["PRODUCT_NOT_FOUND"] = 3] = "PRODUCT_NOT_FOUND";
    AndroidUnfetchedStatusCode[AndroidUnfetchedStatusCode["NO_ELIGIBLE_OFFER"] = 4] = "NO_ELIGIBLE_OFFER";
})(AndroidUnfetchedStatusCode || (AndroidUnfetchedStatusCode = {}));
//# sourceMappingURL=definitions.js.map