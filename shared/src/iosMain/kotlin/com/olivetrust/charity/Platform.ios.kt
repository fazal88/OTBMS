package com.olivetrust.charity

import platform.UIKit.*
import platform.CoreLocation.*
import platform.Foundation.*
import platform.AVFAudio.*
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.UniformTypeIdentifiers.*
import platform.darwin.*
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.cinterop.*
import platform.posix.memcpy

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

class IOSDeviceInfo : DeviceInfo {
    override val id: String = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
    override val model: String = UIDevice.currentDevice.model
}

actual fun getDeviceInfo(): DeviceInfo = IOSDeviceInfo()

class IOSLocationService : LocationService {
    private var currentDelegate: CLLocationManagerDelegateProtocol? = null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val locationManager = CLLocationManager()
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                if (location != null) {
                    manager.stopUpdatingLocation()
                    if (continuation.isActive) {
                        val coordinate = location.coordinate
                        continuation.resume(Location(coordinate.useContents { latitude }, coordinate.useContents { longitude }))
                    }
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                when (didChangeAuthorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> {
                        manager.startUpdatingLocation()
                    }
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted -> {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    else -> {}
                }
            }
        }

        currentDelegate = delegate
        locationManager.delegate = delegate

        val status = CLLocationManager.authorizationStatus()
        when (status) {
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            else -> {
                if (continuation.isActive) continuation.resume(null)
            }
        }

        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
            locationManager.delegate = null
            currentDelegate = null
        }
    }
}

actual fun getLocationService(): LocationService = IOSLocationService()

@OptIn(ExperimentalNativeApi::class)
actual val isDebug: Boolean = kotlin.native.Platform.isDebugBinary

actual fun sendSms(phoneNumber: String, message: String) {
    val finalMessage = if (isDebug) "[TEST] $message" else message
    println("IOS_SMS: To $phoneNumber: $finalMessage")
}

actual fun openMaps(latitude: Double, longitude: Double, label: String) {
    val urlString = if (label.isNotBlank()) {
        "https://maps.apple.com/?q=${label.replace(" ", "+")}&ll=$latitude,$longitude"
    } else {
        "https://maps.apple.com/?ll=$latitude,$longitude"
    }
    val url = NSURL.URLWithString(urlString)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareUrl(url: String, title: String) {
    val activityItems = listOf(url)
    val activityViewController = UIActivityViewController(activityItems, null)
    
    val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = window?.rootViewController
    
    rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
}

actual suspend fun getPlatformFcmToken(): String? {
    return IosNotificationHelper.awaitFcmToken(timeoutSeconds = 15)
}

actual fun setScreenshotProtection(enabled: Boolean) {
    println("IOS_SCREENSHOT_PROTECTION: ${if (enabled) "Enabled" else "Disabled"} (Not fully implemented on iOS)")
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toKotlinByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length)
        }
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
class IOSFilePicker : FilePicker {
    private var currentDelegate: NSObject? = null

    private fun getRootViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        return window?.rootViewController
    }

    override suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val root = getRootViewController()
        if (root == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.mediaTypes = listOf("public.image")

        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                val data = image?.let { UIImageJPEGRepresentation(it, 0.8) }
                continuation.resume(data?.toKotlinByteArray())
                picker.dismissViewControllerAnimated(true, null)
                currentDelegate = null
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                continuation.resume(null)
                currentDelegate = null
            }
        }
        currentDelegate = delegate
        picker.delegate = delegate
        root.presentViewController(picker, animated = true, completion = null)
    }

    override suspend fun pickImageOrPdf(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val root = getRootViewController()
        if (root == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeImage, UTTypePDF),
            asCopy = true
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val data = url?.let { NSData.dataWithContentsOfURL(it) }
                continuation.resume(data?.toKotlinByteArray())
                currentDelegate = null
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                continuation.resume(null)
                currentDelegate = null
            }
        }
        currentDelegate = delegate
        picker.delegate = delegate
        root.presentViewController(picker, animated = true, completion = null)
    }

    override suspend fun takePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val root = getRootViewController()
        if (root == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        
        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                val data = image?.let { UIImageJPEGRepresentation(it, 0.8) }
                continuation.resume(data?.toKotlinByteArray())
                picker.dismissViewControllerAnimated(true, null)
                currentDelegate = null
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                continuation.resume(null)
                currentDelegate = null
            }
        }
        currentDelegate = delegate
        picker.delegate = delegate
        root.presentViewController(picker, animated = true, completion = null)
    }
}

actual fun getFilePicker(): FilePicker = IOSFilePicker()

@OptIn(ExperimentalForeignApi::class)
class IOSAudioRecorder : AudioRecorder {
    private var recorder: AVAudioRecorder? = null
    private var audioFileURL: NSURL? = null

    override fun startRecording() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            session.setActive(true, error = null)

            val tempDir = NSTemporaryDirectory()
            val path = tempDir + "temp_recording.m4a"
            audioFileURL = NSURL.fileURLWithPath(path)

            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC.toInt(),
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1,
                AVEncoderAudioQualityKey to AVAudioQualityHigh
            )

            recorder = AVAudioRecorder(audioFileURL!!, settings, null)
            recorder?.prepareToRecord()
            recorder?.record()
        } catch (e: Exception) {
            println("IOS_AUDIO_RECORDER_ERROR: ${e.message}")
        }
    }

    override fun stopRecording(): ByteArray? {
        recorder?.stop()
        val data = audioFileURL?.let { NSData.dataWithContentsOfURL(it) }
        val bytes = data?.toKotlinByteArray()
        
        audioFileURL?.path?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        recorder = null
        audioFileURL = null
        
        return bytes
    }

    override fun isRecording(): Boolean = recorder?.recording ?: false
}

actual fun getAudioRecorder(): AudioRecorder = IOSAudioRecorder()

@OptIn(ExperimentalForeignApi::class)
class IOSAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private var onComplete: (() -> Unit)? = null
    private var onPrepared: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var currentDelegate: NSObject? = null

    override fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: run {
            onError?.invoke("Invalid URL")
            return
        }
        
        // Download data asynchronously to avoid blocking the UI thread
        val task = NSURLSession.sharedSession.dataTaskWithURL(nsUrl) { data, _, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (error != null) {
                    onError?.invoke(error.localizedDescription)
                    return@dispatch_async
                }
                if (data != null) {
                    playInternal(data)
                } else {
                    onError?.invoke("No data received")
                }
            }
        }
        task.resume()
    }

    override fun play(data: ByteArray) {
        playInternal(data.toNSData())
    }

    private fun playInternal(nsData: NSData) {
        stop()
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null)
            session.setActive(true, error = null)

            player = AVAudioPlayer(nsData, null).apply {
                val delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
                    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
                        dispatch_async(dispatch_get_main_queue()) {
                            onComplete?.invoke()
                        }
                    }

                    override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
                        dispatch_async(dispatch_get_main_queue()) {
                            onError?.invoke(error?.localizedDescription ?: "Decode error")
                        }
                    }
                }
                currentDelegate = delegate
                this.delegate = delegate
                play()
                onPrepared?.invoke()
            }
        } catch (e: Exception) {
            println("IOS_AUDIO_PLAYER_ERROR: ${e.message}")
            onError?.invoke(e.message ?: "Unknown error")
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun stop() {
        player?.stop()
        player = null
        currentDelegate = null
    }

    override fun isPlaying(): Boolean = player?.playing ?: false

    override fun setCompletionListener(callback: () -> Unit) {
        onComplete = callback
    }

    override fun setOnPreparedListener(callback: () -> Unit) {
        onPrepared = callback
    }

    override fun setOnErrorListener(callback: (String) -> Unit) {
        onError = callback
    }
}

actual fun getAudioPlayer(): AudioPlayer = IOSAudioPlayer()
