import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    kotlin("native.cocoapods")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared module for OliveTrustCharity"
        homepage = "https://github.com/olivetrust"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "Shared"
            isStatic = true
        }
        pod("FirebaseCore") { moduleName = "FirebaseCore" }
        pod("FirebaseFirestore") { moduleName = "FirebaseFirestore" }
        pod("FirebaseAuth") { moduleName = "FirebaseAuth" }
        pod("FirebaseStorage") { moduleName = "FirebaseStorage" }
        pod("FirebaseFunctions") { moduleName = "FirebaseFunctions" }
        pod("FirebaseMessaging") { moduleName = "FirebaseMessaging" }
    }
    
    androidLibrary {
       namespace = "com.olivetrust.charity.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            
            // Firebase Android (Transitive for GitLive)
            implementation("com.google.firebase:firebase-common-ktx:21.0.0")
            implementation("com.google.firebase:firebase-firestore-ktx:25.1.2")
            implementation("com.google.firebase:firebase-auth-ktx:23.2.0")
            implementation("com.google.firebase:firebase-storage-ktx:21.0.1")
            implementation("com.google.firebase:firebase-functions-ktx:21.0.0")
            implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")
            implementation("com.google.android.gms:play-services-location:21.3.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Icons
            implementation(libs.compose.materialIconsCore)
            // implementation(libs.compose.materialIconsExtended) // Skipping due to sync error

            // Firebase
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.messaging)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)

            // Multiplatform Settings
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-coroutines:1.3.0")

            // Crypto
            implementation(libs.kotlinCrypto.hash.sha2)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
