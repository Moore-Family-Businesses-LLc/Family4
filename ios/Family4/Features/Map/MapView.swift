import SwiftUI
import MapKit
import CoreLocation

struct MapView: View {
    @EnvironmentObject var store: AppStore
    @StateObject private var locationManager = LocationManager()
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 37.3318, longitude: -122.0312),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var isFollowingUser = true

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 0) {
                SectionHeaderView(title: "Family Map")

                ZStack(alignment: .topTrailing) {
                    Map(coordinateRegion: $region,
                        showsUserLocation: true,
                        annotationItems: memberAnnotations) { annotation in
                        MapAnnotation(coordinate: annotation.coordinate) {
                            MemberMapPin(member: annotation.member)
                        }
                    }
                    .colorScheme(.dark)
                    .ignoresSafeArea(edges: .bottom)

                    // Controls overlay
                    VStack(spacing: 8) {
                        Button {
                            if let loc = locationManager.lastLocation {
                                withAnimation {
                                    region.center = loc.coordinate
                                    isFollowingUser = true
                                }
                            }
                        } label: {
                            Image(systemName: "location.fill")
                                .foregroundColor(isFollowingUser ? .accentCyan : .textPrimary)
                                .frame(width: 44, height: 44)
                                .background(Color.bgSurface.opacity(0.9))
                                .clipShape(Circle())
                        }
                    }
                    .padding(16)
                }
            }

            // Member list bottom card
            VStack {
                Spacer()
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(store.members) { member in
                            MemberLocationChip(member: member)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .background(Color.bgSurface.opacity(0.95))
            }
        }
        .onAppear { locationManager.start() }
    }

    private var memberAnnotations: [MemberAnnotation] {
        store.members.compactMap { member in
            guard let lat = member.latitude, let lon = member.longitude else { return nil }
            return MemberAnnotation(member: member, coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon))
        }
    }
}

struct MemberAnnotation: Identifiable {
    let id = UUID()
    let member: FamilyMember
    let coordinate: CLLocationCoordinate2D
}

struct MemberMapPin: View {
    let member: FamilyMember
    var body: some View {
        VStack(spacing: 2) {
            AvatarView(name: member.displayName, size: 36)
                .overlay(Circle().stroke(Color.accentCyan, lineWidth: 2))
            Image(systemName: "triangle.fill")
                .font(.system(size: 8))
                .foregroundColor(.accentCyan)
        }
    }
}

struct MemberLocationChip: View {
    let member: FamilyMember
    var body: some View {
        HStack(spacing: 8) {
            AvatarView(name: member.displayName, size: 32)
            VStack(alignment: .leading, spacing: 1) {
                Text(member.displayName)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.textPrimary)
                Text(member.isOnline ? "Online" : "Last seen \(member.lastSeen.formatted(date: .omitted, time: .shortened))")
                    .font(.system(size: 10))
                    .foregroundColor(.textMuted)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// MARK: - Location Manager
class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var lastLocation: CLLocation? = nil
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func start() {
        manager.requestWhenInUseAuthorization()
        manager.startUpdatingLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        lastLocation = locations.last
    }
}
