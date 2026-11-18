# Changelog

## 8.0.0

### Breaking changes

- **Capacitor 8.** The peer dependency is `@capacitor/core >= 8.0.0`. Use 7.x with Capacitor 7 apps.
- **Minimum platform versions.**
  - Android: minSdk 24 (was 23), compileSdk and targetSdk 36 (were 35). Android 6.0 is no longer supported. The values come from the app's `variables.gradle`; these are the defaults.
  - iOS: deployment target 15.0 (was 14.0). Nothing is lost: every method already needed iOS 15 (StoreKit 2).
- **Google Play Billing Library 8.3.0** (was 7.0.0), added to the app by the plugin.
- **`getLatestTransaction` on Android.** Billing 8 removed the purchase history API, so the method now finds only subscriptions the user owns now:

  | User's subscription | 7.x | 8.0.0 |
  |---|---|---|
  | Active, or cancelled but not yet expired | `0` | `0` |
  | Suspended (account hold, paused) | `0` | `0` with `data.isSuspended: true` |
  | Expired | `0` with the last purchase token | `3` |
  | Never purchased | `3` | `3` |

  On iOS the method still returns expired transactions. On Android the history must come from your backend.
- **New error codes on Android**, on every method that talks to Google Play:
  - `500` "Android: BillingClient failed to initialise": the connection to Google Play Billing could not be set up;
  - `502` "Android: Google Play Billing returned an error": the outcome is unknown. In 7.x the same situation returned a false "not found" (`1` or `3`).

  Both come with `billingResponseCode` and `billingDebugMessage`.
- **`purchaseProduct` on Android** resolves with `1` "Failed to open native popover" (with `billingResponseCode`, e.g. `7` ITEM_ALREADY_OWNED) when Google Play does not open the popover. In 7.x it resolved with `0`.
- **`getProductDetails` on Android** resolves with `2` "The product exists but its details could not be retrieved" (with `unfetchedStatusCode`) when Google Play knows the product but returns no details, e.g. no offer the user is eligible for.
- **`ANDROID-PURCHASE-RESPONSE`**:
  - a purchase whose acknowledge fails now has `successful: false` (was `true`);
  - a purchase that was already acknowledged now has `successful: true` (was `false`).

### Added

- `acknowledgePurchase({ purchaseToken })` and `consumePurchase({ purchaseToken })` (Android; `-1` on iOS).
- `getProductDetails` on Android: `productType` (`subs` or `inapp`) and the fields `priceAmountMicros`, `priceCurrencyCode`, `productType`, `basePlanId`, `offerId` and `pricingPhases`.
- `getCurrentEntitlements` and `ANDROID-PURCHASE-RESPONSE` on Android: `productIds`, `isAcknowledged` and `purchaseState`. The event also carries `productId`, `billingResponseCode`, and `purchaseToken` when `successful` is `false`.
- `getLatestTransaction` on Android: `isSuspended`.
- `refundLatestTransaction` on iOS, with `responseMessage` "Refund request submitted", "User closed the refund request sheet" or "Refund request sheet closed".
- New types: `AndroidPurchaseState`, `AndroidUnfetchedStatusCode`, `AndroidBillingErrorDetails`.
- **Swift Package Manager**: `Package.swift` at the root, package `OttimisCapacitorIap`. CocoaPods keeps working.

### Fixed

- Android: promises that never resolved. Calls made before the connection to Google Play is ready now wait for it, or resolve with `500`. The connection is restored automatically after a disconnection.
- Android: crash when `productIdentifier` was missing (`reject` without `return`).
- Android: the result of the acknowledge and of `launchBillingFlow` is now checked.
- Android: one result object per purchase in `getCurrentEntitlements`, and the purchase state is compared by value.
- Android: `manageSubscriptions` resolves, encodes the URL parameters, and handles a missing Play Store.
- `setGoogleVerificationDetails` resolves immediately on every platform. In 7.x it never resolved on Android and was rejected on iOS.
- `refundLatestTransaction` on Android resolves with `-1` "Not supported on Android" instead of being rejected.
- Logs no longer contain purchase tokens, order JSON or account ids. On iOS logging is in debug builds only.
- iOS: transaction listeners rewritten without deprecated APIs; they are cancelled when the plugin is released.

### Deprecated

- `setGoogleVerificationDetails`: it does nothing. It will be removed in a future major version.

### Removed

- iOS: the `getTrialStatus` and `startTrial` registrations. They had no implementation and were never part of the JS API.
- Android: the server-side verification code behind `setGoogleVerificationDetails`, and the unused `androidx.core:core-ktx` dependency.
- iOS: `SubscriptionsPlugin.m` and `SubscriptionsPlugin.h`. Methods are registered with `CAPBridgedPlugin`.
