import Foundation
import Speech
import AVFoundation

// MARK: - Voice Activation Manager
// Mirrors Android's VoiceCommandService — listens for "Family Four" wake phrase,
// then responds "I'm Ready" and accepts follow-up navigation commands.

class VoiceActivationManager: NSObject, ObservableObject, SFSpeechRecognizerDelegate {
    static let shared = VoiceActivationManager()

    @Published var isListening = false
    @Published var statusText = "Say \"Family Four\" to activate"

    private let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var audioEngine = AVAudioEngine()
    private var tts = AVSpeechSynthesizer()
    private var isCommandMode = false
    private var commandTimer: Timer?

    private let wakePhrases = ["family four", "family 4", "family for"]

    override init() {
        super.init()
        speechRecognizer?.delegate = self
    }

    func start() {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            guard status == .authorized else { return }
            AVAudioApplication.requestRecordPermission { granted in
                guard granted else { return }
                DispatchQueue.main.async { self?.startListening() }
            }
        }
    }

    func stop() {
        stopListening()
        isListening = false
    }

    private func startListening() {
        do {
            try startAudioSession()
            try beginRecognition()
            DispatchQueue.main.async { self.isListening = true }
        } catch {
            print("Voice activation error: \(error)")
            // Retry after 2 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
                self?.startListening()
            }
        }
    }

    private func stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionRequest = nil
        recognitionTask = nil
    }

    private func startAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.record, mode: .measurement, options: .duckOthers)
        try session.setActive(true, options: .notifyOthersOnDeactivation)
    }

    private func beginRecognition() throws {
        stopListening()
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let req = recognitionRequest else { return }
        req.shouldReportPartialResults = true
        req.requiresOnDeviceRecognition = true

        recognitionTask = speechRecognizer?.recognitionTask(with: req) { [weak self] result, error in
            guard let self = self else { return }
            if let result = result {
                let text = result.bestTranscription.formattedString.lowercased()
                self.processText(text)
            }
            if error != nil || (result?.isFinal ?? false) {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    self.startListening()
                }
            }
        }

        let inputNode = audioEngine.inputNode
        let format = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }
        audioEngine.prepare()
        try audioEngine.start()
    }

    private func processText(_ text: String) {
        if !isCommandMode {
            if wakePhrases.contains(where: { text.contains($0) }) {
                onWakeDetected()
            }
        } else {
            processCommand(text)
        }
    }

    private func onWakeDetected() {
        isCommandMode = true
        DispatchQueue.main.async {
            self.statusText = "Listening for command…"
        }
        speak("I'm Ready") {
            self.commandTimer = Timer.scheduledTimer(withTimeInterval: 8, repeats: false) { [weak self] _ in
                self?.isCommandMode = false
                DispatchQueue.main.async { self?.statusText = "Say \"Family Four\" to activate" }
            }
        }
    }

    private func processCommand(_ text: String) {
        commandTimer?.invalidate()
        isCommandMode = false
        DispatchQueue.main.async { self.statusText = "Say \"Family Four\" to activate" }

        var navTarget: String? = nil
        if text.contains("calendar")  { navTarget = "calendar" }
        else if text.contains("chat") { navTarget = "chat" }
        else if text.contains("notes"){ navTarget = "notes" }
        else if text.contains("tasks"){ navTarget = "tasks" }
        else if text.contains("map")  { navTarget = "map" }
        else if text.contains("weather") { navTarget = "weather" }
        else if text.contains("health")  { navTarget = "health" }
        else if text.contains("album")   { navTarget = "albums" }
        else if text.contains("board")   { navTarget = "board" }
        else if text.contains("home") || text.contains("dashboard") { navTarget = "dashboard" }

        if let target = navTarget {
            speak("Opening \(target)")
            NotificationCenter.default.post(name: .voiceNavigation, object: target)
        }
    }

    private func speak(_ text: String, completion: (() -> Void)? = nil) {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        utterance.rate = 0.5
        tts.speak(utterance)
        // Approximate duration delay for completion
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { completion?() }
    }
}

extension Notification.Name {
    static let voiceNavigation = Notification.Name("voiceNavigation")
}
