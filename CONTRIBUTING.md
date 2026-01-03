# Contributing to Essentials

Thank you for your interest in contributing to Essentials! This guide will help you set up your development environment and understand the architecture of the project.

## Environment Setup

1. **Android Studio**: Download and install the latest version of [Android Studio](https://developer.android.com/studio).
2. **JDK**: Ensure you have JDK 17 or higher installed.
3. **Clone the project**:
   ```bash
   git clone https://github.com/sameerasw/essentials.git
   ```
4. **Open in Android Studio**: Open the project and wait for Gradle to sync.
5. **Shizuku**: Many features require [Shizuku](https://shizuku.rikka.app/). Install it on your device for testing.

## Architecture Overview

Essentials follows a modern Android architecture:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Pattern**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Manual injection (view models are managed by `MainViewModel` or passed through activities).

## Feature Implementation Workflow

Adding a new feature involves three main steps:

### 1. Define Metadata in `FeatureRegistry.kt`

All features must be registered in the `FeatureRegistry` object. This centralizes metadata and enables automated search indexing.

```kotlin
object : Feature(
    id = "MyNewFeature",
    title = "My New Feature",
    iconRes = R.drawable.my_feature_icon,
    category = "Tools",
    description = "A short description of the feature",
    permissionKeys = listOf("ACCESSIBILITY"), // Optional
    searchableSettings = listOf(
        SearchSetting("Option Title", "Description", "highlight_key", listOf("keyword1", "keyword2"))
    )
) {
    override fun isEnabled(viewModel: MainViewModel) = viewModel.isMyFeatureEnabled.value
    override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {
        viewModel.setMyFeatureEnabled(enabled, context)
    }
}
```

### 2. Create the Settings UI

Create a new composable in `app/src/main/java/com/sameerasw/essentials/ui/composables/configs/`.

- Use `RoundedCardContainer` for grouped items.
- Use `IconToggleItem` or `SimpleToggleItem` for toggles.
- Use `Modifier.highlight(highlightSetting == "key")` to support search highlighting.

### 3. Register in `FeatureSettingsActivity.kt`

Add your new UI to the `when(feature)` block in `FeatureSettingsActivity.kt` to link it to the registration ID.

```kotlin
"MyNewFeature" -> {
    MyNewFeatureSettingsUI(
        viewModel = viewModel,
        modifier = Modifier.padding(top = 16.dp),
        highlightSetting = highlightSetting
    )
}
```

## Search System

The search system is fully automated. By adding `SearchSetting` objects to your feature in `FeatureRegistry.kt`, they will automatically:

1. Appear in the universal search results.
2. Navigate the user to the correct feature screen.
3. Trigger a pulse animation on the target item via the `highlight` modifier.

## Code Style

- Use **PascalCase** for Composables.
- Use **camelCase** for variables and functions.
- Prefer **functional components** and avoid heavy logic in the UI layer.

## Pull Requests

We welcome pull requests! To ensure a smooth review process, please follow these guidelines:

1.  **Create a Branch**: Create a new branch for your feature or bugfix (e.g., `feature/my-new-feature` or `fix/issue-description`).
2.  **Keep it Focused**: A PR should ideally do one thing. If you have multiple unrelated changes, please separate them into multiple PRs.
3.  **Test Your Changes**: Before submitting, ensure that your changes build correctly and that you've tested them on a physical device or emulator.
4.  **Describe Your Work**: In your PR description, explain _what_ you changed and _why_. If your change affects the UI, please include screenshots or a screen recording.
5.  **Code Style**: Ensure your code follows the existing style of the project.
6.  **Update Documentation**: If you've added a new feature, ensure you've registered it in `FeatureRegistry.kt` as described above so it's searchable.
7. **All to develop**: Please make sure your branches are based on `develop` and also they are set to merge back to `develop` as well.

## Questions?

If you have any questions or need help, feel free to open an issue or reach out in our community channels.
