# @ottimis/capacitor-iap

Capacitor plugin for in-app purchases and subscriptions, built on **StoreKit 2** (iOS) and **Google Play Billing Library 8** (Android).

Every method returns a promise that resolves with a `responseCode` and a `responseMessage`: the plugin does not reject on store errors, so check the code.

## Compatibility

| Plugin | Capacitor | iOS | Android | Google Play Billing |
|---|---|---|---|---|
| 8.x | 8 | 15.0+ | minSdk 24, compile/target SDK 36 | 8.3.0 |
| 7.x | 7 | 14.0+ | minSdk 23, compile/target SDK 35 | 7.0.0 |

Use plugin 7.x with Capacitor 7 apps and plugin 8.x with Capacitor 8 apps. Capacitor 8 itself needs Node.js 22+, Xcode 26+ and Android Studio Otter (2025.2.1)+.

Upgrading from 7.x: read the [CHANGELOG](CHANGELOG.md), in particular the change to `getLatestTransaction` on Android.

## Install

```bash
npm install @ottimis/capacitor-iap
npx cap sync
```

### iOS: CocoaPods and Swift Package Manager

The plugin supports both dependency managers:

- **CocoaPods**: the `OttimisCapacitorIap` pod. The app's `Podfile` needs `platform :ios, '15.0'`.
- **Swift Package Manager**: the `OttimisCapacitorIap` package (`Package.swift` at the root of the plugin). `npx cap sync` adds it to the app's `CapApp-SPM` package.

Enable the *In-App Purchase* capability in Xcode.

### Android

The plugin takes `minSdkVersion`, `compileSdkVersion` and `targetSdkVersion` from the app's `variables.gradle`. With Capacitor 8 they are 24, 36 and 36.

Google Play Billing Library 8.3.0 is added to the app by the plugin.

## Usage

```typescript
import { Subscriptions, AndroidProductType } from '@ottimis/capacitor-iap';
```

### Product details

```typescript
const res = await Subscriptions.getProductDetails({ productIdentifier: 'com.example.monthly' });
if (res.responseCode === 0) {
  console.log(res.data?.price);
}
```

On Android `productType` selects subscriptions (`subs`, default) or one-time products (`inapp`). For subscriptions `price` is the **first pricing phase** of the offer that `purchaseProduct` buys, so it can be an introductory price: `pricingPhases` lists every phase, with `priceAmountMicros` and `priceCurrencyCode`.

### Purchase on iOS

On iOS the promise resolves when the purchase ends:

```typescript
const res = await Subscriptions.purchaseProduct({
  productIdentifier: 'com.example.monthly',
  accountId: userUuid, // optional, must be a UUID: becomes the appAccountToken
});
if (res.responseCode === 0) {
  // res.data.jws: the signed transaction, to verify on your backend
}
```

### Purchase on Android

On Android `purchaseProduct` resolves with `0` as soon as the Google Play popover is open. The outcome arrives with the `ANDROID-PURCHASE-RESPONSE` event, so register the listener early, when the app starts:

```typescript
await Subscriptions.addListener('ANDROID-PURCHASE-RESPONSE', (event) => {
  if (event.successful) {
    // event.purchaseToken: send it to your backend
  } else {
    // cancelled, failed, pending, or acknowledge failed (see billingResponseCode)
  }
});

const res = await Subscriptions.purchaseProduct({
  productIdentifier: 'com.example.monthly',
  accountId: userId,            // optional: becomes the obfuscatedAccountId
  acknowledgePurchases: false,  // optional, default true (see below)
});
if (res.responseCode !== 0) {
  // the popover did not open: e.g. 1 with billingResponseCode 7 (ITEM_ALREADY_OWNED), or 500/502
}
```

The event is also fired for purchases made outside the app (promo codes, Play Store) while it is running.

#### Acknowledging purchases

Google refunds purchases that are not acknowledged within 3 days.

- `acknowledgePurchases: true` (default): the plugin acknowledges the purchase as soon as it is paid, before your backend has verified it.
- `acknowledgePurchases: false`: the plugin does not acknowledge it. Do it from your backend (Google Play Developer API) or call `acknowledgePurchase({ purchaseToken })`.

Purchases not linked to a `purchaseProduct` call (promo codes, Play Store, pending purchases completed later) use the value of the last `purchaseProduct` call, or `true` if there was none.

To recover purchases that were never acknowledged (for example completed while the app was closed), read `isAcknowledged` and `purchaseState` from `getCurrentEntitlements`.

Consumable one-time products must be consumed with `consumePurchase({ purchaseToken })` before they can be bought again. Consuming also acknowledges the purchase.

### Restoring purchases

```typescript
const res = await Subscriptions.getCurrentEntitlements({ productType: AndroidProductType.SUBS });
if (res.responseCode === 0) {
  // res.data: Transaction[]
}
```

On Android each call returns one product type (`subs` by default): call it again with `inapp` for one-time products. On iOS it returns all the current entitlements.

A code other than `0` does not always mean "nothing purchased": see the response codes below.

### Latest transaction

`getLatestTransaction({ productIdentifier })` does not mean the same thing on the two platforms:

- **iOS**: the latest transaction of the product, **also when it has expired**.
- **Android** (from 8.0.0): only subscriptions the user owns now, active or in grace period, plus suspended ones (account hold, paused) flagged with `isSuspended: true`. **Expired subscriptions are not returned** (`3`): Billing 8 removed the purchase history API, so on Android the history must come from your backend (stored purchase tokens, Google Play Developer API, Real-time Developer Notifications).

### Response codes

Each method lists its codes in the [API](#api) section. These apply to every method:

| Code | Meaning |
|---|---|
| `0` | Success. |
| `-1` | Not available on this platform: web (`Incompatible with web`), `refundLatestTransaction` on Android, `acknowledgePurchase` and `consumePurchase` on iOS. |
| `500` | Android: the connection to Google Play Billing could not be set up. `billingResponseCode` and `billingDebugMessage` say why. |
| `502` | Android: Google Play Billing returned an error. **The outcome is unknown**: do not treat it as "not purchased" and do not revoke access because of it. |

On Android, calls made while the plugin is still connecting to Google Play wait for the connection instead of failing, and the connection is restored automatically after a disconnection.

### Deprecated: `setGoogleVerificationDetails`

`setGoogleVerificationDetails` does nothing and resolves immediately on every platform. It is kept only for compatibility and will be removed in a future major version: remove the call.

## Known limitations

- **`getLatestTransaction` on Android** does not return expired subscriptions (see above).
- **Default acknowledge on Android** happens on the device, before your backend verifies the purchase. Use `acknowledgePurchases: false` if your backend acknowledges.
- **Purchases completed while the app is closed** are not acknowledged by the plugin. Acknowledge them from your backend, or find them with `getCurrentEntitlements` (`isAcknowledged: false`) and call `acknowledgePurchase`.
- **Consumables on Android** are never consumed automatically: call `consumePurchase`.
- **Subscription offers on Android**: `purchaseProduct` always buys the first offer returned by Google Play for the product, and `getProductDetails` describes that same offer. Choosing a different base plan or offer is not supported.
- **`refundLatestTransaction`** is iOS only.

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`getProductDetails(...)`](#getproductdetails)
* [`purchaseProduct(...)`](#purchaseproduct)
* [`getCurrentEntitlements(...)`](#getcurrententitlements)
* [`getLatestTransaction(...)`](#getlatesttransaction)
* [`refundLatestTransaction(...)`](#refundlatesttransaction)
* [`manageSubscriptions(...)`](#managesubscriptions)
* [`acknowledgePurchase(...)`](#acknowledgepurchase)
* [`consumePurchase(...)`](#consumepurchase)
* [`setGoogleVerificationDetails(...)`](#setgoogleverificationdetails)
* [`addListener('ANDROID-PURCHASE-RESPONSE', ...)`](#addlistenerandroid-purchase-response-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

A test method which just returns what is passed in

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### getProductDetails(...)

```typescript
getProductDetails(options: { productIdentifier: string; productType?: AndroidProductType; }) => Promise<ProductDetailsResponse>
```

Receives a product ID and returns the product details.

On Android `productType` selects subscriptions (default) or one-time products. For subscriptions,
`price` is the first pricing phase of the offer that `purchaseProduct` buys, and `pricingPhases`
lists all the phases of that offer.

| Param         | Type                                                                                                            |
| ------------- | --------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; productType?: <a href="#androidproducttype">AndroidProductType</a>; }</code> |

**Returns:** <code>Promise&lt;<a href="#productdetailsresponse">ProductDetailsResponse</a>&gt;</code>

--------------------


### purchaseProduct(...)

```typescript
purchaseProduct(options: { productIdentifier: string; accountId?: string; acknowledgePurchases?: boolean; productType?: AndroidProductType; }) => Promise<PurchaseProductResponse>
```

Starts the purchase of a product.

On iOS the promise resolves with the transaction. On Android it resolves as soon as the native popover
is open: the outcome arrives with the `ANDROID-PURCHASE-RESPONSE` event.

`accountId` (optional): on iOS it must be a UUID and becomes the `appAccountToken` (otherwise the
call resolves with `1` "Invalid accountId"); on Android it becomes the `obfuscatedAccountId`.

`acknowledgePurchases` (Android, default `true`): when `false` the plugin does not acknowledge the
purchase, and the app or its backend must do it within 3 days (see `acknowledgePurchase`).

| Param         | Type                                                                                                                                                                |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; accountId?: string; acknowledgePurchases?: boolean; productType?: <a href="#androidproducttype">AndroidProductType</a>; }</code> |

**Returns:** <code>Promise&lt;<a href="#purchaseproductresponse">PurchaseProductResponse</a>&gt;</code>

--------------------


### getCurrentEntitlements(...)

```typescript
getCurrentEntitlements(options: { productType?: AndroidProductType; sync?: boolean; }) => Promise<CurrentEntitlementsResponse>
```

Returns the purchases the user currently owns.

On Android each call returns one product type, `productType` (default `subs`); on iOS all the current
entitlements are returned. On Android each entry has `isAcknowledged` and `purchaseState`, so that purchases still waiting to be
acknowledged can be recovered.

| Param         | Type                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ productType?: <a href="#androidproducttype">AndroidProductType</a>; sync?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#currententitlementsresponse">CurrentEntitlementsResponse</a>&gt;</code>

--------------------


### getLatestTransaction(...)

```typescript
getLatestTransaction(options: { productIdentifier: string; }) => Promise<LatestTransactionResponse>
```

Returns the latest transaction of a product.

On Android (Billing 8) only subscriptions the user currently owns are found, including suspended
ones (account hold, paused), flagged with `isSuspended`. Expired subscriptions are not returned.

| Param         | Type                                        |
| ------------- | ------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#latesttransactionresponse">LatestTransactionResponse</a>&gt;</code>

--------------------


### refundLatestTransaction(...)

```typescript
refundLatestTransaction(options: { productIdentifier: string; }) => Promise<RefundLatestTransactionResponse>
```

Opens the refund request sheet for the latest transaction of a product. iOS only: on Android it
resolves with `-1`.

| Param         | Type                                        |
| ------------- | ------------------------------------------- |
| **`options`** | <code>{ productIdentifier: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#refundlatesttransactionresponse">RefundLatestTransactionResponse</a>&gt;</code>

--------------------


### manageSubscriptions(...)

```typescript
manageSubscriptions(options?: { productIdentifier?: string | undefined; bid?: string | undefined; } | undefined) => any
```

Opens the store page where the user manages subscriptions.
On Android `productIdentifier` and `bid` (the application id) are required.

| Param         | Type                                                       |
| ------------- | ---------------------------------------------------------- |
| **`options`** | <code>{ productIdentifier?: string; bid?: string; }</code> |

**Returns:** <code>any</code>

--------------------


### acknowledgePurchase(...)

```typescript
acknowledgePurchase(options: { purchaseToken: string; }) => Promise<AcknowledgePurchaseResponse>
```

Acknowledges an Android purchase. Needed when `purchaseProduct` was called with
`acknowledgePurchases: false` and the backend does not acknowledge it: Google refunds purchases
not acknowledged within 3 days. On iOS it resolves with `-1`.

| Param         | Type                                    |
| ------------- | --------------------------------------- |
| **`options`** | <code>{ purchaseToken: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#acknowledgepurchaseresponse">AcknowledgePurchaseResponse</a>&gt;</code>

**Since:** 8.0.0

--------------------


### consumePurchase(...)

```typescript
consumePurchase(options: { purchaseToken: string; }) => Promise<ConsumePurchaseResponse>
```

Consumes an Android one-time purchase, so that it can be bought again. Consuming also
acknowledges it. On iOS it resolves with `-1`: consumables are finished by the plugin.

| Param         | Type                                    |
| ------------- | --------------------------------------- |
| **`options`** | <code>{ purchaseToken: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#consumepurchaseresponse">ConsumePurchaseResponse</a>&gt;</code>

**Since:** 8.0.0

--------------------


### setGoogleVerificationDetails(...)

```typescript
setGoogleVerificationDetails(options: { googleVerifyEndpoint: string; bid: string; }) => Promise<void>
```

**Deprecated**: does nothing and resolves immediately, on every platform. Remove the call.

| Param         | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`options`** | <code>{ googleVerifyEndpoint: string; bid: string; }</code> |

--------------------


### addListener('ANDROID-PURCHASE-RESPONSE', ...)

```typescript
addListener(eventName: 'ANDROID-PURCHASE-RESPONSE', listenerFunc: (response: AndroidPurchasedTrigger) => void) => Promise<PluginListenerHandle>
```

Android: outcome of a purchase. Fired when the billing flow started by `purchaseProduct` ends, and for
purchases made outside the app (promo codes, Play Store) while it is running.

| Param              | Type                                                                                               |
| ------------------ | -------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'ANDROID-PURCHASE-RESPONSE'</code>                                                           |
| **`listenerFunc`** | <code>(response: <a href="#androidpurchasedtrigger">AndroidPurchasedTrigger</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### Interfaces


#### ProductDetailsResponse

| Prop                      | Type                                                                                    | Description                                                                     |
| ------------------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| **`responseCode`**        | <code><a href="#productdetailsresponsecode">ProductDetailsResponseCode</a></code>       |                                                                                 |
| **`responseMessage`**     | <code><a href="#productdetailsresponsemessage">ProductDetailsResponseMessage</a></code> |                                                                                 |
| **`data`**                | <code><a href="#product">Product</a></code>                                             |                                                                                 |
| **`unfetchedStatusCode`** | <code><a href="#androidunfetchedstatuscode">AndroidUnfetchedStatusCode</a></code>       | Android: `UnfetchedProduct.StatusCode` when the product could not be retrieved. |


#### Product

| Prop                    | Type                                                              | Description                                                                                                  |
| ----------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **`productIdentifier`** | <code>string</code>                                               |                                                                                                              |
| **`price`**             | <code>string</code>                                               | Formatted price. On Android subscriptions: the first pricing phase of the offer bought by `purchaseProduct`. |
| **`displayName`**       | <code>string</code>                                               |                                                                                                              |
| **`description`**       | <code>string</code>                                               |                                                                                                              |
| **`priceAmountMicros`** | <code>number</code>                                               | Android: price of `price` in micro-units.                                                                    |
| **`priceCurrencyCode`** | <code>string</code>                                               | Android: ISO 4217 currency code of `price`.                                                                  |
| **`productType`**       | <code><a href="#androidproducttype">AndroidProductType</a></code> | Android: `subs` or `inapp`.                                                                                  |
| **`basePlanId`**        | <code>string</code>                                               | Android subscriptions: base plan of the offer bought by `purchaseProduct`.                                   |
| **`offerId`**           | <code>string</code>                                               | Android subscriptions: offer bought by `purchaseProduct`; absent when it is the base plan.                   |
| **`pricingPhases`**     | <code>AndroidPricingPhase[]</code>                                | Android subscriptions: all the pricing phases of the offer bought by `purchaseProduct`.                      |


#### AndroidPricingPhase

| Prop                    | Type                | Description                                                |
| ----------------------- | ------------------- | ---------------------------------------------------------- |
| **`formattedPrice`**    | <code>string</code> |                                                            |
| **`priceAmountMicros`** | <code>number</code> |                                                            |
| **`priceCurrencyCode`** | <code>string</code> |                                                            |
| **`billingPeriod`**     | <code>string</code> | ISO 8601 period, e.g. `P1M`.                               |
| **`billingCycleCount`** | <code>number</code> |                                                            |
| **`recurrenceMode`**    | <code>number</code> | 1 infinite recurring, 2 finite recurring, 3 non recurring. |


#### PurchaseProductResponse

| Prop                      | Type                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Description                                                                     |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| **`responseCode`**        | <code>0 \| 2 \| 1 \| 5 \| 4 \| 3 \| -1 \| 500 \| 502</code>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |                                                                                 |
| **`responseMessage`**     | <code>'Incompatible with web' \| 'Could not find a product matching the given productIdentifier' \| 'Android: BillingClient failed to initialise' \| 'Android: Google Play Billing returned an error' \| 'Successfully purchased product' \| 'Invalid accountId' \| '<a href="#product">Product</a> seems to have been purchased but the transaction failed verification' \| 'User closed the native popover before purchasing' \| '<a href="#product">Product</a> request made but is currently pending - likely due to parental restrictions' \| 'An unknown error occurred whilst in the purchasing process' \| 'Successfully opened native popover' \| 'Failed to open native popover'</code> |                                                                                 |
| **`data`**                | <code><a href="#transaction">Transaction</a></code>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |                                                                                 |
| **`unfetchedStatusCode`** | <code><a href="#androidunfetchedstatuscode">AndroidUnfetchedStatusCode</a></code>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Android: `UnfetchedProduct.StatusCode` when the product could not be retrieved. |


#### Transaction

| Prop                 | Type                                                                  | Description                                                                                       |
| -------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| **`productId`**      | <code>string</code>                                                   | <a href="#product">Product</a> of the transaction. On Android, the first product of the purchase. |
| **`jws`**            | <code>string</code>                                                   |                                                                                                   |
| **`purchaseToken`**  | <code>string</code>                                                   |                                                                                                   |
| **`productIds`**     | <code>string[]</code>                                                 | Android: all the products of the purchase.                                                        |
| **`isAcknowledged`** | <code>boolean</code>                                                  | Android: whether the purchase has been acknowledged.                                              |
| **`purchaseState`**  | <code><a href="#androidpurchasestate">AndroidPurchaseState</a></code> | Android: state of the purchase.                                                                   |
| **`isSuspended`**    | <code>boolean</code>                                                  | Android, `getLatestTransaction` only: subscription in account hold or paused.                     |


#### CurrentEntitlementsResponse

| Prop                  | Type                                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------- |
| **`responseCode`**    | <code><a href="#currententitlementsresponsecode">CurrentEntitlementsResponseCode</a></code>       |
| **`responseMessage`** | <code><a href="#currententitlementsresponsemessage">CurrentEntitlementsResponseMessage</a></code> |
| **`data`**            | <code>Transaction[]</code>                                                                        |


#### LatestTransactionResponse

| Prop                  | Type                                                                                          |
| --------------------- | --------------------------------------------------------------------------------------------- |
| **`responseCode`**    | <code><a href="#latesttransactionresponsecode">LatestTransactionResponseCode</a></code>       |
| **`responseMessage`** | <code><a href="#latesttransactionresponsemessage">LatestTransactionResponseMessage</a></code> |
| **`data`**            | <code><a href="#transaction">Transaction</a></code>                                           |


#### RefundLatestTransactionResponse

| Prop                  | Type                                                                                                      |
| --------------------- | --------------------------------------------------------------------------------------------------------- |
| **`responseCode`**    | <code><a href="#refundlatesttransactionresponsecode">RefundLatestTransactionResponseCode</a></code>       |
| **`responseMessage`** | <code><a href="#refundlatesttransactionresponsemessage">RefundLatestTransactionResponseMessage</a></code> |


#### AcknowledgePurchaseResponse

| Prop                  | Type                                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------- |
| **`responseCode`**    | <code><a href="#acknowledgepurchaseresponsecode">AcknowledgePurchaseResponseCode</a></code>       |
| **`responseMessage`** | <code><a href="#acknowledgepurchaseresponsemessage">AcknowledgePurchaseResponseMessage</a></code> |


#### ConsumePurchaseResponse

| Prop                  | Type                                                                                      |
| --------------------- | ----------------------------------------------------------------------------------------- |
| **`responseCode`**    | <code><a href="#consumepurchaseresponsecode">ConsumePurchaseResponseCode</a></code>       |
| **`responseMessage`** | <code><a href="#consumepurchaseresponsemessage">ConsumePurchaseResponseMessage</a></code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### AndroidPurchasedTrigger

| Prop                      | Type                                                                  | Description                                                                                                                                                                                |
| ------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`successful`**          | <code>boolean</code>                                                  | `true` when the purchase is paid and, if the plugin had to acknowledge it, the acknowledge succeeded. `false` for cancelled, failed or pending purchases, and when the acknowledge failed. |
| **`purchaseToken`**       | <code>string</code>                                                   | Present whenever the event refers to a purchase, also when `successful` is `false`.                                                                                                        |
| **`productId`**           | <code>string</code>                                                   | First product of the purchase.                                                                                                                                                             |
| **`productIds`**          | <code>string[]</code>                                                 |                                                                                                                                                                                            |
| **`isAcknowledged`**      | <code>boolean</code>                                                  |                                                                                                                                                                                            |
| **`purchaseState`**       | <code><a href="#androidpurchasestate">AndroidPurchaseState</a></code> |                                                                                                                                                                                            |
| **`billingResponseCode`** | <code>number</code>                                                   | Google Play response code of the purchase, or of the acknowledge when it failed.                                                                                                           |


### Type Aliases


#### ProductDetailsResponseCode

`1`: the product does not exist. `2` (Android): it exists but could not be retrieved, e.g. no offer the
user is eligible for (see `unfetchedStatusCode`).

<code>-1 | 0 | 1 | 2 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### AndroidBillingErrorResponseCode

Android-only response codes, possible on every method that talks to Google Play:
- `500`: the connection to Google Play Billing could not be set up (`billingResponseCode` says why);
- `502`: Google Play Billing returned an error. The outcome is unknown: it does not mean "nothing found".

<code>500 | 502</code>


#### ProductDetailsResponseMessage

<code>"Incompatible with web" | "Successfully found the product details for given productIdentifier" | "Could not find a product matching the given productIdentifier" | "The product exists but its details could not be retrieved" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


#### AndroidBillingErrorResponseMessage

<code>"Android: BillingClient failed to initialise" | "Android: Google Play Billing returned an error"</code>


#### PurchaseProductIOSResponseCode

<code>-1 | 0 | 1 | 2 | 3 | 4 | 5</code>


#### PurchaseProductAndroidResponseCode

`1` also when the popover could not be opened (`billingResponseCode` says why, e.g. `7` ITEM_ALREADY_OWNED).

<code>-1 | 0 | 1 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### PurchaseProductIOSResponseMessage

<code>"Incompatible with web" | "Successfully purchased product" | "Could not find a product matching the given productIdentifier" | "Invalid accountId" | "<a href="#product">Product</a> seems to have been purchased but the transaction failed verification" | "User closed the native popover before purchasing" | "<a href="#product">Product</a> request made but is currently pending - likely due to parental restrictions" | "An unknown error occurred whilst in the purchasing process"</code>


#### PurchaseProductAndroidResponseMessage

<code>"Incompatible with web" | "Successfully opened native popover" | "Failed to open native popover" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


#### CurrentEntitlementsResponseCode

<code>-1 | 0 | 1 | 2 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### CurrentEntitlementsResponseMessage

<code>"Incompatible with web" | "Successfully found all entitlements across all product types" | "No entitlements were found" | "Unknown problem trying to retrieve entitlements" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


#### LatestTransactionResponseCode

<code>-1 | 0 | 1 | 2 | 3 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### LatestTransactionResponseMessage

<code>"Incompatible with web" | "Successfully found the latest transaction matching given productIdentifier" | "Latest transaction found" | "Could not find a product matching the given productIdentifier" | "No transaction for given productIdentifier, or it could not be verified" | "Unknown problem trying to retrieve latest transaction" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


#### RefundLatestTransactionResponseCode

<code>-1 | 0 | 1 | 2 | 3 | 4</code>


#### RefundLatestTransactionResponseMessage

<code>"Incompatible with web" | "Not supported on Android" | "Refund request submitted" | "User closed the refund request sheet" | "Refund request sheet closed" | "Successfully found the latest transaction matching given productIdentifier" | "Could not find a product matching the given productIdentifier" | "No transaction for given productIdentifier, or it could not be verified" | "Unknown problem trying to refund latest transaction" | "Problem getting UIScene"</code>


#### AcknowledgePurchaseResponseCode

<code>-1 | 0 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### AcknowledgePurchaseResponseMessage

<code>"Incompatible with web" | "Not supported on iOS" | "Successfully acknowledged the purchase" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


#### ConsumePurchaseResponseCode

<code>-1 | 0 | <a href="#androidbillingerrorresponsecode">AndroidBillingErrorResponseCode</a></code>


#### ConsumePurchaseResponseMessage

<code>"Incompatible with web" | "Not supported on iOS" | "Successfully consumed the purchase" | <a href="#androidbillingerrorresponsemessage">AndroidBillingErrorResponseMessage</a></code>


### Enums


#### AndroidProductType

| Members     | Value                |
| ----------- | -------------------- |
| **`SUBS`**  | <code>"subs"</code>  |
| **`INAPP`** | <code>"inapp"</code> |


#### AndroidUnfetchedStatusCode

| Members                         | Value          |
| ------------------------------- | -------------- |
| **`UNKNOWN`**                   | <code>0</code> |
| **`INVALID_PRODUCT_ID_FORMAT`** | <code>2</code> |
| **`PRODUCT_NOT_FOUND`**         | <code>3</code> |
| **`NO_ELIGIBLE_OFFER`**         | <code>4</code> |


#### AndroidPurchaseState

| Members                 | Value          |
| ----------------------- | -------------- |
| **`UNSPECIFIED_STATE`** | <code>0</code> |
| **`PURCHASED`**         | <code>1</code> |
| **`PENDING`**           | <code>2</code> |

</docgen-api>
