# Android IASDK documentation

**IASDK** is Android SDK that helps integrate IhreApotheke into your app by providing
plug-and-play UI and communication with backed services.

## Requirements

- min SDK: `30`
-

## Quick Start

### 1. Adding repository

To add our maven repository hosted on github.com to your project, add following to `repositories` block in your `settings.gradle.kts` file:

```kotlin
repositories {
	maven {
		name = "IA SDK repo"
		url = uri("https://maven.pkg.github.com/ihreapotheken/p-IA-SDK-Android")
		credentials {
			username = System.getenv("GITHUB_USERNAME") ?: ""
			password = System.getenv("GITHUB_TOKEN") ?: ""
		}
	}
}
```

To generate your access token, go to [GitHub settings page](https://github.com/settings/tokens)

#### Optional 
If you want, you can instead using `System` environment variables use `local.properties` to store your credentials. Find `local.properties`file in root of your project and add the following

```properties
github.username=<your user name>  
github.password=<your github access token>
```

and then in `settings.gradle.kts` file fetch those values with following code (you can add this to top of file):

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
		name = "IA SDK repo"
		url = uri("https://maven.pkg.github.com/ihreapotheken/p-IA-SDK-Android")
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
    implementation("de.ihreapotheke.sdk.android:integrations")

    // Only the feature UIs you need
    implementation("de.ihreapotheke.sdk.android:otc")
    implementation("de.ihreapotheke.sdk.android:ordering")
    // other features...
}
```

> [!IMPORTANT]
> Dependency for  `integrations` is required, it provides the base functionality for the SDK.
> Add other dependencies for features that you want to use (otc, ordering ,rx ,pharmacy...)

### 3. SDK initialization

Before using any functionality from the SDK, it must be initialized in `Application` class.

1. Create (or extend) your `Application` class
2. Register the required SDK modules
3. Provide your public API key for the SDK

```kotlin
class MyApp : Application() {
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
        .init(  
            context = applicationContext,  
            apiKey = "api_key"  
        )
    }
}
```

> [!IMPORTANT]
> `IntegrationsModule` is required, it provides the base functionality for the SDK.

Modules like `OtcModule`, `OrderingModule` and others are included as needed, depending on your app features.

### 4. SDK usage

#### Wrap your Compose tree in `SdkTheme`

At the root of your `setContent { … }`, wrap in the SDK theme so our custom color/typography/dimensions are provided:

```kotlin
 class MainActivity : ComponentActivity() {  
  
    override fun onCreate(savedInstanceState: Bundle?) {  
        super.onCreate(savedInstanceState)  
  
        enableEdgeToEdge()  
  
        setContent {  
            SdkTheme {  
                AppScaffold()  
            }  
        }  
    }  
}
```

#### Scaffold + Navigation setup

Inside your top-level Composable (e.g. `AppScaffold()`), set up your `Scaffold` and a **single** `NavHost`:

```kotlin
@Composable
fun AppScaffold() {
  val navController = rememberNavController()
  var isAtRoot by remember { mutableStateOf(true) }

  Scaffold(
    bottomBar = { /* your host bottom bar, if any */ }
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
      NavHost(
        navController    = navController,
        startDestination = Route.Integration.StartScreen  // ⚠️ must be our SDK’s StartScreen in order to show Onboarding and Legal screens
      ) {
        // 1) Your host-app routes:
        hostAppNavigationGraph()

        // 2) All SDK routes
        sdkGraphProvider()
      }
    }

    // Required
    SdkEntryScreen(
      onDestinationChanged = { atRoot -> isAtRoot = atRoot },
      navController        = navController,
      startRoute           = HostAppRoute.StartHostApp // main screen that should be shown after Onboarding and Legal - can be host apps screen or one of sdk screens
    )
  }

  // Optional: protect your status bar color under Compose
  StatusBarProtection(LocalColorTokens.current.get("Header/bg"))
}

```

##### What each piece does

- **`hostAppNavigationGraph()`**  
    Your own app’s navigation graph (declare in your code).
    
- **`sdkGraphProvider()`**  
    Brings in every SDK screen under the same `NavHost` (you don’t need to list them by hand).
    
- **`Route.Integration.StartScreen`**  
    Entry-point for the SDK: runs onboarding, legal, and pharmacy-picker flows before your “real” feature.
    
- **`SdkEntryScreen(...)`**  
    Listens and handles sdk navigation changes

#### Host-app Route Definition

Define your own routes in a NavGraphBuilder.hostAppNavigationGraph() extension, for example:

```kotlin
fun NavGraphBuilder.hostAppNavigationGraph() {
    navigation<HostAppRoute.StartHostApp>(startDestination = HostAppRoute.DefaultScreen) {
        composable<HostAppRoute.DefaultScreen> {
            /* Your home screen UI */
        }
        // ... other host screens
    }
}
```

And your routes:

```kotlin
@Serializable
sealed class HostAppRoute  {

    @Serializable
    data object StartHostApp : HostAppRoute()

    @Serializable
    data object DefaultScreen : HostAppRoute()
}
```

#### Navigating to Sdk Screens

To access Sdk Features you need to navigate to the entry point of feature's flow

```kotlin
    // openSdkSearchScreen
navController.navigate(Route.Otc.StartScreen)

// openSdkCart
navController.navigate(Route.Ordering.StartScreen)

// openSdkPharmacyScreen
navController.navigate(Route.Pharmacy.StartScreen)
```

#####  Bottom Tab Navigation

Also, you can implement Bottom tab navigation in your app and call Sdk's destinations like this

```kotlin
    private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
    when (selectedTab) {
        BottomTab.OTC -> navController.navigate(Route.Otc.StartScreen) {
            manageBottomNavigationBackStack()
        }
        BottomTab.CART -> navController.navigate(Route.Ordering.StartScreen) {
            manageBottomNavigationBackStack()
        }
        BottomTab.PHARMACY -> navController.navigate(Route.Pharmacy.StartScreen) {
            manageBottomNavigationBackStack()
        }
    }
}
```

For bottom‐tab back‐stack management, follow [Google’s Compose Navigation guide](https://developer.android.com/develop/ui/compose/navigation#bottom-nav):
```kotlin
private fun NavOptionsBuilder.manageBottomNavigationBackStack() {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
    restoreState = true
}
```

### 5. How it works

- **`SdkTheme`**  
  Injects color palettes, typography, and dimensions.

- **`hostAppNavigationGraph()`**  
  Registers your own app’s destinations first.

- **`sdkGraphProvider()`**  
  Auto-includes every SDK feature under the same navigation graph—no manual listing required.

- **`Route.Integration.StartScreen`**  
  The SDK’s entry point: will run onboarding → legal → apofinder → your feature.

- **`SdkEntryScreen`**  
  Listens for nav changes, monitor the current destination (e.g. for UI adjustments) and resets to `startDestination` when your SDK flow completes.

- **`StatusBarProtection`**  
  Applies a safe background color to avoid status-bar bleed-through.

### 6. Recap of Host Integration Steps

1. **Gradle**: `implementation project(":integration")` + feature modules

2. **Theme**: wrap root in `SdkTheme { … }`

3. **NavHost**: call `hostAppNavigationGraph()` followed by `sdkGraphProvider()`

4. **Entry Screen**: include `SdkEntryScreen(...)` below your NavHost

5. **Status Bar**: optionally use `StatusBarProtection(...)`
