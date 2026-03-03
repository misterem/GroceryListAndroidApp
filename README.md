# Grocery List Android Application

## Description
Grocery List is a native Android application designed to streamline the management of household shopping requirements. It provides a persistent, organized interface for tracking items, quantities, and categories to enhance the shopping experience.

## Key Features
- **Dynamic List Management**: Efficiently add, edit, and remove grocery items in real-time.
- **Barcode Scanning**: Integration of barcode scanning using the on-device camera for rapid item entry.
- **Persistent Data Storage**: Utilizes local database integration (SQLite/Room) to ensure list data is retained across application sessions.
- **Categorization & Sorting**: Logical grouping of items to facilitate faster in-store navigation.
- **User Interface**: Clean, responsive design following Android Material Design guidelines for optimal usability.

## Technical Stack
- **Platform**: Android
- **Language**: Java / Kotlin
- **Database**: SQLite / Room Persistence Library
- **UI Framework**: XML / Material Components
- **Architecture**: MVVM (Model-View-ViewModel)

## Repository Structure
- `/app/src/main/java/`: Contains the core application logic, activities, and data models.
- `/app/src/main/res/`: Resources including layout XML files, strings, and drawable assets.
- `build.gradle`: Configuration for dependencies and build parameters.
- `AndroidManifest.xml`: Defines application components and required permissions.

## Getting Started

### Prerequisites
- Android Studio (Electric Eel or newer recommended)
- Android SDK 33+
- A physical Android device or an Emulator

### Installation & Setup
1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/misterem/GroceryListAndroidApp.git
   ```
2. Open the project in **Android Studio**.
3. Sync the project with Gradle files to download necessary dependencies.
4. Build the project using `Build > Make Project`.
5. Run the application on your device or emulator using the `Run` button.

## Future Enhancements
- Implementation of cloud synchronization for multi-device support.
- Shared list functionality for collaborative household management.
