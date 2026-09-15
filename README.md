# One Task

Kotlin + Jetpack Compose Android app, built with a clean MVVM structure.

- Package: `com.nj031.onetask`
- Min SDK 26, target/compile SDK 34
- Material 3, Jetpack Navigation Compose

## Structure

```
app/src/main/java/com/nj031/onetask/
  ui/screens/    Composable screens
  ui/theme/      Material 3 theme (Color, Type, Theme)
  viewmodel/     ViewModels
  data/          Repositories / data sources
  navigation/    NavHost + route definitions
  MainActivity.kt
```

## Build

Requires Android Studio (or the Android SDK + `ANDROID_HOME`/`local.properties`
pointing at it).

```
./gradlew assembleDebug
```
