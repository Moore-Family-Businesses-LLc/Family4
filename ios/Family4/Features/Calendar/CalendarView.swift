import SwiftUI

struct CalendarView: View {
    @EnvironmentObject var store: AppStore
    @State private var selectedDate = Date()
    @State private var showAddEvent = false
    @State private var displayedMonth = Date()

    private let calendar = Calendar.current
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)
    private let dayNames = ["S","M","T","W","T","F","S"]

    var eventsForSelected: [CalendarEvent] {
        store.calendarEvents.filter { calendar.isDate($0.startTime, inSameDayAs: selectedDate) }
            .sorted { $0.startTime < $1.startTime }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Calendar", action: { showAddEvent = true }, actionLabel: "+ Event")

                    ScrollView {
                        VStack(spacing: 0) {
                            // Month nav
                            HStack {
                                Button { shiftMonth(-1) } label: {
                                    Image(systemName: "chevron.left").foregroundColor(.accentCyan)
                                }
                                Spacer()
                                Text(displayedMonth.formatted(.dateTime.month(.wide).year()))
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.textPrimary)
                                Spacer()
                                Button { displayedMonth = Date(); selectedDate = Date() } label: {
                                    Text("Today").font(.system(size: 13)).foregroundColor(.accentCyan)
                                }
                                Button { shiftMonth(1) } label: {
                                    Image(systemName: "chevron.right").foregroundColor(.accentCyan)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(Color.bgSurface)

                            // Day headers
                            HStack(spacing: 0) {
                                ForEach(dayNames, id: \.self) { d in
                                    Text(d)
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(.textMuted)
                                        .frame(maxWidth: .infinity)
                                }
                            }
                            .padding(.vertical, 8)
                            .background(Color.bgSurface)
                            Divider().background(Color.divider)

                            // Calendar grid
                            LazyVGrid(columns: columns, spacing: 0) {
                                ForEach(daysInGrid, id: \.self) { date in
                                    CalendarDayCell(
                                        date: date,
                                        isToday: date.map { calendar.isDateInToday($0) } ?? false,
                                        isSelected: date.map { calendar.isDate($0, inSameDayAs: selectedDate) } ?? false,
                                        hasEvent: date.map { hasEvents($0) } ?? false,
                                        eventColor: date.map { firstEventColor($0) } ?? nil
                                    )
                                    .onTapGesture {
                                        if let d = date { selectedDate = d }
                                    }
                                }
                            }
                            .background(Color.bgSurface)
                            .padding(.bottom, 8)

                            // Selected day label
                            HStack {
                                Text(selectedDate.formatted(.dateTime.weekday(.wide).month().day().year()))
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(.textSecondary)
                                Spacer()
                                if !eventsForSelected.isEmpty {
                                    CyanChip(text: "\(eventsForSelected.count) event\(eventsForSelected.count == 1 ? "" : "s")")
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)

                            // Events list
                            if eventsForSelected.isEmpty {
                                EmptyStateView(title: "No events", subtitle: "Tap + Event to add one",
                                               systemIcon: "calendar.badge.plus")
                                    .frame(height: 200)
                            } else {
                                LazyVStack(spacing: 8) {
                                    ForEach(eventsForSelected) { event in
                                        CalendarEventRow(event: event) {
                                            store.deleteCalendarEvent(event)
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                            }
                            Spacer(minLength: 40)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAddEvent) {
                AddEventSheet(selectedDate: selectedDate)
            }
            .navigationBarHidden(true)
        }
    }

    // MARK: - Helpers
    private func shiftMonth(_ delta: Int) {
        if let d = calendar.date(byAdding: .month, value: delta, to: displayedMonth) {
            displayedMonth = d
        }
    }

    private var daysInGrid: [Date?] {
        guard let monthStart = calendar.dateInterval(of: .month, for: displayedMonth)?.start else { return [] }
        let weekday = calendar.component(.weekday, from: monthStart) - 1
        let daysInMonth = calendar.range(of: .day, in: .month, for: displayedMonth)?.count ?? 30
        var days: [Date?] = Array(repeating: nil, count: weekday)
        for d in 0..<daysInMonth {
            days.append(calendar.date(byAdding: .day, value: d, to: monthStart))
        }
        // pad to complete row
        while days.count % 7 != 0 { days.append(nil) }
        return days
    }

    private func hasEvents(_ date: Date) -> Bool {
        store.calendarEvents.contains { calendar.isDate($0.startTime, inSameDayAs: date) }
    }

    private func firstEventColor(_ date: Date) -> Color? {
        guard let event = store.calendarEvents.first(where: { calendar.isDate($0.startTime, inSameDayAs: date) }) else { return nil }
        return Color(hex: event.colorHex)
    }
}

// MARK: - Day Cell
struct CalendarDayCell: View {
    let date: Date?
    let isToday: Bool
    let isSelected: Bool
    let hasEvent: Bool
    let eventColor: Color?

    var body: some View {
        VStack(spacing: 3) {
            if let date = date {
                let day = Calendar.current.component(.day, from: date)
                ZStack {
                    if isToday {
                        Circle().fill(Color.accentCyan).frame(width: 34, height: 34)
                    } else if isSelected {
                        Circle().fill(Color.accentPurple).frame(width: 34, height: 34)
                    }
                    Text("\(day)")
                        .font(.system(size: 14, weight: isToday || isSelected ? .bold : .regular))
                        .foregroundColor(isToday || isSelected ? .bgPrimary : .textPrimary)
                }
                // Event dot
                Circle()
                    .fill(eventColor ?? Color.accentCyan)
                    .frame(width: 5, height: 5)
                    .opacity(hasEvent ? 1 : 0)
            }
        }
        .frame(height: 50)
    }
}

// MARK: - Event Row
struct CalendarEventRow: View {
    let event: CalendarEvent
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: event.colorHex))
                .frame(width: 4)
            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.textPrimary)
                if event.allDay {
                    Text("All day")
                        .font(.system(size: 12))
                        .foregroundColor(.textMuted)
                } else {
                    Text("\(event.startTime.formatted(date: .omitted, time: .shortened)) – \(event.endTime.formatted(date: .omitted, time: .shortened))")
                        .font(.system(size: 12))
                        .foregroundColor(.textMuted)
                }
                if !event.location.isEmpty {
                    Label(event.location, systemImage: "mappin.circle")
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                }
            }
            .padding(12)
            Spacer()
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .font(.system(size: 13))
                    .foregroundColor(.errorRed.opacity(0.7))
            }
            .padding(.trailing, 12)
        }
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.cardStroke, lineWidth: 1))
    }
}

// MARK: - Add Event Sheet
struct AddEventSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    let selectedDate: Date

    @State private var title = ""
    @State private var description = ""
    @State private var location = ""
    @State private var startTime: Date
    @State private var endTime: Date
    @State private var allDay = false
    @State private var pickedColorHex = "#3B82D4"
    @State private var reminderMinutes = 15

    private let colorOptions = ["#3B82D4","#00C851","#FF4444","#FF8800","#7B2FFF","#FF69B4","#00D4FF"]
    private let reminderOptions = [0, 5, 15, 30, 60]

    init(selectedDate: Date) {
        self.selectedDate = selectedDate
        var start = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: selectedDate) ?? selectedDate
        _startTime = State(initialValue: start)
        _endTime = State(initialValue: start.addingTimeInterval(3600))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 20) {
                        // Title
                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Event Title")
                            TextField("Enter title…", text: $title)
                                .textFieldStyle(F4TextFieldStyle())
                        }

                        // Date / time
                        VStack(alignment: .leading, spacing: 6) {
                            Toggle("All Day", isOn: $allDay)
                                .toggleStyle(SwitchToggleStyle(tint: .accentCyan))
                                .padding(.horizontal, 16)
                                .foregroundColor(.textPrimary)

                            if !allDay {
                                DatePicker("Start", selection: $startTime, displayedComponents: [.date, .hourAndMinute])
                                    .padding(.horizontal, 16)
                                    .foregroundColor(.textPrimary)
                                    .colorScheme(.dark)
                                DatePicker("End", selection: $endTime, in: startTime..., displayedComponents: [.date, .hourAndMinute])
                                    .padding(.horizontal, 16)
                                    .foregroundColor(.textPrimary)
                                    .colorScheme(.dark)
                            }
                        }
                        .padding(.vertical, 4)

                        // Location
                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Location (optional)")
                            TextField("Add location…", text: $location)
                                .textFieldStyle(F4TextFieldStyle())
                        }

                        // Color picker
                        VStack(alignment: .leading, spacing: 8) {
                            SectionLabel(text: "Colour")
                            HStack(spacing: 12) {
                                ForEach(colorOptions, id: \.self) { hex in
                                    Circle()
                                        .fill(Color(hex: hex))
                                        .frame(width: 32, height: 32)
                                        .scaleEffect(pickedColorHex == hex ? 1.3 : 1.0)
                                        .overlay(Circle().stroke(Color.white.opacity(pickedColorHex == hex ? 0.8 : 0), lineWidth: 2))
                                        .onTapGesture {
                                            withAnimation(.spring()) { pickedColorHex = hex }
                                        }
                                }
                            }
                            .padding(.horizontal, 16)
                        }

                        // Reminder
                        VStack(alignment: .leading, spacing: 8) {
                            SectionLabel(text: "Reminder")
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(reminderOptions, id: \.self) { min in
                                        Text(min == 0 ? "None" : "\(min) min")
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(reminderMinutes == min ? .bgPrimary : .accentCyan)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 7)
                                            .background(reminderMinutes == min ? Color.accentCyan : Color.accentCyan.opacity(0.12))
                                            .clipShape(Capsule())
                                            .onTapGesture { reminderMinutes = min }
                                    }
                                }
                                .padding(.horizontal, 16)
                            }
                        }

                        // Description
                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Description (optional)")
                            TextField("Notes about this event…", text: $description, axis: .vertical)
                                .textFieldStyle(F4TextFieldStyle())
                                .lineLimit(3...)
                        }

                        Spacer(minLength: 20)
                    }
                    .padding(.top, 8)
                }
            }
            .navigationTitle("New Event")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }.foregroundColor(.textMuted)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { save() }
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(title.isEmpty ? .textMuted : .accentCyan)
                        .disabled(title.isEmpty)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    private func save() {
        let event = CalendarEvent(
            title: title,
            description: description,
            startTime: allDay ? Calendar.current.startOfDay(for: startTime) : startTime,
            endTime: allDay ? Calendar.current.date(bySettingHour: 23, minute: 59, second: 0, of: endTime) ?? endTime : endTime,
            allDay: allDay,
            location: location,
            colorHex: pickedColorHex,
            reminderMinutes: reminderMinutes
        )
        store.addCalendarEvent(event)
        dismiss()
    }
}

// MARK: - Custom TextField Style
struct F4TextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .foregroundColor(.textPrimary)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.cardStroke, lineWidth: 1))
            .padding(.horizontal, 16)
    }
}
