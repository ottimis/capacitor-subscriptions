import Foundation
import StoreKit
import Capacitor
import UIKit

/// Logs only in debug builds. Never pass transaction identifiers, tokens or JWS here.
private func debugLog(_ message: @autoclosure () -> String) {
    #if DEBUG
    print("[Subscriptions] " + message())
    #endif
}

@objc public class Subscriptions: NSObject {

    private var updatesListener: Task<Void, Never>?
    private var unfinishedListener: Task<Void, Never>?

    override init() {
        super.init()
        updatesListener = listenForTransactions()
        unfinishedListener = finishTransactions()
    }

    deinit {
        updatesListener?.cancel()
        unfinishedListener?.cancel()
    }

    // When the subscription renews at the end of the month, a transaction will
    // be queued for when the app is next opened. This listener handles any transactions
    // within the queue and finishes verified purchases to clear the queue and prevent
    // any bugs or performance issues occuring
    private func listenForTransactions() -> Task<Void, Never> {
        return Task.detached {
            // Iterate through any transactions that don't come from a direct call to `purchase()`.
            for await verification in Transaction.updates {
                guard let transaction = Subscriptions.checkVerified(verification) else {
                    // Skip this one only: the listener must keep running for the next transactions.
                    debugLog("checkVerified failed - Transaction.updates")
                    continue
                }

                await transaction.finish()
                debugLog("Transaction finished and removed from paymentQueue - Transaction.updates")
            }
        }
    }

    private func finishTransactions() -> Task<Void, Never> {
        return Task.detached {
            // Iterate through any transactions that were not finished yet.
            for await verification in Transaction.unfinished {
                guard let transaction = Subscriptions.checkVerified(verification) else {
                    debugLog("checkVerified failed - Transaction.unfinished")
                    continue
                }

                await transaction.finish()
                debugLog("Transaction finished and removed from paymentQueue - Transaction.unfinished")
            }
        }
    }

    @objc public func getProductDetails(_ productIdentifier: String) async -> PluginCallResultData {

        guard let product = await getProduct(productIdentifier) else {
            return [
                "responseCode": 1,
                "responseMessage": "Could not find a product matching the given productIdentifier"
            ]
        }

        return [
            "responseCode": 0,
            "responseMessage": "Successfully found the product details for given productIdentifier",
            "data": [
                "displayName": product.displayName,
                "description": product.description,
                "price": product.displayPrice
            ]
        ]
    }

    @objc public func purchaseProduct(_ productIdentifier: String, _ accountId: String?) async -> PluginCallResultData {

        do {
            guard let product = await getProduct(productIdentifier) else {
                return [
                    "responseCode": 1,
                    "responseMessage": "Could not find a product matching the given productIdentifier"
                ]
            }

            // Add appAccountToken to purchase
            var purchaseOptions = Set<Product.PurchaseOption>()
            if let accountId = accountId {
                guard let accountUUID = UUID(uuidString: accountId) else {
                    return [
                        "responseCode": 1,
                        "responseMessage": "Invalid accountId"
                    ]
                }
                purchaseOptions.insert(Product.PurchaseOption.appAccountToken(accountUUID))
            }

            let result: Product.PurchaseResult = try await product.purchase(options: purchaseOptions)

            switch result {

            case .success(let verification):

                guard let transaction = Subscriptions.checkVerified(verification) else {
                    return [
                        "responseCode": 2,
                        "responseMessage": "Product seems to have been purchased but the transaction failed verification"
                    ]
                }

                await transaction.finish()
                return [
                    "responseCode": 0,
                    "responseMessage": "Successfully purchased product",
                    "data": [
                        "productId": transaction.productID,
                        "jws": verification.jwsRepresentation
                    ]
                ]

            case .userCancelled:

                return [
                    "responseCode": 3,
                    "responseMessage": "User closed the native popover before purchasing"
                ]

            case .pending:

                return [
                    "responseCode": 4,
                    "responseMessage": "Product request made but is currently pending - likely due to parental restrictions"
                ]

            @unknown default:

                return [
                    "responseCode": 5,
                    "responseMessage": "An unknown error occurred whilst in the purchasing process"
                ]
            }

        } catch {
            debugLog("purchaseProduct: " + error.localizedDescription)
            return [
                "responseCode": 5,
                "responseMessage": "An unknown error occurred whilst in the purchasing process"
            ]
        }
    }

    @objc public func getCurrentEntitlements(sync: Bool) async -> PluginCallResultData {

        do {
            if sync {
                try await AppStore.sync()
            }

            var transactions: [Any] = []

            // Loop through each verification result in currentEntitlements, verify the transaction
            // then add it to the list if verified.
            for await verification in Transaction.currentEntitlements {
                if let transaction = Subscriptions.checkVerified(verification) {
                    transactions.append(
                        [
                            "productId": transaction.productID,
                            "jws": verification.jwsRepresentation
                        ]
                    )
                }
            }

            if transactions.isEmpty {
                return [
                    "responseCode": 1,
                    "responseMessage": "No entitlements were found"
                ]
            }

            return [
                "responseCode": 0,
                "responseMessage": "Successfully found all entitlements across all product types",
                "data": transactions
            ]

        } catch {
            debugLog("getCurrentEntitlements: " + error.localizedDescription)
            return [
                "responseCode": 2,
                "responseMessage": "Unknown problem trying to retrieve entitlements"
            ]
        }
    }

    @objc public func getLatestTransaction(_ productIdentifier: String) async -> PluginCallResultData {

        guard let product = await getProduct(productIdentifier) else {
            return [
                "responseCode": 1,
                "responseMessage": "Could not find a product matching the given productIdentifier"
            ]
        }

        guard let verification = await product.latestTransaction,
              let transaction = Subscriptions.checkVerified(verification) else {
            // The user hasn't purchased this product.
            return [
                "responseCode": 2,
                "responseMessage": "No transaction for given productIdentifier, or it could not be verified"
            ]
        }

        debugLog("Latest transaction found for \(transaction.productID)")

        return [
            "responseCode": 0,
            "responseMessage": "Latest transaction found",
            "data": [
                "productId": transaction.productID,
                "jws": verification.jwsRepresentation
            ]
        ]
    }

    @objc public func refundLatestTransaction(_ productIdentifier: String) async -> PluginCallResultData {

        do {
            guard let product = await getProduct(productIdentifier) else {
                return [
                    "responseCode": 1,
                    "responseMessage": "Could not find a product matching the given productIdentifier"
                ]
            }

            guard let verification = await product.latestTransaction,
                  let transaction = Subscriptions.checkVerified(verification) else {
                // The user hasn't purchased this product.
                return [
                    "responseCode": 2,
                    "responseMessage": "No transaction for given productIdentifier, or it could not be verified"
                ]
            }

            let windowScene = await MainActor.run {
                UIApplication.shared.connectedScenes.first as? UIWindowScene
            }
            guard let scene = windowScene else {
                return [
                    "responseCode": 4,
                    "responseMessage": "Problem getting UIScene"
                ]
            }

            let status = try await transaction.beginRefundRequest(in: scene)
            switch status {
            case .success:
                return [
                    "responseCode": 0,
                    "responseMessage": "Refund request submitted"
                ]
            case .userCancelled:
                return [
                    "responseCode": 0,
                    "responseMessage": "User closed the refund request sheet"
                ]
            @unknown default:
                return [
                    "responseCode": 0,
                    "responseMessage": "Refund request sheet closed"
                ]
            }
        } catch {
            debugLog("refundLatestTransaction: " + error.localizedDescription)
            return [
                "responseCode": 3,
                "responseMessage": "Unknown problem trying to refund latest transaction"
            ]
        }
    }

    @objc public func manageSubscriptions() async {
        guard let url = URL(string: "https://apps.apple.com/account/subscriptions") else {
            return
        }
        _ = await UIApplication.shared.open(url)
    }

    private func getProduct(_ productIdentifier: String) async -> Product? {
        do {
            return try await Product.products(for: [productIdentifier]).first
        } catch {
            debugLog("getProduct: " + error.localizedDescription)
            return nil
        }
    }

    private static func checkVerified<T>(_ result: VerificationResult<T>) -> T? {
        switch result {
        case .verified(let safe):
            return safe
        case .unverified:
            return nil
        }
    }
}
