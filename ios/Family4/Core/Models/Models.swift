import Foundation
import CoreData

// MARK: - All Core Data Entity definitions as Swift structs + NSManagedObject extensions
// These mirror the Android Room entities exactly.

// ─────────────────────────────────────────────────────────────────────────────
// MARK: FamilyMember
// ─────────────────────────────────────────────────────────────────────────────
@objc(FamilyMemberMO)
public class FamilyMemberMO: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var displayName: String
    @NSManaged public var avatarUri: String?
    @NSManaged public var phoneNumber: String?
    @NSManaged public var role: String          // ADMIN | MEMBER | CHILD
    @NSManaged public var publicKey: String
    @NSManaged public var isOnline: Bool
    @NSManaged public var lastSeen: Double
    @NSManaged public var latitude: Double
    @NSManaged public var longitude: Double
    @NSManaged public var locationUpdatedAt: Double
    @NSManaged public var batteryLevel: Int32
    @NSManaged public var statusMessage: String
}

struct FamilyMember: Identifiable, Hashable {
    var id: String
    var displayName: String
    var avatarUri: String?
    var phoneNumber: String?
    var role: String = "MEMBER"
    var publicKey: String = ""
    var isOnline: Bool = false
    var lastSeen: Date = Date()
    var latitude: Double? = nil
    var longitude: Double? = nil
    var batteryLevel: Int = -1
    var statusMessage: String = ""

    var initials: String {
        displayName.split(separator: " ")
            .prefix(2)
            .compactMap { $0.first.map(String.init) }
            .joined()
            .uppercased()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: ChatMessage
// ─────────────────────────────────────────────────────────────────────────────
@objc(ChatMessageMO)
public class ChatMessageMO: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var senderId: String
    @NSManaged public var receiverId: String
    @NSManaged public var content: String        // plaintext after decrypt
    @NSManaged public var messageType: String
    @NSManaged public var mediaUri: String?
    @NSManaged public var timestamp: Double
    @NSManaged public var isRead: Bool
    @NSManaged public var isDelivered: Bool
    @NSManaged public var isSentByMe: Bool
}

struct ChatMessage: Identifiable, Hashable {
    var id: String = UUID().uuidString
    var senderId: String
    var receiverId: String
    var content: String
    var messageType: String = "TEXT"
    var mediaUri: String?
    var timestamp: Date = Date()
    var isRead: Bool = false
    var isDelivered: Bool = false
    var isSentByMe: Bool = false
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Note
// ─────────────────────────────────────────────────────────────────────────────
@objc(NoteMO)
public class NoteMO: NSManagedObject {
    @NSManaged public var id: Int64
    @NSManaged public var title: String
    @NSManaged public var content: String
    @NSManaged public var colorHex: String
    @NSManaged public var isPinned: Bool
    @NSManaged public var isArchived: Bool
    @NSManaged public var isChecklist: Bool
    @NSManaged public var checklistJson: String
    @NSManaged public var imageUri: String?
    @NSManaged public var reminderTime: Double
    @NSManaged public var createdAt: Double
    @NSManaged public var updatedAt: Double
    @NSManaged public var label: String?
}

struct NoteItem: Identifiable, Hashable {
    var id: Int64 = 0
    var title: String = ""
    var content: String = ""
    var colorHex: String = "#1E2240"
    var isPinned: Bool = false
    var isArchived: Bool = false
    var isChecklist: Bool = false
    var checklistJson: String = "[]"
    var imageUri: String? = nil
    var reminderTime: Date? = nil
    var createdAt: Date = Date()
    var updatedAt: Date = Date()
    var label: String? = nil
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: CalendarEvent
// ─────────────────────────────────────────────────────────────────────────────
@objc(CalendarEventMO)
public class CalendarEventMO: NSManagedObject {
    @NSManaged public var id: Int64
    @NSManaged public var title: String
    @NSManaged public var eventDescription: String
    @NSManaged public var startTime: Double
    @NSManaged public var endTime: Double
    @NSManaged public var allDay: Bool
    @NSManaged public var location: String
    @NSManaged public var colorHex: String
    @NSManaged public var reminderMinutes: Int32
    @NSManaged public var createdBy: String
    @NSManaged public var sharedWithAll: Bool
}

struct CalendarEvent: Identifiable, Hashable {
    var id: Int64 = 0
    var title: String
    var description: String = ""
    var startTime: Date
    var endTime: Date
    var allDay: Bool = false
    var location: String = ""
    var colorHex: String = "#3B82D4"
    var reminderMinutes: Int = 15
    var createdBy: String = ""
    var sharedWithAll: Bool = true
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Task
// ─────────────────────────────────────────────────────────────────────────────
@objc(TaskMO)
public class TaskMO: NSManagedObject {
    @NSManaged public var id: Int64
    @NSManaged public var title: String
    @NSManaged public var taskDescription: String
    @NSManaged public var isCompleted: Bool
    @NSManaged public var dueDate: Double
    @NSManaged public var priority: Int32    // 0=Low 1=Normal 2=High 3=Urgent
    @NSManaged public var assignedTo: String
    @NSManaged public var createdAt: Double
    @NSManaged public var completedAt: Double
    @NSManaged public var listName: String
}

struct TaskItem: Identifiable, Hashable {
    var id: Int64 = 0
    var title: String
    var description: String = ""
    var isCompleted: Bool = false
    var dueDate: Date? = nil
    var priority: Int = 1
    var assignedTo: String = ""
    var createdAt: Date = Date()
    var completedAt: Date? = nil
    var listName: String = "General"

    var priorityLabel: String {
        switch priority {
        case 0: return "Low"
        case 1: return "Normal"
        case 2: return "High"
        case 3: return "Urgent"
        default: return "Normal"
        }
    }

    var priorityColor: String {
        switch priority {
        case 0: return "#8892B0"
        case 1: return "#00D4FF"
        case 2: return "#FFAA00"
        case 3: return "#FF3D71"
        default: return "#00D4FF"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: BoardPost
// ─────────────────────────────────────────────────────────────────────────────
@objc(BoardPostMO)
public class BoardPostMO: NSManagedObject {
    @NSManaged public var id: Int64
    @NSManaged public var authorId: String
    @NSManaged public var authorName: String
    @NSManaged public var content: String
    @NSManaged public var imageUri: String
    @NSManaged public var pinned: Bool
    @NSManaged public var reactionsJson: String
    @NSManaged public var postType: String
    @NSManaged public var createdAt: Double
}

struct BoardPost: Identifiable, Hashable {
    var id: Int64 = 0
    var authorId: String
    var authorName: String
    var content: String
    var imageUri: String = ""
    var pinned: Bool = false
    var reactions: [String: Int] = [:]
    var postType: String = "chat"  // chat | announcement | event | photo | task
    var createdAt: Date = Date()

    var typeColor: String {
        switch postType {
        case "announcement": return "#FFAA00"
        case "event":        return "#00D4FF"
        case "photo":        return "#00E096"
        case "task":         return "#7B2FFF"
        default:             return "#3B82D4"
        }
    }

    var typeEmoji: String {
        switch postType {
        case "announcement": return "📢"
        case "event":        return "📅"
        case "photo":        return "📷"
        case "task":         return "✅"
        default:             return "💬"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: ShoppingItem
// ─────────────────────────────────────────────────────────────────────────────
struct ShoppingItem: Identifiable, Hashable {
    var id: Int64 = 0
    var name: String
    var quantity: String = "1"
    var category: String = "General"
    var addedBy: String = ""
    var isChecked: Bool = false
    var addedAt: Date = Date()
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Chore
// ─────────────────────────────────────────────────────────────────────────────
struct Chore: Identifiable, Hashable {
    var id: Int64 = 0
    var title: String
    var assignedTo: String
    var pointValue: Int = 10
    var isCompleted: Bool = false
    var dueDate: Date? = nil
    var recurrence: String = "NONE"
    var createdAt: Date = Date()
    var iconEmoji: String = "🧹"
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Poll
// ─────────────────────────────────────────────────────────────────────────────
struct FamilyPoll: Identifiable, Hashable {
    var id: Int64 = 0
    var question: String
    var options: [String] = []
    var votes: [String: [String]] = [:]  // option -> [memberId]
    var createdBy: String = ""
    var createdAt: Date = Date()
    var expiresAt: Date? = nil
    var isActive: Bool = true
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: BedtimeAlert
// ─────────────────────────────────────────────────────────────────────────────
struct BedtimeAlert: Identifiable, Hashable {
    var id: Int64 = 0
    var memberId: String
    var bedtimeHour: Int = 21
    var bedtimeMinute: Int = 0
    var wakeHour: Int = 7
    var wakeMinute: Int = 0
    var daysOfWeek: [Int] = [1,2,3,4,5,6,7]
    var isEnabled: Bool = true
    var graceMinutes: Int = 15
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: HealthRecord
// ─────────────────────────────────────────────────────────────────────────────
struct HealthRecord: Identifiable, Hashable {
    var id: Int64 = 0
    var memberId: String
    var steps: Int = 0
    var heartRate: Int = 0
    var calories: Int = 0
    var sleepHours: Double = 0
    var waterIntakeMl: Int = 0
    var recordDate: Date = Date()
    var weightKg: Double = 0
    var moodScore: Int = 3  // 1-5
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: SafeZone
// ─────────────────────────────────────────────────────────────────────────────
struct SafeZone: Identifiable, Hashable {
    var id: Int64 = 0
    var name: String
    var latitude: Double
    var longitude: Double
    var radiusMeters: Double = 200
    var colorHex: String = "#00D4FF"
    var alertOnExit: Bool = true
    var alertOnEntry: Bool = false
    var createdBy: String = ""
    var createdAt: Date = Date()
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: PhotoAlbum + Photo
// ─────────────────────────────────────────────────────────────────────────────
struct PhotoAlbum: Identifiable, Hashable {
    var id: Int64 = 0
    var name: String
    var coverUri: String? = nil
    var createdAt: Date = Date()
    var isShared: Bool = true
    var createdBy: String = ""
}

struct FamilyPhoto: Identifiable, Hashable {
    var id: Int64 = 0
    var albumId: Int64
    var uri: String
    var caption: String = ""
    var takenAt: Date = Date()
    var takenBy: String = ""
    var latitude: Double? = nil
    var longitude: Double? = nil
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: WeatherData
// ─────────────────────────────────────────────────────────────────────────────
struct WeatherData {
    var cityName: String
    var tempC: Double
    var feelsLikeC: Double
    var description: String
    var humidity: Int
    var windKph: Double
    var emoji: String
}
