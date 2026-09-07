import Foundation
import Combine
import SwiftUI

// MARK: - AppStore
// Central observable store — equivalent to Android ViewModels + Room DAOs combined.
// Uses in-memory arrays backed by UserDefaults JSON for persistence in v1.
// Replace with Core Data or Firebase sync for production.

@MainActor
final class AppStore: ObservableObject {
    static let shared = AppStore()

    // ── Family Members ────────────────────────────────────────────────────────
    @Published var members: [FamilyMember] = []
    @Published var currentUser: FamilyMember = FamilyMember(
        id: "me",
        displayName: UserDefaults.standard.string(forKey: "displayName") ?? "You",
        role: "ADMIN"
    )

    // ── Chat ──────────────────────────────────────────────────────────────────
    @Published var allMessages: [ChatMessage] = []
    @Published var unreadCount: Int = 0

    // ── Notes ─────────────────────────────────────────────────────────────────
    @Published var notes: [NoteItem] = []

    // ── Calendar ──────────────────────────────────────────────────────────────
    @Published var calendarEvents: [CalendarEvent] = []

    // ── Tasks ─────────────────────────────────────────────────────────────────
    @Published var tasks: [TaskItem] = []

    // ── Board ─────────────────────────────────────────────────────────────────
    @Published var boardPosts: [BoardPost] = []

    // ── Shopping ──────────────────────────────────────────────────────────────
    @Published var shoppingItems: [ShoppingItem] = []

    // ── Chores ────────────────────────────────────────────────────────────────
    @Published var chores: [Chore] = []

    // ── Polls ─────────────────────────────────────────────────────────────────
    @Published var polls: [FamilyPoll] = []

    // ── Bedtime ───────────────────────────────────────────────────────────────
    @Published var bedtimeAlerts: [BedtimeAlert] = []

    // ── Albums + Photos ───────────────────────────────────────────────────────
    @Published var albums: [PhotoAlbum] = []
    @Published var photos: [FamilyPhoto] = []

    // ── Health ────────────────────────────────────────────────────────────────
    @Published var healthRecords: [HealthRecord] = []

    // ── Safe Zones ────────────────────────────────────────────────────────────
    @Published var safeZones: [SafeZone] = []

    // ── Settings ──────────────────────────────────────────────────────────────
    @Published var settings = AppSettings()

    // ── Weather ───────────────────────────────────────────────────────────────
    @Published var weather: WeatherData? = nil
    @Published var weatherError: String? = nil
    @Published var weatherLoading: Bool = false

    // ── Onboarding ────────────────────────────────────────────────────────────
    @Published var hasCompletedOnboarding: Bool = UserDefaults.standard.bool(forKey: "onboardingDone")
    @Published var isAuthenticated: Bool = UserDefaults.standard.bool(forKey: "isAuthenticated")

    private init() {
        loadPersistedData()
        seedDemoData()
    }

    // MARK: - Persistence helpers
    private func loadPersistedData() {
        if let data = UserDefaults.standard.data(forKey: "notes"),
           let decoded = try? JSONDecoder().decode([NoteItem].self, from: data) {
            notes = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "tasks"),
           let decoded = try? JSONDecoder().decode([TaskItem].self, from: data) {
            tasks = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "calendarEvents"),
           let decoded = try? JSONDecoder().decode([CalendarEvent].self, from: data) {
            calendarEvents = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "boardPosts"),
           let decoded = try? JSONDecoder().decode([BoardPost].self, from: data) {
            boardPosts = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "shoppingItems"),
           let decoded = try? JSONDecoder().decode([ShoppingItem].self, from: data) {
            shoppingItems = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "chores"),
           let decoded = try? JSONDecoder().decode([Chore].self, from: data) {
            chores = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "polls"),
           let decoded = try? JSONDecoder().decode([FamilyPoll].self, from: data) {
            polls = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "members"),
           let decoded = try? JSONDecoder().decode([FamilyMember].self, from: data) {
            members = decoded
        }
        if let data = UserDefaults.standard.data(forKey: "bedtimeAlerts"),
           let decoded = try? JSONDecoder().decode([BedtimeAlert].self, from: data) {
            bedtimeAlerts = decoded
        }
    }

    func persist() {
        try? UserDefaults.standard.set(JSONEncoder().encode(notes), forKey: "notes")
        try? UserDefaults.standard.set(JSONEncoder().encode(tasks), forKey: "tasks")
        try? UserDefaults.standard.set(JSONEncoder().encode(calendarEvents), forKey: "calendarEvents")
        try? UserDefaults.standard.set(JSONEncoder().encode(boardPosts), forKey: "boardPosts")
        try? UserDefaults.standard.set(JSONEncoder().encode(shoppingItems), forKey: "shoppingItems")
        try? UserDefaults.standard.set(JSONEncoder().encode(chores), forKey: "chores")
        try? UserDefaults.standard.set(JSONEncoder().encode(polls), forKey: "polls")
        try? UserDefaults.standard.set(JSONEncoder().encode(members), forKey: "members")
        try? UserDefaults.standard.set(JSONEncoder().encode(bedtimeAlerts), forKey: "bedtimeAlerts")
    }

    // MARK: - Demo seed
    private func seedDemoData() {
        guard members.isEmpty else { return }
        members = [
            FamilyMember(id: "dad", displayName: "Dad", role: "ADMIN", isOnline: true, statusMessage: "Home 🏠"),
            FamilyMember(id: "mom", displayName: "Mom", role: "ADMIN", isOnline: true, statusMessage: "Working 💼"),
            FamilyMember(id: "kid1", displayName: "Alex", role: "CHILD", isOnline: false, statusMessage: "School 🎒"),
            FamilyMember(id: "kid2", displayName: "Sam", role: "CHILD", isOnline: false, statusMessage: ""),
        ]
        persist()
    }

    // MARK: - Notes CRUD
    func addNote(_ note: NoteItem) {
        var n = note; n.id = Int64(Date().timeIntervalSince1970 * 1000)
        notes.insert(n, at: 0); persist()
    }
    func updateNote(_ note: NoteItem) {
        if let idx = notes.firstIndex(where: { $0.id == note.id }) { notes[idx] = note; persist() }
    }
    func deleteNote(_ note: NoteItem) {
        notes.removeAll { $0.id == note.id }; persist()
    }
    func togglePin(_ note: NoteItem) {
        if let idx = notes.firstIndex(where: { $0.id == note.id }) {
            notes[idx].isPinned.toggle(); persist()
        }
    }

    // MARK: - Tasks CRUD
    func addTask(_ task: TaskItem) {
        var t = task; t.id = Int64(Date().timeIntervalSince1970 * 1000)
        tasks.insert(t, at: 0); persist()
    }
    func toggleTask(_ task: TaskItem) {
        if let idx = tasks.firstIndex(where: { $0.id == task.id }) {
            tasks[idx].isCompleted.toggle()
            tasks[idx].completedAt = tasks[idx].isCompleted ? Date() : nil
            persist()
        }
    }
    func deleteTask(_ task: TaskItem) { tasks.removeAll { $0.id == task.id }; persist() }

    // MARK: - Calendar CRUD
    func addCalendarEvent(_ event: CalendarEvent) {
        var e = event; e.id = Int64(Date().timeIntervalSince1970 * 1000)
        calendarEvents.append(e); persist()
    }
    func deleteCalendarEvent(_ event: CalendarEvent) {
        calendarEvents.removeAll { $0.id == event.id }; persist()
    }

    // MARK: - Board CRUD
    func addBoardPost(_ post: BoardPost) {
        var p = post; p.id = Int64(Date().timeIntervalSince1970 * 1000)
        boardPosts.insert(p, at: 0); persist()
    }
    func reactToPost(postId: Int64, emoji: String) {
        if let idx = boardPosts.firstIndex(where: { $0.id == postId }) {
            boardPosts[idx].reactions[emoji, default: 0] += 1; persist()
        }
    }
    func deleteBoardPost(_ post: BoardPost) { boardPosts.removeAll { $0.id == post.id }; persist() }

    // MARK: - Chat
    func messages(for memberId: String) -> [ChatMessage] {
        allMessages.filter { ($0.senderId == memberId && !$0.isSentByMe) || ($0.receiverId == memberId && $0.isSentByMe) }
            .sorted { $0.timestamp < $1.timestamp }
    }
    func sendMessage(to memberId: String, content: String, type: String = "TEXT") {
        let msg = ChatMessage(senderId: currentUser.id, receiverId: memberId, content: content, messageType: type, isSentByMe: true)
        allMessages.append(msg)
        unreadCount = allMessages.filter { !$0.isRead && !$0.isSentByMe }.count
    }

    // MARK: - Shopping CRUD
    func addShoppingItem(_ item: ShoppingItem) {
        var i = item; i.id = Int64(Date().timeIntervalSince1970 * 1000)
        shoppingItems.append(i); persist()
    }
    func toggleShoppingItem(_ item: ShoppingItem) {
        if let idx = shoppingItems.firstIndex(where: { $0.id == item.id }) {
            shoppingItems[idx].isChecked.toggle(); persist()
        }
    }
    func deleteShoppingItem(_ item: ShoppingItem) { shoppingItems.removeAll { $0.id == item.id }; persist() }

    // MARK: - Chores CRUD
    func addChore(_ chore: Chore) {
        var c = chore; c.id = Int64(Date().timeIntervalSince1970 * 1000)
        chores.append(c); persist()
    }
    func toggleChore(_ chore: Chore) {
        if let idx = chores.firstIndex(where: { $0.id == chore.id }) {
            chores[idx].isCompleted.toggle(); persist()
        }
    }
    func deleteChore(_ chore: Chore) { chores.removeAll { $0.id == chore.id }; persist() }

    // MARK: - Polls
    func addPoll(_ poll: FamilyPoll) {
        var p = poll; p.id = Int64(Date().timeIntervalSince1970 * 1000)
        polls.insert(p, at: 0); persist()
    }
    func vote(pollId: Int64, option: String, memberId: String) {
        if let idx = polls.firstIndex(where: { $0.id == pollId }) {
            // Remove previous vote
            for key in polls[idx].votes.keys {
                polls[idx].votes[key]?.removeAll { $0 == memberId }
            }
            polls[idx].votes[option, default: []].append(memberId)
            persist()
        }
    }

    // MARK: - Auth
    func completeOnboarding(name: String) {
        currentUser.displayName = name
        UserDefaults.standard.set(name, forKey: "displayName")
        hasCompletedOnboarding = true
        isAuthenticated = true
        UserDefaults.standard.set(true, forKey: "onboardingDone")
        UserDefaults.standard.set(true, forKey: "isAuthenticated")
    }

    func signOut() {
        isAuthenticated = false
        hasCompletedOnboarding = false
        UserDefaults.standard.set(false, forKey: "isAuthenticated")
        UserDefaults.standard.set(false, forKey: "onboardingDone")
    }
}

// MARK: - AppSettings
struct AppSettings: Codable {
    var temperatureUnit: String = "F"      // "C" | "F"
    var darkMode: String = "dark"
    var fontSize: String = "medium"
    var notificationsEnabled: Bool = true
    var locationSharingEnabled: Bool = true
    var biometricEnabled: Bool = false
    var driveBackupEnabled: Bool = true
    var sosAutoCallEnabled: Bool = false
    var voiceActivationEnabled: Bool = false
    var walkieChannel: Int = 1
    var appPinEnabled: Bool = false
    var appPin: String = ""
    var chatRetentionDays: Int = 30
    var locationIntervalSec: Int = 60
}

// MARK: - Codable conformances
extension NoteItem: Codable {}
extension TaskItem: Codable {}
extension CalendarEvent: Codable {}
extension BoardPost: Codable {}
extension ShoppingItem: Codable {}
extension Chore: Codable {}
extension FamilyPoll: Codable {}
extension FamilyMember: Codable {}
extension BedtimeAlert: Codable {}
extension HealthRecord: Codable {}
extension SafeZone: Codable {}
extension PhotoAlbum: Codable {}
extension FamilyPhoto: Codable {}
