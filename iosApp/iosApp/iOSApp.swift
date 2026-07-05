import SwiftUI
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import shared

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        print("APP_DELEGATE: didFinishLaunchingWithOptions called")
        
        let isDebug = _isDebugAssertConfiguration()
        print("APP_DELEGATE: Mode: \(isDebug ? "DEBUG (UAT)" : "RELEASE (PROD)")")

        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: path) {
            print("APP_DELEGATE: Configuring Firebase with \(path)")
            FirebaseApp.configure(options: options)
        } else {
            print("APP_DELEGATE: Configuring Firebase with default options")
            FirebaseApp.configure()
        }

        // Setup push notifications for both Debug (UAT/sandbox) and Release (Production)
        // APNs keys are configured for both sandbox and production environments
        let modeStr = isDebug ? "DEBUG (UAT/Sandbox APNs)" : "RELEASE (Production APNs)"
        print("APP_DELEGATE: Setting up push notifications in \(modeStr) mode...")
        UNUserNotificationCenter.current().delegate = self
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { granted, error in
            print("APP_DELEGATE: Authorization granted: \(granted), error: \(error?.localizedDescription ?? "none")")
        }
        
        application.registerForRemoteNotifications()
        Messaging.messaging().delegate = self
        
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        print("APNS_TOKEN_RECEIVED: \(deviceToken.map { String(format: "%02.2hhx", $0) }.joined())")
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("APNS_REGISTRATION_FAILED: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("FCM_TOKEN: \(fcmToken ?? "nil")")
        // Push token into Kotlin bridge so IosNotificationHelper.awaitFcmToken() resolves immediately.
        // This fixes the "no APNs token specified before fetching FCM token" race condition.
        IosNotificationHelper.shared.onFcmTokenReceived(token: fcmToken)
        NotificationCenter.default.post(name: Notification.Name("FCMTokenUpdated"), object: nil, userInfo: ["token": fcmToken ?? ""])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([[.banner, .list, .sound]])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            let isDebug = _isDebugAssertConfiguration()
            let environment: shared.Environment = isDebug ? .uat : .production
            let config = AppConfig(environment: environment, isDebug: isDebug)
            ContentView(config: config)
        }
    }
}