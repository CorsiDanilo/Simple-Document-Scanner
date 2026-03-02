# Changelog

All notable changes to this project will be documented in this file.

## [1.0.1] - 2026-03-02

### Added
- **Swipe Gestures on Scans**: 
  - Left-to-Right swipe (Blue) to rename scans instantly.
  - Right-to-Left swipe (Red) to delete scans directly from the list.
- **Onboarding UI Hint**: First-time users will now see an automated slide-in and out visual hint of the swipe actions on their first scan.

### UI/UX Improvements
- **Preview Screen Optimization**: Preview background was enhanced to appear cleaner and layout was fixed to prevent the image from overlapping with the document name.
- **Auto-save on Share**: Shared scans from the Preview Screen are now seamlessly saved in "My Scans" automatically. Added an informative text on the share dialog explaining this.
- **Cleaner Scan List**: Removed static edit/delete action icons from the individual scan rows resulting in a cleaner, minimalist layout relying on gestures.

## [1.0.0] - 2026-03-01

### Highlights
- Official launch of the application with a premium, minimalist B&W aesthetic.

### Added
- **New Visual Identity**: Implemented a professional, high-contrast B&W app icon and logo.
- **Custom Splash Screen**: Added a modern splash screen using the Android SplashScreen API with a 1-second stay-on-screen condition.
- **Destructive Action Safeguards**: Added confirmation dialogs for deleting scans and discarding unsaved work to prevent data loss.
- **Localization**: Full Italian translation (`values-it`) for all interface strings.
- **CI/CD Pipeline**: Integrated GitHub Actions for automated building, signing, and releasing of APKs.

### UI/UX Improvements
- Adopted Material 3 (Material You) design principles throughout the app.
- Enhanced navigation flow between the home, scanner, and document management screens.
- Improved multi-file sharing support for both PDF and Image formats.