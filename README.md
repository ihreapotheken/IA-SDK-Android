# Android IASDK documentation

**IASDK** is Android SDK that helps integrate IhreApotheken into your app by providing
plug-and-play UI and communication with backend services.

# Latest version

Latest version of IA SDK: 

![GitHub Release](https://img.shields.io/github/v/release/ihreapotheken/IA-SDK-Android?style=for-the-badge&label=STABLE&color=green)


## Requirements

### Android SDK
- **Minimum SDK**: `27` (Android 8.1)
- **Compile SDK**: `36`
- **Target SDK**: `36`

> [!NOTE]
> The `cardlink` module (electronic health card / NFC) requires **Android 11 (API 30)** at runtime and declares
> `minSdk 30`. Apps with a lower `minSdk` can still include it, see [CardLink requirements](#cardlink-requirements).
> All other modules work from API 27.

### Kotlin & Java
- **Kotlin**: `2.1.10` or higher
- **Java**: `17` (source and target compatibility)

### Jetpack Compose
The SDK is built with the following Compose artifacts. They are the **minimum** versions your project resolves to;
if your app uses newer versions Gradle picks the newer ones.
- **Compose UI**: `1.9.4`
- **Compose Runtime**: `1.9.4`
- **Material3**: `1.4.0`
- **Activity Compose**: `1.10.0`
- **Navigation Compose**: `2.9.7`

> [!NOTE]
> If you manage Compose through the Compose BOM, use a BOM that maps to Compose UI `1.9.4` or newer.

### Build Tools
- **Android Gradle Plugin (AGP)**: `8.9.1` or higher (required for `compileSdk 36`; the SDK itself is built with `8.10.1`)
- **Gradle**: `8.11.1` or higher

### Integration Limitations
- The SDK requires Jetpack Compose. View-based integration is supported via `IaSdkView`, but the underlying implementation uses Compose.
- Kotlin version must be compatible with Compose compiler. The SDK is tested with Kotlin `2.1.10`.
- Projects with `minSdk` below 27 cannot integrate the SDK. CardLink features are only available on Android 11+ devices.

## Running the Example Apps

The example apps (`example`, `example2`, `example3`) require an API access key to run.

### Local Development

Create a `.env` file in the project root with your API key:

```
APPSDK_ACCESS_KEY=your_api_key_here
```

> [!IMPORTANT]
> The `.env` file is git-ignored and should never be committed. Each developer needs to create their own.

### CI/CD (GitHub Actions)

For CI/CD pipelines, the build will automatically use the `APPSDK_ACCESS_KEY` environment variable when the `.env` file is not present.

Add the secret to your GitHub repository:
1. Go to repository Settings → Secrets and variables → Actions
2. Create a new repository secret named `APPSDK_ACCESS_KEY`

The example workflow passes the secret as an environment variable:

```yaml
- name: Build with Gradle
  env:
    APPSDK_ACCESS_KEY: ${{ secrets.APPSDK_ACCESS_KEY }}
  run: ./gradlew build
```

> [!NOTE]
> If neither the `.env` file nor the environment variable is found, the build will fail with an error message.

## Quick Start

### 1. Adding repositories

The SDK is published to GitHub Packages. It also needs the Mapbox repository (for the pharmacy map) and, only if you
use the `cardlink` module, the Link4Health repository. Add the following to the `repositories` block in your
`settings.gradle.kts` file:

```kotlin
repositories {
    // Mapbox Maps, used by the pharmacy finder (required for every integration)
    maven {
        url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    }

    // IA SDK artifacts
    maven {
        name = "IA SDK repo"
        url = uri("https://maven.pkg.github.com/ihreapotheken/IA-SDK-Android")
        credentials {
            username = System.getenv("GITHUB_USERNAME") ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: ""
        }
    }

    // Link4Health CardLink SDK, only needed when you add the cardlink module
    maven {
        name = "Link4Health"
        url = uri("https://nexus.link4.health/repository/link4health-releases/")
        mavenContent { releasesOnly() }
    }
}
```

To generate your access token, go to [GitHub settings page](https://github.com/settings/tokens)

#### Optional
Instead of `System` environment variables you can use `local.properties` to store your credentials.
Find the `local.properties` file in the root of your project and add the following:

```properties
github.username=<your user name>
github.token=<your github access token>
```

and then in `settings.gradle.kts` file fetch those values with following code
(you can add this to top of file):

```kotlin
val localProperties = java.util.Properties()
val localPropertiesFile = File(rootDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val githubUsername = localProperties.getProperty("github.username") ?: ""
val githubToken = localProperties.getProperty("github.token") ?: ""
```

and then add following to `repositories` block in your `settings.gradle.kts` file:

```kotlin
repositories {
    maven {
        url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    }

    maven {
        name = "IA SDK repo"
        url = uri("https://maven.pkg.github.com/ihreapotheken/IA-SDK-Android")
        credentials {
            username = githubUsername
            password = githubToken
        }
    }

    // only needed when you add the cardlink module
    maven {
        name = "Link4Health"
        url = uri("https://nexus.link4.health/repository/link4health-releases/")
        mavenContent { releasesOnly() }
    }
}
```

### 2. Setting up dependencies

Add dependencies in your `build.gradle.kts` (app module) file:

```kotlin
dependencies {
    // Required: base functionality, navigation, prerequisite flow, appointments, lab tests, scanner
    implementation("de.ihreapotheken.sdk:integrations:<version>")

    // Only the feature UIs you need
    implementation("de.ihreapotheken.sdk:otc:<version>")        // product search and product details
    implementation("de.ihreapotheken.sdk:ordering:<version>")   // cart, checkout, orders, prescription transfer
    implementation("de.ihreapotheken.sdk:rx:<version>")         // prescription photo / QR redemption
    implementation("de.ihreapotheken.sdk:pharmacy:<version>")   // selected pharmacy screen
    implementation("de.ihreapotheken.sdk:apofinder:<version>")  // pharmacy finder as a standalone screen
    implementation("de.ihreapotheken.sdk:cardlink:<version>")   // electronic health card (NFC), see CARDLINK.md
}
```
> [!NOTE]
> Replace `<version>` with the SDK version, for example `2.9.0`. Use the same version for every module.

> [!IMPORTANT]
> `integrations` is required; it provides the base functionality of the SDK.
> Add the other dependencies only for the features you want to use (otc, ordering, rx, pharmacy, apofinder, cardlink).

#### CardLink requirements

The `cardlink` module declares `minSdk 30`. If your app's `minSdk` is lower (the SDK itself supports 27), tell the
manifest merger to accept it. CardLink checks the Android version at runtime and reports
`failedToInitializeCardlink()` on devices below Android 11 instead of crashing.

```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-sdk tools:overrideLibrary="de.ihreapotheken.sdk.cardlink, de.link4health.cardlink.sdk, de.link4health.egk" />
    ...
</manifest>
```

The Link4Health SDK bundles BouncyCastle, which ships a duplicate `META-INF` file. Exclude it in your app module:

```kotlin
// build.gradle.kts (app module)
android {
    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}
```

### 3. SDK initialization

Before using any functionality from the SDK, it must be initialized in `Application` class.

1. Create (or extend) your `Application` class
2. Register the required SDK modules
3. Initialize SDK with your API key and configuration

```kotlin
class MyApp : Application(), SdkEventListener {
    override fun onCreate() {
        super.onCreate()

        // Register the modules your app will use
        IaSdk.register(
            IntegrationsModule,
            OtcModule,
            OrderingModule,
            PharmacyModule,
            RxModule,
        )

        // Initialize SDK
        IaSdk.init(
            context = applicationContext,
            environmentType = EnvironmentType.PROD, // or STAGING / DEV
            apiKey = "your_api_key",
            clientId = "your_client_id",
            configuration = IaSdkConfiguration(
                shouldFetchThemeFromRemote = true,
                prerequisiteFlowConfiguration = PrerequisiteFlowConfiguration(
                    shouldRunOnboarding = true,
                    shouldRunLegal = true
                )
            ),
            sdkEventListener = this
        )
    }

    override fun onSdkEvent(event: SdkEvent) {
        when (event) {
            SdkEvent.InitStatus.InitializationCompleted -> {
                Log.d("App", "SDK initialized successfully")
            }
            is SdkEvent.InitError -> {
                // ApiKeyNotValid, ApiKeyNotProvided, ClientIdNotProvided, NotInitialized, Generic
                Log.e("App", "SDK initialization error: ${event.message}")
            }
            else -> { /* handle other events */
            }
        }
    }
}
```

> [!IMPORTANT]
> Module registration is required before calling `init()`. `IntegrationsModule` is always required. Optional
> modules: `OtcModule`, `OrderingModule`, `PharmacyModule`, `RxModule`, `ApofinderModule`, `CardlinkModule`.
> Mandatory modules (appointments, lab tests, scanner, debug viewer, data) are added automatically.

> [!NOTE]
> `SdkEventListener` doesn't necessarily need to be set at the startup, it can also be set later with `IaSdk.setEventListener(...)`.
> The full list of errors and how they reach your app is in [ERRORS.md](ERRORS.md).

#### Configuration options

`IaSdkConfiguration` is a data class with defaults for every parameter:

| Parameter | Default | Description |
|---|---|---|
| `shouldFetchThemeFromRemote` | `false` | Load the pharmacy theme from the backend |
| `prerequisiteFlowConfiguration` | `PrerequisiteFlowConfiguration()` | `shouldRunOnboarding` / `shouldRunLegal` switches for the prerequisite flow |
| `channelId` | `null` | Channel identifier passed to the backend |
| `shouldShowIndicator` | `false` | Show the SDK's own loading indicator |
| `uiConfiguration` | `IAUIConfiguration()` | Header, button and illustration settings. `IAUIConfiguration(shouldShowMascotIllustrations = false)` hides the Pharmi mascot (white-label hosts) |
| `isSaveCardEnabled` | `false` | Allow users to save their health card in CardLink |
| `analyticsEnabled` | `false` | Emit analytics events to the `AnalyticsListener` |
| `shouldUseApiKeyForMobileSettings` | `true` | Send the API key when loading mobile settings |
| `playStoreId` | `null` | Your Play Store application id. When set, the force-update dialog gets an "Aktualisieren" button |
| `hapticFeedbackEnabled` | `true` | Master switch for SDK haptic feedback, see [Haptic feedback](#haptic-feedback) |
| `isPickupLockerAndCourierShippingEnabled` | `false` | Enable pickup-locker and courier delivery options and the "Widerruf" footer link |

#### Monitoring Initialization State

You can observe the SDK initialization state using the `initState` flow:

```kotlin
lifecycleScope.launch {
    IaSdk.initState.collect { state ->
        when (state) {
            InitState.UNINITIALIZED -> { /* SDK not initialized */
            }
            InitState.INITIALIZING -> { /* Show loading indicator */
            }
            InitState.INITIALIZED -> {
                // SDK is ready, configure additional settings
                IaSdk.setHostUiConfig(HostUiConfig(showDataProcessing = true))
            }
            InitState.ERROR -> { /* initialization failed, see SdkEventListener for details */
            }
        }
    }
}
```

#### Setting Up Listeners

Configure listeners after initialization to receive SDK events:

```kotlin
// Cart listener - receive updates when cart changes
IaSdk.ordering.setCartListener(object : CartListener {
    override fun onCartChanged(totalProducts: Int, totalPrescription: Int, totalItems: Int) {
        updateCartBadge(totalItems)
    }
})

// Checkout listener - receive notifications when checkout completes
IaSdk.ordering.setCheckoutListener(object : CheckoutListener {
    override fun onCheckoutCompleted(hostOrderId: String, sdkOrderId: String) {
        Log.d("App", "Checkout completed: $hostOrderId")
    }
})
```

### 4. SDK Integration Modes

The SDK supports four integration modes:

#### A. Embedded Mode (Composable)

Embed SDK screens directly in your Compose navigation. `IaSdkScreen` takes an entry point and optional callbacks:

```kotlin
@Composable
fun <R : IaSdkResult?> IaSdkScreen(
    sdkEntryPoint: SdkEntryPoint<R>,
    presentationMode: PresentationMode = PresentationMode.EMBEDDED,
    onNavigateToTarget: (suspend (SdkNavigationTarget) -> Boolean)? = null,
    onResult: ((R & Any) -> Unit)? = null,
    onFinish: () -> Unit = {},
    onPrerequisiteResult: ((PrerequisiteResult) -> Boolean)? = null,
)
```

`onNavigateToTarget` is called whenever the SDK is about to navigate to one of its first-level screens
(`SdkNavigationTarget`: `IaScreen.StartScreen`, `CartScreen`, `SearchScreen`, `PharmacyScreen`, `ApofinderScreen`,
`RedeemPrescription`, `ThankYouScreen`, `ThankYouScreenCompleted`). Return `true` when your app handled the
navigation, `false` to let the SDK navigate internally.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(navController, startDestination = HostAppRoute.MainScreen) {
                // Your app screens
                composable<HostAppRoute.MainScreen> {
                    HomeScreen()
                }

                // SDK screens
                composable<HostAppRoute.SdkSearchScreen> {
                    IaSdkScreen(
                        sdkEntryPoint = IaScreen.SearchScreen,
                        onNavigateToTarget = { target ->
                            // Handle navigation to other SDK screens
                            when (target) {
                                IaScreen.CartScreen -> {
                                    navController.navigate(HostAppRoute.SdkCartScreen)
                                    true // Navigation handled
                                }
                                else -> false // Let SDK handle it
                            }
                        }
                    )
                }

                composable<HostAppRoute.SdkCartScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.CartScreen)
                }

                composable<HostAppRoute.SdkPharmacyScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.PharmacyScreen)
                }

                composable<HostAppRoute.SdkStartScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.StartScreen)
                }
            }

            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    onBottomTabSelect(selectedTab = it, navController = navController)
                }
            )
        }
    }
}
```

#### B. Full Flow Mode (New Activity)

Launch SDK screens in a separate activity:

```kotlin
Button(
    onClick = {
        IaSdkActivity.start(
            context = context,
            view = IaScreen.StartScreen
        )
    }
) {
    Text("Open SDK")
}

// With custom back action handling and navigation interception
IaSdkActivity.start(
    context = context,
    view = IaScreen.CartScreen,
    onBackAction = {
        // Custom logic before closing
        // Return true to close activity, false to prevent closing
        true
    },
    onNavigateToTarget = { target ->
        // Same contract as IaSdkScreen: true = handled by host, false = SDK navigates
        false
    },
)

// Close every SDK activity the host has opened (e.g. on logout)
IaSdkActivity.finishAllActivities()
```

#### C. View-Based Integration

For traditional View-based apps, create SDK views. The context must belong to an `Activity`:

```kotlin
class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return IaSdkView.createView(
            context = requireActivity(),
            sdkEntryPoint = IaScreen.SearchScreen,
            onNavigateToTarget = null, // optional, same contract as IaSdkScreen
        )
    }
}
```

#### D. SDK Components (Composable)

In addition to full SDK screens, the SDK exposes individual UI components that you can place
anywhere in your Compose layouts. These are standalone composables that provide specific SDK
functionality without rendering an entire screen.

> [!IMPORTANT]
> Before rendering any SDK component, the SDK automatically validates that all prerequisites
> are fulfilled (onboarding, pharmacy selection, legal agreements — depending on your
> `PrerequisiteFlowConfiguration`). If the prerequisites have not been completed yet,
> the SDK will launch the prerequisite flow. If the user completes it, the component renders
> normally. If the user dismisses or cancels the prerequisite flow, an error state with a
> retry button is shown in place of the component. This behavior is handled internally —
> no additional setup is required from the host app.

##### IaProductGrid

Displays a product grid with a title and a "load more" action. Can be placed inside any host-app screen.

```kotlin
@Composable
fun IaProductGrid(
    productType: ProductType,
    bottomPadding: Dp = 0.dp,
    showLoading: Boolean = true,
)
```

**Parameters:**
- `productType` — determines which products are loaded and displayed. Available types:
  - `ProductType.CurrentOffers` — current offers
  - `ProductType.ProductOfTheMonths` — products of the month
  - `ProductType.ProductRecommendations(pzn: String)` — recommendations based on a product PZN
  - `ProductType.CustomersAlsoBought(pzn: String)` — "customers also bought" based on a product PZN
  - `ProductType.Custom(title: String, pzns: List<String>)` — custom list of products by PZN with a custom title
- `bottomPadding` — extra bottom padding applied to grid content. Defaults to `0.dp`
- `showLoading` — when `true`, displays a loading indicator while products are being fetched. Defaults to `true`

**Example:**
```kotlin
// Display current offers
IaProductGrid(
    productType = ProductType.CurrentOffers,
    showLoading = false,
)

// Display a custom product list
IaProductGrid(
    productType = ProductType.Custom(
        title = "Custom Products",
        pzns = listOf("06680881", "04460328", "02588163", "00067286")
    ),
    showLoading = true,
)
```

##### IaCartButton

A floating cart button that displays the current cart item count and navigates to the cart screen.

```kotlin
@Composable
fun IaCartButton(
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
)
```

**Parameters:**
- `modifier` — modifier applied to the cart button
- `onClick` — optional callback invoked when the button is tapped. When provided, the SDK's default navigation is suppressed. If `null`, the SDK opens the cart in a new Activity

**Example:**
```kotlin
// Let the SDK handle cart navigation
IaCartButton(modifier = Modifier.size(80.dp))

// Handle navigation yourself
IaCartButton(modifier = Modifier.size(80.dp)) {
    navController.navigate(HostAppRoute.SdkCartScreen)
}
```

### 5. Available SDK Entry Points

The SDK provides the following entry points via `IaScreen` object:

- `StartScreen` - Dashboard/home screen
- `SearchScreen` - Product search functionality
- `CartScreen` - Shopping cart and checkout
- `PharmacyScreen` - Selected pharmacy details
- `ApofinderScreen` - Apofinder (pharmacy selection) screen. Emits a typed `IaSdkResult.PharmacySelected` result. See [Standalone Apofinder Screen](#standalone-apofinder-screen).
- `RedeemPrescription` - Redeem Prescription screen that lists the available redemption methods (CardLink / NFC, prescription photo, QR code). See [Redeem Prescription Screen](#redeem-prescription-screen).
- `PrerequisiteFlow` - Onboarding, legal and pharmacy selection flow
- `TransferPrescriptionsScreen` - For internal use only. To transfer prescriptions use `IaSdk.ordering.transferPrescriptions(...)`, see [Transfer Prescriptions](#transfer-prescriptions)

`IaScreen.ThankYouScreen` and `IaScreen.ThankYouScreenCompleted` are **navigation targets only** (they are reported to
`onNavigateToTarget` after a successful checkout and when the user closes the thank-you screen); they cannot be used
as entry points.

#### Entry-point result types

`SdkEntryPoint<R>` carries the result type produced by that entry point at the type level:

- **Fire-and-forget entry points** declare `SdkEntryPoint<Nothing?>` and produce no result (e.g., `StartScreen`, `CartScreen`, `RedeemPrescription`).
- **Result-producing entry points** declare a concrete `IaSdkResult` subtype (e.g., `ApofinderScreen : SdkEntryPoint<IaSdkResult.PharmacySelected>`).

When you pass a result-producing entry point to `IaSdkScreen` or `IaSdkActivity.start`, the `onResult` callback is typed accordingly — no runtime `when`-switch is needed.

### 6. Advanced Features

#### Setting User Data Dynamically

```kotlin
// After user logs in
IaSdk.setUserData(
    IaUserData(
        salutation = "Herr",
        firstName = "John",
        lastName = "Doe",
        email = "john@example.com",
        countryCode = 49,                         // optional
        phoneNumberWithoutCountryCode = "1234567" // optional
    )
)

// Clear user data on logout
IaSdk.setUserData(null)
```

> [!NOTE]
> `IaSdk.setUserData(GuestUser)` is deprecated since 2.9.0 and will be removed in a future release. Use `IaUserData`.

#### Delivery and billing addresses

The host app can provide one delivery and one billing address that the SDK offers during checkout. Each call
overwrites the previous host-provided address; `null` removes it. The user's own saved addresses are not affected.
The address arrives unconfirmed: the user reviews it once in the address form before it can be used for an order.

```kotlin
val address = IaUserAddress(
    salutation = "Herr",
    firstName = "John",
    lastName = "Doe",
    street = "Musterstraße",
    houseNumber = "1",
    zipCode = "10115",
    city = "Berlin",
    phoneNumber = "+491234567",  // optional
    countryCode = 49,            // optional
    company = null,              // optional
)

IaSdk.setUserDeliveryAddress(address)
IaSdk.setUserBillingAddress(address)

// Remove the host-provided addresses
IaSdk.setUserDeliveryAddress(null)
IaSdk.setUserBillingAddress(null)
```

#### Customizing UI Configuration

```kotlin
IaSdk.setHostUiConfig(
    HostUiConfig(
        showDataProcessing = true
    )
)
```

#### Haptic feedback

The SDK plays haptic feedback on key interactions. It is enabled by default and can be switched off at init time
(`IaSdkConfiguration(hapticFeedbackEnabled = false)`) or at runtime, for example to mirror an in-app preference.
Haptics play only when this switch and the device's system setting are both enabled.

```kotlin
IaSdk.setHapticFeedbackEnabled(userPrefs.hapticsOn)
```

#### Transfer Prescriptions

Prescriptions captured by your app (images, PDFs or e-prescription codes) can be handed to the SDK, which adds them to
the cart. The call needs an `Activity` context.

```kotlin
val request = TransferPrescriptionRequest(
    images = listOf(ImagePrescription(data = prescriptionImageBytes)),
    pdfs = listOf(PdfPrescription(data = prescriptionPdfBytes)),
    codes = null,     // optional: e-prescription codes
    orderId = null,   // optional
)

// Using PresentationMode.FULL_FLOW (opens in new activity), default listener
IaSdk.ordering.transferPrescriptions(
    context = activity,
    transferPrescriptionRequest = request,
    presentationMode = PresentationMode.FULL_FLOW,
)

// Using PresentationMode.OVERLAY with custom listener
IaSdk.ordering.transferPrescriptions(
    context = activity,
    transferPrescriptionRequest = request,
    presentationMode = PresentationMode.OVERLAY,
    transferPrescriptionListener = object : TransferPrescriptionListener {
        override fun onTransferPrescriptionEvent(event: TransferPrescriptionEvent): HandlingDecision {
            when (event) {
                is TransferPrescriptionEvent.Failed -> {
                    Log.e("TransferPrescriptions", "Transfer prescription failed: ${event.errorMessage}")
                }
                TransferPrescriptionEvent.Loading -> {}

                is TransferPrescriptionEvent.Success -> {
                    if (event.navigateToCart) {
                        // Navigate to cart
                    }
                }
            }
            // Host app handled navigation
            return HandlingDecision.HANDLED
        }
    }
)
```

`transferPrescriptions` returns `false` when the SDK is not initialized or the ordering module is not registered.

#### Intercepting scanned prescriptions

With `RxModule` registered, the SDK's scanner adds scanned e-prescription codes and prescription photos to the cart.
If your app needs to see every scan first (for example to upload it to your own backend), register a
`RedeemPrescriptionListener`. It is called on the main thread **before** any cart work, and the scanner stays blocked
until your decision is returned, so return promptly and move backend work to a background dispatcher.

```kotlin
IaSdk.rx.setRedeemPrescriptionListener(object : RedeemPrescriptionListener {
    override suspend fun onRedeemPrescriptionEvent(event: RedeemPrescriptionEvent): RedeemPrescriptionDecision {
        return when (event) {
            is RedeemPrescriptionEvent.EPrescriptionScanned -> {
                // event.rawCode: raw QR payload, event.urls: e-prescription task URLs
                withContext(Dispatchers.IO) { uploadToOwnBackend(event.rawCode, event.urls) }
                RedeemPrescriptionDecision.Handled
            }
            is RedeemPrescriptionEvent.PrescriptionPhotoScanned -> {
                // event.imageData: raw image bytes
                RedeemPrescriptionDecision.PerformDefault
            }
        }
    }
})

// Remove the listener; the SDK falls back to its default behaviour (add to cart)
IaSdk.rx.setRedeemPrescriptionListener(null)
```

Decisions:
- `RedeemPrescriptionDecision.PerformDefault` — the SDK continues as usual and adds the prescription to the cart (requires `OrderingModule`).
- `RedeemPrescriptionDecision.Handled` — your app took over; the SDK does nothing with the scan and the scanner resumes.
- `RedeemPrescriptionDecision.Error(message)` — the SDK shows `message` in the scanner's error banner.

Java and bridge hosts (NativeScript, Flutter platform channels) that cannot implement a `suspend` function extend
`CallbackRedeemPrescriptionListener` instead and call the `decisionHandler` exactly once:

```kotlin
IaSdk.rx.setRedeemPrescriptionListener(object : CallbackRedeemPrescriptionListener() {
    override fun onRedeemPrescriptionEvent(
        event: RedeemPrescriptionEvent,
        decisionHandler: (RedeemPrescriptionDecision) -> Unit,
    ) {
        decisionHandler(RedeemPrescriptionDecision.PerformDefault)
    }
})
```

#### Pharmacy selection

```kotlin

// Select a pharmacy programmatically and observe the outcome
IaSdk.pharmacy.setPharmacyId("2163", PharmacyConfigListener { result ->
    when (result) {
        is PharmacyConfigResult.Success -> Log.d("App", "Pharmacy set: ${result.pharmacyId}")
        is PharmacyConfigResult.ValidationFailed -> Log.e("App", "Invalid pharmacy: ${result.reason}")
        PharmacyConfigResult.NotInitialized -> Log.e("App", "SDK not initialized")
    }
})

// Read or clear the current pharmacy (suspend functions)
lifecycleScope.launch {
    val pharmacyId: String = IaSdk.pharmacy.getPharmacyId() // empty string when none is set
    IaSdk.pharmacy.clearPharmacyId()                         // also clears the cart
}
```

#### Standalone Apofinder Screen

Apofinder (pharmacy selection) can be embedded as a **standalone screen** — for example as a dedicated "Find pharmacy" tab in your bottom navigation, or as a destination launched from your own UI. The selected pharmacy is delivered to the host via the typed `onResult` callback.

**Requirements:**
- Register `ApofinderModule` before `IaSdk.init(...)`:
  ```kotlin
  IaSdk.register(IntegrationsModule, ApofinderModule, /* other modules */)
  ```

**Embedded (Compose):**

```kotlin
IaSdkScreen(
    sdkEntryPoint = IaScreen.ApofinderScreen,
    onResult = { result ->
        // result is typed as IaSdkResult.PharmacySelected
        hostViewModel.onPharmacySelected(result.pharmacyId)
    },
    onFinish = { /* user dismissed the flow */ },
)
```

> [!NOTE]
> When `onResult` is invoked from `IaSdkScreen`, the SDK does **not** auto-close or navigate.
> The host is responsible for what comes next (close the host screen, switch tabs, etc.).
> The selected pharmacy is already persisted by the SDK before the callback fires.

**Full-flow Activity:**

```kotlin
IaSdkActivity.start(
    context = context,
    view = IaScreen.ApofinderScreen,
    onResult = { result ->
        Toast.makeText(context, "Selected: ${result.pharmacyId}", Toast.LENGTH_SHORT).show()
    },
)
```

> [!NOTE]
> When launched via `IaSdkActivity.start`, the activity finishes automatically after `onResult` is invoked.

#### Redeem Prescription Screen

`IaScreen.RedeemPrescription` exposes the prescription-redemption hub as a standalone screen. It renders up to three sections, each gated on what your app has registered:

- **Electronic health card (CardLink / NFC)** — visible only when `CardlinkModule` is registered, the device runs Android 11+ and supports NFC, and the currently selected pharmacy has both CardLink services enabled (PCASE36 and PCASE38).
- **Prescription photo** — visible only when `RxModule` is registered. Opens the prescription camera.
- **QR code (e-prescription)** — visible only when `RxModule` is registered. Opens the QR scanner.

If none of the sections are available, the screen renders informational messages instead.

**Requirements:**
- Register the modules for the redemption methods you want to expose (`RxModule` for photo/QR, `CardlinkModule` for NFC).
- The standard prerequisite chain (onboarding, legal, pharmacy selection) still runs before the screen opens if not already completed.

**Embedded (Compose):**

```kotlin
IaSdkScreen(
    sdkEntryPoint = IaScreen.RedeemPrescription,
    onFinish = { navController.popBackStack() },
)
```

**Full-flow Activity:**

```kotlin
IaSdkActivity.start(
    context = context,
    view = IaScreen.RedeemPrescription,
)
```

`RedeemPrescription` is a fire-and-forget entry point (`SdkEntryPoint<Nothing?>`) — it does not deliver a typed result. Use `onFinish` (embedded) or `onBackAction` (activity) to react to the user closing the flow. To be notified of every scan before the SDK processes it, see [Intercepting scanned prescriptions](#intercepting-scanned-prescriptions).

#### Prerequisite Flow Callback

`IaSdkScreen` exposes an optional `onPrerequisiteResult` callback so the host can react to the terminal state of the prerequisite flow (onboarding, legal, pharmacy selection). It fires once the flow settles, **including** when prerequisites were already completed in a previous session and no UI ran this time.

```kotlin
enum class PrerequisiteResult {
    COMPLETED,  // prerequisites are satisfied (just completed or already done)
    CANCELLED,  // user backed out or explicitly cancelled the flow
}
```

**Registering the callback:**

```kotlin
IaSdkScreen(
    sdkEntryPoint = IaScreen.StartScreen,
    presentationMode = PresentationMode.FULL_FLOW,
    onFinish = { sdkOpen = false },
    onPrerequisiteResult = { result ->
        when (result) {
            PrerequisiteResult.COMPLETED -> false       // let SDK render its content
            PrerequisiteResult.CANCELLED -> {
                sdkOpen = false                          // host dismisses the SDK surface
                true                                     // tell SDK to suppress its default UI
            }
        }
    },
)
```

**Return-value semantics (`handled` / `not handled`):**

The `Boolean` returned by the callback is honored **only for `CANCELLED`**:

| Result      | Return `true` (handled)                                                                       | Return `false` (not handled)                                                          |
|-------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `COMPLETED` | Ignored — SDK always proceeds to render its content.                                          | Same — SDK renders content.                                                           |
| `CANCELLED` | SDK renders an empty composable in place of its retry view — host owns the next UX step.      | SDK falls back to its built-in retry screen with an activation button.                |

**Auto-close in full-flow mode:**
When the SDK is launched via `IaSdkActivity.start(...)`, the activity registers a default cancel handler that returns `true` and calls `finish()` — so the SDK activity closes automatically if the user cancels the prerequisite flow.

**Checking prerequisite state imperatively:**

`IaSdk` exposes a suspend snapshot of the prerequisite state. Use it to gate whether you should mount an `IaSdkScreen` at all (e.g., show your own pre-onboarding UI first):

```kotlin
suspend fun isPrerequisiteFlowCompleted(): Boolean
```

```kotlin
// In a coroutine scope (e.g., lifecycleScope)
lifecycleScope.launch {
    if (IaSdk.isPrerequisiteFlowCompleted()) {
        navController.navigate(HostAppRoute.SdkStartScreen)
    } else {
        showOnboardingTeaser()
    }
}
```

Returns `true` if onboarding, legal, and pharmacy selection are all satisfied; `false` otherwise (including when the SDK is not initialized).

#### Transferring SDK v1 User Data

If your app previously used SDK v1 and you want to preserve user data (pharmacy selection, guest info, search history) after upgrading to v2, call `transferSDKv1UserData` **before** `init()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        IaSdk.register(IntegrationsModule, /* your modules */)

        // Transfer v1 user data before init (runs once, no-op if no v1 data)
        IaSdk.transferSDKv1UserData(this)

        // Initialize SDK
        IaSdk.init(
            context = applicationContext,
            // ... your configuration
        )
    }
}
```

> [!NOTE]
> This method is optional — only needed when migrating from SDK v1 to v2.
> It runs only once. Subsequent calls are no-ops after a successful transfer.
> No completion callback is needed — `init()` synchronizes with the transfer internally.

**Data transferred from v1:**
- Device ID
- Selected pharmacy and pharmacy history
- Guest user information (name, email, phone)
- Product search history
- Apofinder search history

#### Managing Cart

```kotlin
// Clear cart contents
IaSdk.ordering.deleteCart()
```

#### Clearing All SDK Data

```kotlin
// Clear all stored SDK data (cart, preferences, cache)
// on Logout
if (IaSdk.clearAllData()) {
    Log.d("App", "All SDK data cleared")
}
```

#### Setting Custom Listeners

```kotlin
// Impressum/Legal click listener
IaSdk.setImpressumClickListener(object : SdkImpressumListener {
    override fun onImpressumClick() {
        // Handle impressum click
    }
})

// Data processing click listener
IaSdk.setShowDataProcessingClickListener(object : SdkShowDataProcessingListener {
    override fun onShowDataProcessingClick() {
        // Handle data processing click
    }
})
```

#### Analytics

With `IaSdkConfiguration(analyticsEnabled = true)` the SDK emits analytics events (screen views, user actions,
e-commerce and lifecycle events) to a listener you provide. Consent defaults to `GRANTED`;

```kotlin
IaSdk.setAnalyticsListener(AnalyticsListener { event ->
    when (event) {
        is AnalyticsEvent.ScreenView -> analytics.logScreenView(event.screenName, event.featureName)
        is AnalyticsEvent.UserAction -> analytics.logEvent(event.actionName, event.properties)
        else -> { /* ScreenExit, Lifecycle, Ecommerce, Engagement, Derived */ }
    }
})

IaSdk.setAnalyticsConsent(AnalyticsConsent.GRANTED) // or DENIED / UNKNOWN
```

### 7. Bottom Tab Navigation Example

For apps with bottom navigation, integrate SDK screens as tabs:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(navController, startDestination = HostAppRoute.MainScreen) {
                // Your app screens
                composable<HostAppRoute.MainScreen> {
                    HomeScreen()
                }

                // SDK screens
                composable<HostAppRoute.SdkSearchScreen> {
                    IaSdkScreen(
                        sdkEntryPoint = IaScreen.SearchScreen,
                        onNavigateToTarget = { target ->
                            // Handle navigation to other SDK screens
                            when (target) {
                                IaScreen.CartScreen -> {
                                    navController.navigate(HostAppRoute.SdkCartScreen)
                                    true // Navigation handled
                                }
                                else -> false // Let SDK handle it
                            }
                        }
                    )
                }

                composable<HostAppRoute.SdkCartScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.CartScreen)
                }

                composable<HostAppRoute.SdkPharmacyScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.PharmacyScreen)
                }

                composable<HostAppRoute.SdkStartScreen> {
                    IaSdkScreen(sdkEntryPoint = IaScreen.StartScreen)
                }
            }

            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    onBottomTabSelect(selectedTab = it, navController = navController)
                }
            )
        }
    }
}

private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
    when (selectedTab) {
        BottomTab.HOME -> navController.navigate(HostAppRoute.MainScreen) {
            manageBottomNavigationBackStack()
        }
        BottomTab.SEARCH -> navController.navigate(HostAppRoute.SdkSearchScreen) {
            manageBottomNavigationBackStack()
        }
        BottomTab.CART -> navController.navigate(HostAppRoute.SdkCartScreen) {
            manageBottomNavigationBackStack()
        }
        BottomTab.PHARMACY -> navController.navigate(HostAppRoute.SdkPharmacyScreen) {
            manageBottomNavigationBackStack()
        }
    }
}

private fun NavOptionsBuilder.manageBottomNavigationBackStack() {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
    restoreState = true
}
```

### 8. Presentation Modes

The SDK supports three presentation modes that control **navigation UI behavior** (such as whether a back button is displayed). Presentation mode does **not** affect screen size, layout, or visual rendering — it only determines which navigation elements are shown based on how your app handles screen switching.

- **`PresentationMode.EMBEDDED`** (default) — Use when each SDK first-level screen is placed as a **separate tab** in your bottom navigation. In this mode, the SDK **hides the back button** because bottom navigation tabs already allow the user to switch between screens.

- **`PresentationMode.FULL_FLOW`** — Use when you open SDK screens in a **new activity** or place all first-level SDK screens under a **single tab** in your bottom navigation. In this mode, the SDK **shows a back button** so the user can navigate between SDK screens within that flow.

- **`PresentationMode.OVERLAY`** — For short-lived flows (e.g., prescription transfer) displayed as a bottom sheet overlay on top of your current UI.

Choose the mode based on how your app's navigation is structured:

```kotlin
// Embedded - each SDK screen is its own tab in bottom navigation (no back button)
IaSdkScreen(
    sdkEntryPoint = IaScreen.SearchScreen,
    presentationMode = PresentationMode.EMBEDDED
)

// Full flow - SDK screens in a new activity or grouped under one tab (back button visible)
IaSdkActivity.start(context, IaScreen.StartScreen)

// Transfer prescription as overlay
IaSdk.ordering.transferPrescriptions(
    context = activity,
    transferPrescriptionRequest = TransferPrescriptionRequest(
        images = listOf(ImagePrescription(data = prescriptionImageBytes)),
    ),
    presentationMode = PresentationMode.OVERLAY,
    transferPrescriptionListener = object : TransferPrescriptionListener {
        override fun onTransferPrescriptionEvent(event: TransferPrescriptionEvent): HandlingDecision {
            // Navigate to cart or show success message
            return HandlingDecision.HANDLED
        }
    }
)
```

### 9. Key Components Summary

- **`IaSdk`** - Main SDK entry point for initialization and configuration
- **`IaSdk.pharmacy`** - Access to pharmacy-related functionality
- **`IaSdk.ordering`** - Access to cart, ordering, and prescription transfer
- **`IaSdk.rx`** - Access to prescription-redemption functionality (scan interception)
- **`IaSdkScreen`** - Composable for embedding SDK screens
- **`IaSdkActivity`** - Activity for launching SDK in full-flow mode
- **`IaSdkView`** - Factory for creating SDK views for traditional View hierarchies
- **`IaProductGrid`** - Composable for displaying a product grid within host-app screens
- **`IaCartButton`** - Composable floating cart button with item count
- **`ProductType`** - Sealed interface defining product grid content types
- **`PresentationMode`** - Enum defining how SDK screens are presented
- **`IaSdkConfiguration`** - Configuration options for SDK initialization
- **`IaUserData`** / **`IaUserAddress`** - User and address data the host can hand to the SDK
- **`InitState`** - Observable state for monitoring SDK initialization
- **`IaScreen`** - Object defining all available SDK screens
- **`SdkNavigationTarget`** - SDK screens reported to `onNavigateToTarget`
- **`IaSdkResult`** - Sealed interface of typed results emitted by result-producing entry points (e.g., `IaSdkResult.PharmacySelected` from `ApofinderScreen`)
- **`PrerequisiteResult`** - Enum delivered to `onPrerequisiteResult` (`COMPLETED` / `CANCELLED`)

### 10. API Reference

#### IaSdk Properties

- `initState: StateFlow<InitState>` - Observable initialization state (`UNINITIALIZED`, `INITIALIZING`, `INITIALIZED`, `ERROR`)
- `pharmacy: IaSdkPharmacy` - Pharmacy-related operations
- `ordering: IaSdkOrdering` - Cart and ordering operations
- `rx: IaSdkRx` - Prescription-redemption operations

#### IaSdk Methods

- `register(vararg modules: SdkModule): IaSdk` - Register SDK modules (call before `init`)
- `init(context, environmentType, apiKey, clientId, configuration = IaSdkConfiguration(), sdkEventListener = null)` - Initialize SDK with configuration
- `setEventListener(listener: SdkEventListener?): IaSdk` - Set event listener
- `setUserData(user: IaUserData?): IaSdk` - Set/update user data (`null` clears)
- `setUserData(user: GuestUser?): IaSdk` - **Deprecated**, use the `IaUserData` overload
- `setUserDeliveryAddress(address: IaUserAddress?): IaSdk` - Set/replace the host-provided delivery address (`null` removes)
- `setUserBillingAddress(address: IaUserAddress?): IaSdk` - Set/replace the host-provided billing address (`null` removes)
- `setHostUiConfig(config: HostUiConfig?): IaSdk` - Update UI configuration
- `setHapticFeedbackEnabled(enabled: Boolean): IaSdk` - Enable or disable SDK haptic feedback at runtime
- `setAnalyticsListener(listener: AnalyticsListener?): IaSdk` - Receive analytics events
- `setAnalyticsConsent(consent: AnalyticsConsent): IaSdk` - `GRANTED` (default), `DENIED` or `UNKNOWN`
- `isInitialized(): Boolean` - Check if SDK is initialized
- `suspend isPrerequisiteFlowCompleted(): Boolean` - Snapshot of whether onboarding, legal, and pharmacy selection are all satisfied. See [Prerequisite Flow Callback](#prerequisite-flow-callback)
- `resetPrerequisites(): Boolean` - Reset onboarding/legal/pharmacy flags so the full prerequisite chain runs again (debug and QA surfaces)
- `clearAllData(): Boolean` - Clear all SDK data
- `transferSDKv1UserData(context: Context)` - Transfer user data from SDK v1 to v2 (call before `init`)
- `setImpressumClickListener(listener: SdkImpressumListener): IaSdk` - Set impressum listener
- `setShowDataProcessingClickListener(listener: SdkShowDataProcessingListener): IaSdk` - Set data processing listener

#### IaSdkOrdering Methods

- `setCartListener(listener: CartListener?): IaSdkOrdering` - Listen to cart changes
- `setCheckoutListener(listener: CheckoutListener?): IaSdkOrdering` - Listen to checkout completion
- `transferPrescriptions(context: Activity, transferPrescriptionRequest: TransferPrescriptionRequest, transferPrescriptionListener: TransferPrescriptionListener = DefaultTransferPrescriptionListener(), options: (NavOptionsBuilder.() -> Unit)? = null, presentationMode: PresentationMode): Boolean` - Transfer prescription data; `false` if the SDK is not initialized or ordering is not registered
- `deleteCart(): Boolean` - Clear cart contents

#### IaSdkPharmacy Methods

- `setPharmacyId(pharmacyId: String, pharmacyConfigListener: PharmacyConfigListener? = null)` - Set pharmacy id and observe result (`Success`, `ValidationFailed`, `NotInitialized`)
- `suspend getPharmacyId(): String` - Get the current pharmacy ID, empty string when none is set
- `suspend clearPharmacyId()` - Clear the current pharmacy ID and the cart

#### IaSdkRx Methods

- `setRedeemPrescriptionListener(listener: RedeemPrescriptionListener?): IaSdkRx` - Intercept scanned prescriptions before the SDK processes them (`null` restores default behaviour)

#### Views

- `IaSdkScreen(sdkEntryPoint, presentationMode = EMBEDDED, onNavigateToTarget = null, onResult = null, onFinish = {}, onPrerequisiteResult = null)` - Composable
- `IaSdkActivity.start(context, view, onBackAction = { true }, onNavigateToTarget = null, onResult = null)` - Launch in a new activity
- `IaSdkActivity.finishAllActivities()` - Finish every open SDK activity
- `IaSdkView.createView(context: Context, sdkEntryPoint, onNavigateToTarget = null): View` - Create an SDK `View` (needs an `Activity` context)

### 11. Troubleshooting

**SDK not initializing?**

- Ensure you called `register()` before `init()`, and that `IntegrationsModule` is registered
- Check that your API key and client ID are valid
- Monitor `initState` flow or implement `SdkEventListener` to see initialization errors
- Error reference: [ERRORS.md](ERRORS.md)

**Navigation not working?**

- Verify you're using the correct `IaScreen` for your desired screen
- Ensure SDK modules for required features are registered
- Check that `onNavigateToTarget` callback is properly implemented for tab navigation

**Cart not updating?**

- Verify ordering module is registered: `IaSdk.register(IntegrationsModule, OrderingModule)`

**Build fails resolving `de.link4health` or `com.mapbox` artifacts?**

- Add the Mapbox repository (always) and the Link4Health repository (cardlink) as shown in [Adding repositories](#1-adding-repositories)

**Manifest merger fails with "uses-sdk:minSdkVersion 27 cannot be smaller than version 30 declared in library"?**

- Add the `tools:overrideLibrary` entry from [CardLink requirements](#cardlink-requirements)

### 12. Usage and Testing FAQ

For information on server environments, testing credentials, access requirements, and bug reporting guidelines,
see the [Usage and Testing documentation](https://ihreapotheken.github.io/docs/appsdk/common/usage-and-testing).

## Legal Documents

- [Legal Musterformulierungen](legal/Legal_Musterformulierungen.docx)
- [Legal Requirements](legal/Legal_Requirements.docx)
