# Android IASDK documentation

**IASDK** is Android SDK that helps integrate IhreApotheken into your app by providing
plug-and-play UI and communication with backed services.

# Latest version

Latest version of IA SDK is `0.0.19-3`.

## Requirements

- min SDK: `30`

## Quick Start

### 1. Adding repository

To add our maven repository hosted on github.com to your project, add following to `repositories`
block in your `settings.gradle.kts` file:

```kotlin
repositories {
    maven {
        url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
    }

    maven {
        name = "IA SDK repo"
        url = uri("https://maven.pkg.github.com/ihreapotheken/IA-SDK-Android")
        credentials {
            username = System.getenv("GITHUB_USERNAME") ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: ""
        }
    }
}
```

To generate your access token, go to [GitHub settings page](https://github.com/settings/tokens)

#### Optional
If you want, you can instead using `System` environment variables use `local.properties` to store
your credentials. Find `local.properties` file in root of your project and add the following

```properties
github.username=<your user name>
github.password=<your github access token>
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
}
```

### 2. Setting up dependencies

Add dependencies in your `build.gradle.kts` (app module) file:

```kotlin
dependencies {
    implementation("de.ihreapotheken.sdk:integrations")

    // Only the feature UIs you need
    implementation("de.ihreapotheken.sdk:otc:<version>")
    implementation("de.ihreapotheken.sdk:ordering:<version>")
    // other features...
}
```
> [!NOTE]
> Change `<version>` with correct version.


> [!IMPORTANT]
> Dependency for  `integrations` is required, it provides the base functionality for the SDK.
> Add other dependencies for features that you want to use (otc, ordering ,rx ,pharmacy...)

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
            OtcModule,
            OrderingModule,
            PharmacyModule,
            RxModule,
        )

        // Initialize SDK
        IaSdk.init(
            context = applicationContext,
            environmentType = EnvironmentType.PRODUCTION, // or STAGING
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
                Log.e("App", "SDK initialization error: ${event.message}")
            }
            else -> { /* handle other events */
            }
        }
    }
}
```

> [!IMPORTANT]
> Module registration is required before calling `init()`. Available modules include: `OtcModule`, `OrderingModule`,
`PharmacyModule`, `RxModule`, `CardlinkModule`.

> [!NOTE]
> `SdkEventListener` doesn't necessarily need to be set at the startup, it can  also be set later with `IaSdk.setEventListener(...)`.

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

The SDK supports three integration modes:

#### A. Embedded Mode (Composable)

Embed SDK screens directly in your Compose navigation:

```kotlin
@Composable
fun MainActivity() {
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
                    MainScreen()
                }

                // SDK screens
                composable<HostAppRoute.SdkSearchScreen> {
                    IaSdkScreen(
                        sdkEntryPoint = SdkEntryPoint.SearchScreen,
                        onNavigateToEntryPoint = { targetEntryPoint ->
                            // Handle navigation to other SDK screens
                            when (targetEntryPoint) {
                                SdkEntryPoint.CartScreen -> {
                                    navController.navigate(HostAppRoute.SdkCartScreen)
                                    true // Navigation handled
                                }
                                else -> false // Let SDK handle it
                            }
                        }
                    )
                }

                composable<HostAppRoute.SdkCartScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.CartScreen)
                }

                composable<HostAppRoute.SdkPharmacyScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.PharmacyScreen)
                }

                composable<HostAppRoute.SdkStartScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.StartScreen)
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
            view = SdkEntryPoint.StartScreen
        )
    }
) {
    Text("Open SDK")
}

// With custom back action handling
IaSdkActivity.start(
    context = context,
    view = SdkEntryPoint.CartScreen,
    onBackAction = {
        // Custom logic before closing
        // Return true to close activity, false to prevent closing
        true
    }
)
```

#### C. View-Based Integration

For traditional View-based apps, create SDK views:

```kotlin
class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return IaSdkView.createView(
            context = requireContext(),
            sdkEntryPoint = SdkEntryPoint.SearchScreen
        )
    }
}
```

### 5. Available SDK Entry Points

The SDK provides the following entry points via `SdkEntryPoint` enum:

- `StartScreen` - Dashboard/home screen
- `SearchScreen` - Product search functionality
- `CartScreen` - Shopping cart and checkout
- `PharmacyScreen` - Selected pharmacy details
- `TransferPrescriptionsScreen` - For internal use only. Don't use this, if you want to transfer prescription use `Iasdk.ordering.transferPrescriptions` method
- `LegalDisclaimerScreen` - Legal disclaimers and terms
- `PrerequisiteFlow` - Onboarding and legal flow

### 6. Advanced Features

#### Setting User Data Dynamically

```kotlin
// After user logs in
IaSdk.setUserData(
    GuestUser(
        firstName = "John",
        lastName = "Doe",
        email = "john@example.com"
    )
)

// Clear user data on logout
IaSdk.setUserData(null)
```

#### Customizing UI Configuration

```kotlin
IaSdk.setHostUiConfig(
    HostUiConfig(
        showDataProcessing = true
    )
)
```

#### Transfer Prescriptions

```kotlin
// Using PresentationMode.FULL_FLOW (opens in new activity)
IaSdk.ordering.transferPrescription(
    context = context,
    images = listOf(prescriptionImageBytes),
    pdfs = listOf(prescriptionPdfBytes),
    presentationMode = PresentationMode.FULL_FLOW
)

// Using PresentationMode.OVERLAY with custom listener
IaSdk.ordering.transferPrescription(
    context = context,
    images = listOf(prescriptionImageBytes),
    pdfs = listOf(prescriptionPdfBytes),
    presentationMode = PresentationMode.OVERLAY,
    listener = object : TransferPrescriptionListener {
        override fun onTransferPrescriptionEvent(event: TransferPrescriptionEvent): HandlingDecision {
            when(event) {
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

### 7. Bottom Tab Navigation Example

For apps with bottom navigation, integrate SDK screens as tabs:

```kotlin
@Composable
fun MainActivity() {
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
                    MainScreen()
                }

                // SDK screens
                composable<HostAppRoute.SdkSearchScreen> {
                    IaSdkScreen(
                        sdkEntryPoint = SdkEntryPoint.SearchScreen,
                        onNavigateToEntryPoint = { targetEntryPoint ->
                            // Handle navigation to other SDK screens
                            when (targetEntryPoint) {
                                SdkEntryPoint.CartScreen -> {
                                    navController.navigate(HostAppRoute.SdkCartScreen)
                                    true // Navigation handled
                                }
                                else -> false // Let SDK handle it
                            }
                        }
                    )
                }

                composable<HostAppRoute.SdkCartScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.CartScreen)
                }

                composable<HostAppRoute.SdkPharmacyScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.PharmacyScreen)
                }

                composable<HostAppRoute.SdkStartScreen> {
                    IaSdkScreen(sdkEntryPoint = SdkEntryPoint.StartScreen)
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
}
```

### 8. Presentation Modes

The SDK supports three presentation modes:

- **`PresentationMode.EMBEDDED`** (default) - SDK screens are embedded in your navigation graph
- **`PresentationMode.FULL_FLOW`** - SDK screens open in a new activity with their own navigation
- **`PresentationMode.OVERLAY`** - SDK screens open as an overlay (useful for prescription transfer)

Choose the mode based on your integration needs:

```kotlin
// Embedded - for seamless integration
IaSdkScreen(
    sdkEntryPoint = SdkEntryPoint.SearchScreen,
    presentationMode = PresentationMode.EMBEDDED
)

// Full flow - for separate activity
IaSdkActivity.start(context, SdkEntryPoint.StartScreen)

// Transfer prescription as overlay
IaSdk.ordering.transferPrescription(
    context = context,
    images = listOf(prescriptionImageBytes),
    pdfs = listOf(prescriptionPdfBytes),
    presentationMode = PresentationMode.OVERLAY,
    listener = object : TransferPrescriptionListener {
        override fun onTransferPrescriptionEvent() {
            // Navigate to cart or show success message
        }
    }
)
```

### 9. Key Components Summary

- **`IaSdk`** - Main SDK entry point for initialization and configuration
- **`IaSdk.pharmacy`** - Access to pharmacy-related functionality
- **`IaSdk.ordering`** - Access to cart, ordering, and prescription features
- **`IaSdkScreen`** - Composable for embedding SDK screens
- **`IaSdkActivity`** - Activity for launching SDK in full-flow mode
- **`IaSdkView`** - Factory for creating SDK views for traditional View hierarchies
- **`SdkEntryPoint`** - Enum defining all available SDK screens
- **`PresentationMode`** - Enum defining how SDK screens are presented
- **`IaSdkConfiguration`** - Configuration options for SDK initialization
- **`InitState`** - Observable state for monitoring SDK initialization

### 10. API Reference

#### IaSdk Properties

- `initState: StateFlow<InitState>` - Observable initialization state
- `pharmacy: IaSdkPharmacy` - Pharmacy-related operations
- `ordering: IaSdkOrdering` - Cart and ordering operations

#### IaSdk Methods

- `register(vararg modules: SdkModule): IaSdk` - Register SDK modules
- `init(...)` - Initialize SDK with configuration
- `setEventListener(listener: SdkEventListener?)` - Set event listener
- `setUserData(user: GuestUser?)` - Set/update user data
- `setHostUiConfig(config: HostUiConfig?)` - Update UI configuration
- `isInitialized(): Boolean` - Check if SDK is initialized
- `clearAllData(): Boolean` - Clear all SDK data
- `setImpressumClickListener(listener: SdkImpressumListener)` - Set impressum listener
- `setShowDataProcessingClickListener(listener: SdkShowDataProcessingListener)` - Set data processing listener

#### IaSdkOrdering Methods

- `setCartListener(listener: CartListener)` - Listen to cart changes
- `setCheckoutListener(listener: CheckoutListener)` - Listen to checkout completion
- `transferPrescription(...)` - Transfer prescription data
- `deleteCart()` - Clear cart contents

#### IaSdkPharmacy Methods
- `setPharmacyId(pharmacyId: String, pharmacyConfigListener: PharmacyConfigListener? = null)` - Set pharmacy id and observe result
- `getPharmacyId(): String` - Suspend fun - Get the current pharmacy ID.

### 11. Troubleshooting

**SDK not initializing?**

- Ensure you called `register()` before `init()`
- Check that your API key and client ID are valid
- Monitor `initState` flow or implement `SdkEventListener` to see initialization errors

**Navigation not working?**

- Verify you're using the correct `SdkEntryPoint` for your desired screen
- Ensure SDK modules for required features are registered
- Check that `onNavigateToEntryPoint` callback is properly implemented for tab navigation

**Cart not updating?**

- Verify ordering module is registered: `IaSdk.register(OrderingModule)`
