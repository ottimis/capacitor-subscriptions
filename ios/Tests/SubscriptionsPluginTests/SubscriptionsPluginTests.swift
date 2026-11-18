import XCTest
import Capacitor
@testable import SubscriptionsPlugin

class SubscriptionsPluginTests: XCTestCase {

    // The methods registered through CAPBridgedPlugin must match the JS API and
    // must all exist on the class, otherwise the bridge cannot dispatch the call.
    func testRegisteredMethods() {
        let plugin = SubscriptionsPlugin()
        let expected = [
            "echo",
            "getProductDetails",
            "purchaseProduct",
            "getCurrentEntitlements",
            "getLatestTransaction",
            "refundLatestTransaction",
            "manageSubscriptions",
            "setGoogleVerificationDetails",
            "acknowledgePurchase",
            "consumePurchase"
        ]

        XCTAssertEqual(plugin.identifier, "SubscriptionsPlugin")
        XCTAssertEqual(plugin.jsName, "Subscriptions")
        XCTAssertEqual(plugin.pluginMethods.map { $0.name }, expected)

        for name in expected {
            XCTAssertTrue(
                plugin.responds(to: NSSelectorFromString("\(name):")),
                "Missing @objc method \(name)(_:)"
            )
        }
    }
}
