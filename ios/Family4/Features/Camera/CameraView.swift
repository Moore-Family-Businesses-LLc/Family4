import SwiftUI
import AVFoundation

struct CameraView: View {
    @StateObject private var vm = CameraViewModel()
    @State private var showGallery = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if vm.permissionGranted {
                // Camera preview
                CameraPreviewView(session: vm.session)
                    .ignoresSafeArea()

                VStack {
                    // Top controls
                    HStack {
                        Spacer()
                        VStack(spacing: 20) {
                            CameraControlButton(icon: vm.flashIcon) { vm.toggleFlash() }
                            CameraControlButton(icon: "camera.rotate.fill") { vm.flipCamera() }
                            CameraControlButton(icon: "moon.fill") { vm.toggleNightMode() }
                        }
                        .padding(.trailing, 16)
                    }
                    .padding(.top, 60)

                    Spacer()

                    // Bottom controls
                    HStack(alignment: .center, spacing: 40) {
                        // Last photo thumbnail
                        Button { showGallery = true } label: {
                            if let img = vm.lastCapturedImage {
                                Image(uiImage: img)
                                    .resizable().scaledToFill()
                                    .frame(width: 54, height: 54)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.white.opacity(0.5), lineWidth: 1))
                            } else {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color.white.opacity(0.1))
                                    .frame(width: 54, height: 54)
                            }
                        }

                        // Shutter
                        Button { vm.capturePhoto() } label: {
                            ZStack {
                                Circle().fill(Color.white).frame(width: 78, height: 78)
                                Circle().stroke(Color.white.opacity(0.4), lineWidth: 3).frame(width: 92, height: 92)
                            }
                        }
                        .scaleEffect(vm.isCapturing ? 0.92 : 1.0)
                        .animation(.spring(response: 0.2), value: vm.isCapturing)

                        // Video toggle
                        CameraControlButton(icon: vm.isVideoMode ? "camera.fill" : "video.fill") {
                            vm.toggleVideoMode()
                        }
                        .frame(width: 54, height: 54)
                    }
                    .padding(.bottom, 50)
                }
            } else {
                VStack(spacing: 20) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.textMuted)
                    Text("Camera access required")
                        .foregroundColor(.textPrimary)
                        .font(.system(size: 17, weight: .semibold))
                    Text("Please grant camera permission in Settings.")
                        .foregroundColor(.textMuted)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .foregroundColor(.accentCyan)
                }
            }
        }
        .onAppear { vm.start() }
        .onDisappear { vm.stop() }
    }
}

// MARK: - Camera Control Button
struct CameraControlButton: View {
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(Color.black.opacity(0.45))
                .clipShape(Circle())
        }
    }
}

// MARK: - Camera Preview (UIViewRepresentable)
struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewUIView {
        let view = PreviewUIView()
        view.session = session
        return view
    }

    func updateUIView(_ view: PreviewUIView, context: Context) {}

    class PreviewUIView: UIView {
        var session: AVCaptureSession? {
            didSet {
                guard let session = session else { return }
                previewLayer.session = session
            }
        }

        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

        override func layoutSubviews() {
            super.layoutSubviews()
            previewLayer.frame = bounds
            previewLayer.videoGravity = .resizeAspectFill
        }
    }
}

// MARK: - Camera ViewModel
@MainActor
class CameraViewModel: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {
    @Published var permissionGranted = false
    @Published var isCapturing = false
    @Published var isVideoMode = false
    @Published var lastCapturedImage: UIImage? = nil
    @Published var flashMode: AVCaptureDevice.FlashMode = .auto

    let session = AVCaptureSession()
    private var photoOutput = AVCapturePhotoOutput()
    private var currentPosition: AVCaptureDevice.Position = .back
    private var isNightMode = false

    var flashIcon: String {
        switch flashMode {
        case .on:  return "bolt.fill"
        case .off: return "bolt.slash.fill"
        default:   return "bolt.badge.automatic.fill"
        }
    }

    func start() {
        Task {
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            if status == .authorized {
                await setupSession()
                permissionGranted = true
            } else if status == .notDetermined {
                let granted = await AVCaptureDevice.requestAccess(for: .video)
                if granted {
                    await setupSession()
                    permissionGranted = true
                }
            }
        }
    }

    private func setupSession() async {
        session.beginConfiguration()
        session.sessionPreset = .photo

        // Add input
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: currentPosition),
              let input = try? AVCaptureDeviceInput(device: device) else {
            session.commitConfiguration(); return
        }
        if session.canAddInput(input) { session.addInput(input) }

        // Add photo output
        if session.canAddOutput(photoOutput) { session.addOutput(photoOutput) }
        session.commitConfiguration()

        Task.detached {
            self.session.startRunning()
        }
    }

    func stop() {
        Task.detached { self.session.stopRunning() }
    }

    func capturePhoto() {
        guard permissionGranted else { return }
        isCapturing = true
        let settings = AVCapturePhotoSettings()
        settings.flashMode = flashMode
        photoOutput.capturePhoto(with: settings, delegate: self)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { self.isCapturing = false }
    }

    nonisolated func photoOutput(_ output: AVCapturePhotoOutput,
                       didFinishProcessingPhoto photo: AVCapturePhoto,
                       error: Error?) {
        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
        Task { @MainActor in self.lastCapturedImage = image }
    }

    func flipCamera() {
        currentPosition = currentPosition == .back ? .front : .back
        session.beginConfiguration()
        session.inputs.forEach { session.removeInput($0) }
        if let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: currentPosition),
           let input = try? AVCaptureDeviceInput(device: device),
           session.canAddInput(input) {
            session.addInput(input)
        }
        session.commitConfiguration()
    }

    func toggleFlash() {
        switch flashMode {
        case .auto: flashMode = .on
        case .on:   flashMode = .off
        default:    flashMode = .auto
        }
    }

    func toggleNightMode() { isNightMode.toggle() }
    func toggleVideoMode() { isVideoMode.toggle() }
}
