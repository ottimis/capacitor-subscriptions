import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(SubscriptionsPlugin)
public class SubscriptionsPlugin: CAPPlugin {

    // Allows us to execute the actual code from Subscriptions.swift file
    private let implementation = Subscriptions()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""

        call.resolve([
            "value": value
        ])
    }

    @objc func getProductDetails(_ call: CAPPluginCall) {

        guard let productIdentifier = call.getString("productIdentifier") else {
            call.reject("Must provide a productID")
            return
        }

        Task {
            let response = await implementation.getProductDetails(productIdentifier)
            call.resolve(response)
        }
    }

    @objc func purchaseProduct(_ call: CAPPluginCall) {

        guard let productIdentifier = call.getString("productIdentifier") else {
            call.reject("Must provide a productID")
            return
        }
        let accountId = call.getString("accountId")

        Task {
            let response = await implementation.purchaseProduct(productIdentifier, accountId)
            call.resolve(response)
        }
    }

    @objc func getCurrentEntitlements(_ call: CAPPluginCall) {
        let sync = call.getBool("sync") ?? false

        Task {
            let response = await implementation.getCurrentEntitlements(sync: sync)
            call.resolve(response)
        }
    }

    @objc func getLatestTransaction(_ call: CAPPluginCall) {

        guard let productIdentifier = call.getString("productIdentifier") else {
            call.reject("Must provide a productID")
            return
        }

        Task {
            let response = await implementation.getLatestTransaction(productIdentifier)
            call.resolve(response)
        }
    }

    @objc func refundLatestTransaction(_ call: CAPPluginCall) {

        guard let productIdentifier = call.getString("productIdentifier") else {
            call.reject("Must provide a productID")
            return
        }

        Task {
            let response = await implementation.refundLatestTransaction(productIdentifier)
            call.resolve(response)
        }
    }

    @objc func manageSubscriptions(_ call: CAPPluginCall) {

        Task {
            await implementation.manageSubscriptions()
            call.resolve(["Success": "Opened"])
        }
    }

    /// Deprecated: Android-only and a no-op since 8.0.0. Resolves immediately.
    @objc func setGoogleVerificationDetails(_ call: CAPPluginCall) {
        call.resolve()
    }

    /// Android only: StoreKit has no acknowledge step.
    @objc func acknowledgePurchase(_ call: CAPPluginCall) {
        call.resolve([
            "responseCode": -1,
            "responseMessage": "Not supported on iOS"
        ])
    }

    /// Android only: on iOS consumables are consumed by finishing the transaction, which the plugin already does.
    @objc func consumePurchase(_ call: CAPPluginCall) {
        call.resolve([
            "responseCode": -1,
            "responseMessage": "Not supported on iOS"
        ])
    }
}
