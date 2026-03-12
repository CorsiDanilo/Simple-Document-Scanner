[🏠 Index](./README.md) | [Next ➡](./02-structure.md)

# Project Overview

The `simple-document-scanner` is a native Android application designed to streamline the digitization of physical documents. By leveraging the Google ML Kit Document Scanner API, the application provides a robust, high-quality scanning experience, allowing users to capture, process, and manage documents directly on their devices.

## Purpose and Domain

The application operates within the document management domain, focusing on the conversion of physical media into digital formats (PDF and JPEG). It provides a clean, modern interface for users to scan, store, and share documents, ensuring that scanned files are easily accessible and portable.

## Key Features and Capabilities

*   **Document Scanning:** Integrates the Google ML Kit `GmsDocumentScanning` API for high-fidelity document detection and cropping.
*   **Multi-Format Support:** Supports saving scans as PDF documents or individual JPEG images.
*   **Local Storage Management:** Utilizes a dedicated `FileManager` to handle file I/O operations, ensuring secure storage within the app's private directory.
*   **Document Sharing:** Implements `FileProvider` to generate secure, shareable URIs for external applications.
*   **Modern UI/UX:** Built with Jetpack Compose and Material 3, featuring dynamic color support and a responsive design.
*   **Asynchronous Processing:** Uses Kotlin Coroutines and `Flow` to ensure a non-blocking, responsive user experience during heavy I/O operations.

## Technology Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose, Material 3 |
| **Architecture** | Clean Architecture (MVVM) |
| **Dependency Injection** | Hilt |
| **Asynchronous** | Coroutines, Flow |
| **Scanning API** | Google ML Kit Document Scanner |
| **Image Loading** | Coil |
| **Navigation** | Jetpack Navigation Compose |

## High-Level Architecture

The project follows a Clean Architecture pattern, separating concerns into distinct layers: **Presentation**, **Domain**, and **Data**. This ensures testability and maintainability.

```mermaid
graph TD
    subgraph Presentation
        UI[Screens: HomeScreen, ScannerScreen, ResultScreen, ScansScreen]
        VM[ViewModels: ScannerViewModel, ScansViewModel]
    end

    subgraph Domain
        UC[UseCases: SaveDocumentUseCase, ShareDocumentUseCase]
        RepoInt[Repository Interface: IDocumentRepository]
        Model[Models: ScannedDocument]
    end

    subgraph Data
        RepoImpl[Repository Implementation: DocumentRepositoryImpl]
        FM[FileManager]
        Storage[(Local Storage)]
    end

    UI --> VM
    VM --> UC
    UC --> RepoInt
    RepoImpl -. implements .-> RepoInt
    RepoImpl --> FM
    FM --> Storage
```

## Quick Links

*   **[Getting Started](./03-setup.md)**: Setup instructions and build requirements.
*   **[Architecture Guide](./02-structure.md)**: Detailed breakdown of the MVVM and Clean Architecture implementation.
*   **[API Reference](./07-domain-layer-logic.md)**: Documentation for `IDocumentRepository` and core use cases.

[🏠 Index](./README.md) | [Next ➡](./02-structure.md)