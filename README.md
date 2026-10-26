# AttendEase

An Android attendance app for SUTD. Students tap their NFC student cards on the lecturer's phone to register attendance, and lecturers run and manage attendance sessions from the same app.

Built in **Java** with MVVM, Room, LiveData, ViewBinding and the AndroidX Navigation Component. All data is stored on the device, so no backend or account setup is needed.

**[Download the latest APK](https://github.com/ngzhankang/AttendEase/releases/latest)**

## Try it

1. Install the APK from the [latest release](https://github.com/ngzhankang/AttendEase/releases/latest) on an Android 7.0+ phone (you may need to allow installs from unknown sources).
2. Log in with a demo account. Every account uses the password `password`.

| Role | ID |
|------|----|
| Lecturer | `1000001` (Dr. Alice Tan), `1000002` (Dr. Bob Lim) |
| Student | `1006001` to `1006005` |

The demo data is generated on first launch around the current date, so the term is always in week 15 and lecturer `1000001` always has a 50.001 lesson today. All names and IDs are fictional.

### Try NFC attendance with your own card

You need an NFC-enabled phone and any NFC card, such as a transit card, student card or office pass.

1. Log in as lecturer `1000001`, open **Lectures**, expand **50.001 Information Systems** and tap **Start Attendance** on today's lesson.
2. Tap your card on the back of the phone. It shows **Card Not Recognised** with a **Link this card to a student** button.
3. Tap the button and pick a student. The card is linked and their attendance is registered.
4. Tap the card again to see the **Already Registered** state.
5. End the session, log in as that student, and check their updated attendance.

The card's ID is stored only in the app's local database on your phone. Bank cards and phone wallets show a different ID on every tap, so they can't be linked.

### No NFC?

On a device without NFC, such as an emulator, the scanner shows a **Simulate card tap** button. Choose a student to tap their card, or choose an unregistered card to try the linking flow. Simulated taps go through the same code as real ones.

## Features

### Students
- **Home:** a pulsing "Attendance Taking Now" banner while a lecturer has a session open, a list of upcoming lessons, and an attendance overview for each module with a progress bar coloured green (80% or more), amber (60–79%) or red (under 60%).
- **Calendar:** a custom month grid built with RecyclerView (no calendar library). Coloured dots mark lessons, exams and recess, and tapping a date lists that day's lessons.
- **Profile:** student ID, card ID, change or reset password, and log out.

### Lecturers
- **Home:** today's teaching schedule.
- **Lectures:** the modules they teach, each expanding into its lessons. From a lesson they can start or stop attendance, add a student manually by ID, and view who attended. From a module they can view overall attendance per student and export it as a CSV through the share sheet.
- **Profile:** password management and log out.

### NFC scanning
When a lecturer starts a session, a full-screen scanner reads each card's UID and matches it to a student. The result is designed to be visible from a distance:

| Result | Screen |
|--------|--------|
| Attendance registered | Green, ✓, student name, with a confirmation tone |
| Already registered | Amber, student name |
| Card not recognised | Red, ✗ |

If an unknown card is scanned, the lecturer can link it to an enrolled student who doesn't have a card yet. A counter shows how many students have scanned. Devices without NFC get a button that simulates card taps. Opening and closing a session flips the lesson's active flag, which is what shows or hides the live banner on students' phones.

## Architecture

```
app/src/main/java/com/example/attendease/
├── data/
│   ├── local/        Room entities, DAOs and AppDatabase (singleton + seed callback)
│   ├── model/        Query result POJOs (e.g. LessonWithAttendanceCount, StudentAttendanceSummary)
│   └── repository/   UserRepository, AttendanceRepository
├── ui/
│   ├── auth/         SplashActivity, LoginActivity
│   ├── student/      Home, Calendar and their adapters
│   ├── lecturer/     Home, Lectures and their adapters
│   ├── profile/      ProfileFragment, shared by both roles
│   └── nfc/          NfcScanActivity
├── viewmodel/        StudentViewModel, LecturerViewModel, SharedProfileViewModel
└── util/             Constants, DateUtils, HashUtils, NfcUtils, ExportUtils
```

- **MVVM:** fragments observe `LiveData` from ViewModels and never touch DAOs directly.
- **Reactive updates:** Room DAOs return `LiveData`, so when a lecturer starts a session, the student home screen updates without polling.
- **Login:** passwords are hashed with SHA-256 (`java.security.MessageDigest`) and the session is kept in `SharedPreferences`. The splash screen skips login when a session exists.
- **Data model:** `User`, `Module`, `Lesson`, `Enrollment` (unique per student and module) and `Attendance` (unique per lesson and student, recorded as `NFC` or `MANUAL`).

## Tech stack

| | |
|---|---|
| Language | Java 11 |
| Min / target SDK | 24 / 36 |
| UI | Material Design 3, ViewBinding, Navigation Component, BottomNavigationView |
| Data | Room (SQLite), LiveData, ViewModel |
| Hardware | Android NFC foreground dispatch |
| Build | Gradle (Kotlin DSL), version catalog in `gradle/libs.versions.toml` |

## Building

Requires the Android SDK and JDK 11 or newer.

```bash
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # install on a connected device
./gradlew test                 # unit tests
```

### Signed release build

Release builds are signed when a `keystore.properties` file exists in the project root:

```properties
storeFile=keystore/attendease-release.jks
storePassword=...
keyAlias=attendease
keyPassword=...
```

Then run `./gradlew assembleRelease`. The APK is written to `app/build/outputs/apk/release/`. Without `keystore.properties` the release build still succeeds but is unsigned. The keystore and properties file are gitignored.

## License

[MIT](LICENSE)
