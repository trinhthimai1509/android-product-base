# Theming

**A product brands itself by passing parameters, never by editing `core:designsystem`.**

Every visual decision in `ProductBaseTheme` is a parameter with a neutral default. The defaults
exist so a new app runs on day one, not so anyone ships them.

---

## The one entry point

```kotlin
@Composable
fun ProductBaseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    lightColors: ColorScheme = ProductBaseLightColors,
    darkColors: ColorScheme = ProductBaseDarkColors,
    lightSemanticColors: SemanticColors = ProductBaseLightSemanticColors,
    darkSemanticColors: SemanticColors = ProductBaseDarkSemanticColors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = ProductBaseShapes,
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
)
```

A fully branded product looks like this — one call, in one place, wrapping the whole app:

```kotlin
ProductBaseTheme(
    themeMode = uiState.themeMode,
    dynamicColor = uiState.dynamicColorEnabled,
    lightColors = TrackerLightColors,
    darkColors = TrackerDarkColors,
    typography = TrackerTypography,
    shapes = TrackerShapes,
    spacing = Spacing(md = 20.dp),
) {
    AppNavHost()
}
```

---

## Colors

Supply Material 3 `ColorScheme`s. Generate them from your brand colour with the Material Theme
Builder, or write them by hand.

```kotlin
// ui/theme/Color.kt — product code
val TrackerLightColors = lightColorScheme(
    primary = Color(0xFF00658F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    // …every M3 role you care about; unspecified roles keep Material's defaults
)

val TrackerDarkColors = darkColorScheme(
    primary = Color(0xFF88CEFF),
    onPrimary = Color(0xFF00344C),
    // …
)
```

Read colours in UI code through `MaterialTheme.colorScheme`, never from a top-level `val` — that
is what makes light/dark and dynamic colour work at all.

The Base's own `ProductBaseLightColors` / `ProductBaseDarkColors` are a deliberately neutral
indigo/slate/teal fallback. **Shipping them is a decision to have no brand.**

---

## Typography

A plain Material 3 `Typography`. The default is `MaterialTheme.typography` (the platform default),
so a product that does nothing gets Roboto.

```kotlin
private val Brand = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val TrackerTypography = Typography(
    headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = Brand),
    bodyLarge = TextStyle(fontFamily = Brand, fontSize = 16.sp, lineHeight = 24.sp),
    // …override only the styles you actually change
)
```

Font resources live in **your** `res/font`. The Base ships no font.

---

## Shapes

```kotlin
val TrackerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
```

`ProductBaseShapes` (6/10/14/20/28 dp) is slightly softer than Material's defaults. Components read
`MaterialTheme.shapes`, so overriding here re-shapes cards, dialogs and badges together.

---

## Spacing

Material 3 defines no spacing tokens, so the Base adds a small scale rather than letting magic
`dp` values spread through screens.

```kotlin
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
) {
    val minTouchTarget: Dp = 48.dp     // Android's accessibility minimum; not overridable
}
```

Use it in product screens:

```kotlin
Column(modifier = Modifier.padding(AppTheme.spacing.md)) { … }
```

Pass a denser or airier scale if your design calls for one: `spacing = Spacing(sm = 6.dp, md = 12.dp)`.

---

## Semantic colors

The two meanings Material 3 has no role for, kept deliberately small:

```kotlin
@Immutable
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
)
```

```kotlin
Text(
    text = stringResource(R.string.saved),
    color = AppTheme.semanticColors.success,
)
```

Override per light/dark:

```kotlin
ProductBaseTheme(
    lightSemanticColors = SemanticColors(
        success = Color(0xFF1B6C3A), onSuccess = Color.White,
        warning = Color(0xFF8A5300), onWarning = Color.White,
    ),
    darkSemanticColors = SemanticColors(/* … */),
) { … }
```

**Resist growing this set.** A colour belongs here only when several unrelated screens need the
same *meaning* — not when one screen wants a hue. A one-screen colour is a product constant.
Needing a third semantic role across two real products is a class **C** report, not a local edit.

---

## Dynamic color

```kotlin
ProductBaseTheme(dynamicColor = true) { … }
```

- **Android 12+ (API 31):** the scheme comes from the user's wallpaper and **your `lightColors` /
  `darkColors` are ignored**. That is what Material You means.
- **Below API 31:** the flag has no effect; your schemes are used.
- `dynamicColor = false` always uses your schemes. A brand-critical app should pass `false` — or
  let the user choose, which is what the settings row does.

Whether it is on is a *user* preference in this Base: `AppPreferences.dynamicColorEnabled`
(default `true`), rendered by `SettingsRow.DynamicColor`, which disables itself with an
explanation below Android 12 rather than disappearing.

Semantic colours, spacing and shapes are **never** replaced by dynamic colour — only the M3
`ColorScheme` is. Check your success/warning colours against a few wallpapers if you ship it on.

---

## Theme mode

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }     // core:common
```

Persisted by `AppPreferences.themeMode` (default `SYSTEM`), rendered by `SettingsRow.ThemePicker`,
applied by `ProductBaseTheme`. Three modules, no fourth abstraction.

Read it **before the first frame** and render nothing until it is loaded, or a user who chose dark
sees a flash of light on every cold start:

```kotlin
ProductBaseTheme(themeMode = uiState.themeMode, dynamicColor = uiState.dynamicColorEnabled) {
    if (uiState.isLoading) return@ProductBaseTheme
    AppNavHost()
}
```

For the system bars, also set an XML theme (`android:Theme.Material.Light.NoActionBar` or a
DayNight parent) on your Activity — Compose theming does not cover the window background before
the first frame.

---

## Components and text

Every component in `core:designsystem` takes **already-resolved, localised strings**. There is no
`stringResource` inside them, and no component holds copy.

```kotlin
AppTopBar(title = stringResource(R.string.home))                    // yes
SettingsItem(title = stringResource(R.string.notifications), …)     // yes
```

The only strings the Base owns are mechanism labels in the feature modules, prefixed so they never
collide: `pb_onboarding_*` (Skip / Next / Get started / page indicator), `pb_settings_*` (Light /
Dark / Follow system / Dynamic colour), `pb_premium_*` (Upgrade / Restore purchase / billing
messages). To change any of them, **override the string in your own `res/values`** — same name,
your value. That is a supported customisation, not a hack.

---

## What not to do

| Don't | Do instead |
|---|---|
| Edit `Color.kt` in `core:designsystem` | Pass `lightColors` / `darkColors` |
| Add your component to `core:designsystem` | Keep it in your product until a second product needs it (then report class **C**) |
| Hardcode `16.dp` across screens | `AppTheme.spacing.md` |
| Read a top-level `val` colour | `MaterialTheme.colorScheme.*` |
| Put a string resource inside a shared component | Resolve it at the call site |
| Wrap `ProductBaseTheme` in your own `MyAppTheme` that hardcodes everything | Call it directly with your parameters; one theme entry point, not two |
