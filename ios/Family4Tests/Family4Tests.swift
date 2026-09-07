import XCTest

// MARK: - Family4 Unit Tests
// These tests run in CI via GitHub Actions on the macos-14 runner.
// Each screen's ViewModel is tested in isolation — no UI required.

@testable import Family4

final class AppStoreTests: XCTestCase {

    var store: AppStore!

    override func setUp() {
        super.setUp()
        // Create a fresh in-memory store for each test
        store = AppStore.shared
        store.notes = []
        store.tasks = []
        store.calendarEvents = []
        store.boardPosts = []
        store.shoppingItems = []
        store.chores = []
        store.polls = []
    }

    // MARK: - Notes
    func testAddNote() {
        let note = NoteItem(title: "Test Note", content: "Hello")
        store.addNote(note)
        XCTAssertEqual(store.notes.count, 1)
        XCTAssertEqual(store.notes.first?.title, "Test Note")
    }

    func testDeleteNote() {
        let note = NoteItem(title: "Delete me", content: "")
        store.addNote(note)
        XCTAssertEqual(store.notes.count, 1)
        store.deleteNote(store.notes.first!)
        XCTAssertEqual(store.notes.count, 0)
    }

    func testTogglePinNote() {
        var note = NoteItem(title: "Pin me", content: "")
        store.addNote(note)
        let saved = store.notes.first!
        XCTAssertFalse(saved.isPinned)
        store.togglePin(saved)
        XCTAssertTrue(store.notes.first?.isPinned ?? false)
    }

    // MARK: - Tasks
    func testAddTask() {
        let task = TaskItem(title: "Buy milk", priority: 2)
        store.addTask(task)
        XCTAssertEqual(store.tasks.count, 1)
        XCTAssertEqual(store.tasks.first?.title, "Buy milk")
        XCTAssertEqual(store.tasks.first?.priority, 2)
    }

    func testToggleTask() {
        let task = TaskItem(title: "Task A", priority: 1)
        store.addTask(task)
        let saved = store.tasks.first!
        XCTAssertFalse(saved.isCompleted)
        store.toggleTask(saved)
        XCTAssertTrue(store.tasks.first?.isCompleted ?? false)
        XCTAssertNotNil(store.tasks.first?.completedAt)
    }

    func testDeleteTask() {
        let task = TaskItem(title: "Gone", priority: 0)
        store.addTask(task)
        store.deleteTask(store.tasks.first!)
        XCTAssertEqual(store.tasks.count, 0)
    }

    // MARK: - Calendar
    func testAddCalendarEvent() {
        let event = CalendarEvent(title: "Family Dinner",
                                  startTime: Date(),
                                  endTime: Date().addingTimeInterval(3600))
        store.addCalendarEvent(event)
        XCTAssertEqual(store.calendarEvents.count, 1)
        XCTAssertEqual(store.calendarEvents.first?.title, "Family Dinner")
    }

    func testDeleteCalendarEvent() {
        let event = CalendarEvent(title: "To Delete",
                                  startTime: Date(),
                                  endTime: Date().addingTimeInterval(3600))
        store.addCalendarEvent(event)
        store.deleteCalendarEvent(store.calendarEvents.first!)
        XCTAssertEqual(store.calendarEvents.count, 0)
    }

    // MARK: - Board
    func testAddBoardPost() {
        let post = BoardPost(authorId: "dad", authorName: "Dad", content: "Hello family!")
        store.addBoardPost(post)
        XCTAssertEqual(store.boardPosts.count, 1)
        XCTAssertEqual(store.boardPosts.first?.content, "Hello family!")
    }

    func testReactToBoardPost() {
        let post = BoardPost(authorId: "dad", authorName: "Dad", content: "Test")
        store.addBoardPost(post)
        let id = store.boardPosts.first!.id
        store.reactToPost(postId: id, emoji: "❤️")
        store.reactToPost(postId: id, emoji: "❤️")
        XCTAssertEqual(store.boardPosts.first?.reactions["❤️"], 2)
    }

    // MARK: - Shopping
    func testShoppingCRUD() {
        let item = ShoppingItem(name: "Apples", quantity: "6", category: "Produce")
        store.addShoppingItem(item)
        XCTAssertEqual(store.shoppingItems.count, 1)
        store.toggleShoppingItem(store.shoppingItems.first!)
        XCTAssertTrue(store.shoppingItems.first?.isChecked ?? false)
        store.deleteShoppingItem(store.shoppingItems.first!)
        XCTAssertEqual(store.shoppingItems.count, 0)
    }

    // MARK: - Chores
    func testChoreCompletion() {
        let chore = Chore(title: "Wash dishes", assignedTo: "Alex", pointValue: 15, iconEmoji: "🍽️")
        store.addChore(chore)
        store.toggleChore(store.chores.first!)
        XCTAssertTrue(store.chores.first?.isCompleted ?? false)
    }

    // MARK: - FamilyMember helpers
    func testFamilyMemberInitials() {
        let member = FamilyMember(id: "1", displayName: "John Smith")
        XCTAssertEqual(member.initials, "JS")
    }

    func testSingleNameInitials() {
        let member = FamilyMember(id: "2", displayName: "Mom")
        XCTAssertEqual(member.initials, "M")
    }

    // MARK: - TaskItem helpers
    func testTaskPriorityLabels() {
        XCTAssertEqual(TaskItem(title: "A", priority: 0).priorityLabel, "Low")
        XCTAssertEqual(TaskItem(title: "A", priority: 1).priorityLabel, "Normal")
        XCTAssertEqual(TaskItem(title: "A", priority: 2).priorityLabel, "High")
        XCTAssertEqual(TaskItem(title: "A", priority: 3).priorityLabel, "Urgent")
    }

    // MARK: - BoardPost helpers
    func testBoardPostTypeColor() {
        XCTAssertEqual(BoardPost(authorId: "x", authorName: "X", content: "", postType: "announcement").typeColor, "#FFAA00")
        XCTAssertEqual(BoardPost(authorId: "x", authorName: "X", content: "", postType: "event").typeColor, "#00D4FF")
        XCTAssertEqual(BoardPost(authorId: "x", authorName: "X", content: "", postType: "photo").typeColor, "#00E096")
    }
}
