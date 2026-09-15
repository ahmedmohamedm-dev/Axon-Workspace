# Axon Workspace

A modern Windows desktop workspace manager for organizing software projects and architecture as an interactive tree.

## Features

- Hierarchical project tree with folders and files
- Project status tracking: Not Started, In Progress, Done
- Notes and History tabs
- Smart search with animated tree expansion
- Light, Dark, and Auto themes
- Customizable keyboard shortcuts
- JSON workspace linking and auto-sync
- Import from JSON
- Local and portable workspace data
- Glassmorphism UI inspired by Samsung One UI

## Built With

- Kotlin
- Compose for Desktop
- Kotlin Serialization
- Kotlin Coroutines
- JavaFX

## Requirements

- JDK 21
- Gradle 8.10+

## Run

```bash
./gradlew run
```

## Build

On Windows:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\gradlew.bat createDistributable
```

The distributable application is generated under:

```text
build/compose/binaries/main/app/
```

## Data

Application data is stored locally in:

```text
%USERPROFILE%\.axon-workspace\
```

## Project History

Axon Workspace was originally created as an HTML/JavaScript project.

It was later fully migrated to Kotlin and Compose for Desktop.

The idea, direction, and overall product requirements were defined by me. The project was developed from 0 to 100 through AI-assisted coding. My role was to guide and monitor the development, review the UI/UX, test the application, and validate the final result.

## License

This project is open source and licensed under the MIT License.
