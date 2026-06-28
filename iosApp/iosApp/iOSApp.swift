import SwiftUI
import FirebaseCore
import shared

@main
struct iOSApp: App {
    init() {
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: path) {
            FirebaseApp.configure(options: options)
        } else {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            let isDebug = _isDebugAssertConfiguration()
            // In a real app, you might use a flag from Info.plist to distinguish UAT vs PROD
            let environment: shared.Environment = isDebug ? .uat : .production
            let config = AppConfig(environment: environment, isDebug: isDebug)
            ContentView(config: config)
        }
    }
}