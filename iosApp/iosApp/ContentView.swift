import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    let config: AppConfig
    
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(config: config)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    let config: AppConfig
    
    var body: some View {
        ComposeView(config: config)
            .ignoresSafeArea()
    }
}