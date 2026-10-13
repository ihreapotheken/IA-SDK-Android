# CardLink SDK Integration Guide

## Overview

CardLink SDK enables users to scan their health insurance card (eGK) and redeem electronic prescriptions directly from your app.

There are two ways to use CardLink:

- **Through the IA SDK (recommended).** Register `CardlinkModule` next to your other modules and open
  `IaScreen.RedeemPrescription` (or let the SDK's own screens open it). The SDK handles configuration, consent,
  pharmacy checks and the cart. The CardLink section is shown only when the selected pharmacy has both CardLink
  services enabled (PCASE36 and PCASE38), the device runs Android 11+ and supports NFC. See the
  [Redeem Prescription Screen](README.md#redeem-prescription-screen) section of the main README.
- **Standalone, with `CardLink.Builder`.** For hosts that drive the CardLink flow themselves without the SDK UI.
  This document describes the standalone API.

## Requirements

- Android **11 (API 30)** at runtime. The `cardlink` module declares `minSdk 30`; on older devices every launch
  reports `failedToInitializeCardlink()` and does nothing else.
- NFC-capable device.
- The Link4Health Maven repository and the BouncyCastle packaging exclude, see below.

## Dependencies

Add the Link4Health repository to `settings.gradle.kts` (next to the IA SDK and Mapbox repositories):

```kotlin
repositories {
    maven {
        name = "Link4Health"
        url = uri("https://nexus.link4.health/repository/link4health-releases/")
        mavenContent { releasesOnly() }
    }
}
```

Add the CardLink module to your `build.gradle.kts`, using the same version as your other IA SDK modules:

```kotlin
implementation("de.ihreapotheken.sdk:cardlink:<version>")
```

If your app's `minSdk` is below 30, allow the manifest merger to accept the module:

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

> [!NOTE]
> `CardLink.startCardLink` and `CardLink.startMyCards` are annotated `@RequiresApi(30)`. If your `minSdk` is below
> 30, Android Lint flags unguarded calls; wrap them in a `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R` check or
> rely on `failedToInitializeCardlink()`.

## Basic Usage

### Starting CardLink

```kotlin
// Build configuration
val config = CardLink.Builder()
    .setSdkApiKey("your-api-key")                           // Required
    .addPharmacyId("pharmacy-id")                           // Required
    .addConsentStatus(ConsentStatus.SHOW_CONSENT)           // Required
    .addListener(cardLinkListener)                          // Required
    .setCardLinkEnvironment(CardLinkSdkEnvironmentType.PRODUCTION)
    .build()

// Start CardLink
CardLink.startCardLink(activity, config)
```

## Builder Methods

### Required Parameters

| Method | Description |
|--------|-------------|
| `setSdkApiKey(String)` | SDK API key for authentication |
| `addPharmacyId(String)` | Pharmacy identifier for authorization |
| `addConsentStatus(ConsentStatus)` | Consent screen status |
| `addListener(CardLinkListener)` | Callback listener for events |

### Optional Parameters

| Method | Description |
|--------|-------------|
| `setUserId(String)` | User identifier (defaults to "guest_user_id") |
| `addPhoneNumber(String)` | Pre-fill phone number (only works with `CONSENT_ACCEPTED`) |
| `setSaveCardEnabled(Boolean)` | Enable save card feature |
| `setSavedCardName(String)` | Start directly with a saved card; must match `InsuranceCard.name` |
| `setCanCode(String)` | Pre-fill the card access number (CAN) |
| `setCardLinkEnvironment(CardLinkSdkEnvironmentType)` | `DEBUG` or `PRODUCTION` (default) |
| `setApplicationId(String)` | App ID for PlayStore redirect from FAQ |
| `setHostAppLogFilePath(String)` | Absolute path to your app's log file. CardLink encrypts it and attaches it to "Report a problem" submissions together with its own logs; you do not need to encrypt it yourself |
| `addIllustrationColor(CardLinkIllustrationColor)` | Accent colour of the mascot illustrations: `RED` (default), `BLUE`, `PINK`, `GREEN`, `ORANGE`, `YELLOW`, `PURPLE` |

### Color Customization

Set colors individually:

```kotlin
CardLink.Builder()
    .addPrimaryColor(Color(0xFFF46300))
    .addButtonsColor(Color(0xFFBB846B))
    .addTextLinkColor(Color(0xFF9E923B))
    .addBottomNavigationColor(Color(0xFF8E4D2E))
    .addIllustrationColor(CardLinkIllustrationColor.ORANGE)
    // ...
```

Or set all at once:

```kotlin
CardLink.Builder()
    .addColorScheme(
        primaryColor = Color(0xFFF46300),
        buttonsColor = Color(0xFFBB846B),
        textLinkColor = Color(0xFF9E923B),
        bottomNavigationColor = Color(0xFF8E4D2E)
    )
    // ...
```

Colors can be provided as `@ColorInt Int` or Compose `Color`. The illustration colour is one of a curated set
(`CardLinkIllustrationColor`), not a free colour value.

## ConsentStatus

| Status | Description |
|--------|-------------|
| `SHOW_CONSENT` | Show consent screen on first CardLink launch |
| `CONSENT_ACCEPTED` | Consent already accepted, phone number can be passed |
| `CONSENT_DECLINED` | Consent declined, user treated as guest |

## CardLinkListener

Implement `CardLinkListener` to receive callbacks:

```kotlin
val cardLinkListener = object : CardLinkListener {

    override fun onConsentAccepted(setPhoneNumber: (String) -> Unit) {
        // User accepted consent
        // Call setPhoneNumber("user-phone") to provide phone number
    }

    override fun onConsentDeclined() {
        // User declined consent
    }

    override fun onSessionCreated(session: CardLinkSession) {
        // CardLink session created successfully
    }

    override fun onPrescriptionsRedeemed(prescriptions: String) {
        // Prescriptions were successfully redeemed
    }

    override fun onGoToCart() {
        // User wants to navigate to cart
    }

    override fun openTermsAndConditions() {
        // User tapped on terms and conditions link
    }

    override fun onSaveHealthCard(card: InsuranceCard) {
        // User saved their health card
    }

    override fun reportAnalytics(event: String) {
        // Analytics event reported
    }

    override fun failedToInitializeCardlink() {
        // SDK failed to initialize (e.g., invalid API key, or device below Android 11)
    }
}
```

## Additional APIs

### Saved Cards Management

```kotlin
// Get all saved cards for a user (userId = null means the guest user)
val cards: List<InsuranceCard> = CardLink.getSavedCards(context, userId)

// Delete a saved card by its name
CardLink.deleteCard(context, cardName, userId)

// Update a saved card in place: rename it and overwrite CAN and phone number.
// The card is located by SavedCardData.userId + originalName.
CardLink.updateCard(
    context = context,
    originalName = "Meine Karte",
    card = SavedCardData(
        name = "Meine Karte (neu)",
        phoneNumber = "+491234567",
        canCode = "123456",
        userId = userId ?: "",
    ),
)

// Persist a new order of the saved cards, identified by CAN code
CardLink.reorderSavedCards(context, orderedCanCodes = listOf("123456", "654321"), userId = userId)
```

### Start with Saved Card

```kotlin
val config = CardLink.Builder()
    .setSdkApiKey("your-api-key")
    .addPharmacyId("pharmacy-id")
    .setUserId("user-id")
    .addConsentStatus(ConsentStatus.CONSENT_ACCEPTED)
    .addListener(cardLinkListener)
    // Use a saved card - skips consent screen; the name must match InsuranceCard.name
    .setSavedCardName("Meine Karte")
    .setCanCode("123456")
    .build()

CardLink.startCardLink(activity, config)
```

### My Cards Screen

Launch the saved cards management screen:

```kotlin
CardLink.startMyCards(activity, config)
```

### Log File Access

```kotlin
val logFilePath: String = CardLink.getLogFilePath(context)
```

### Clear All Data

```kotlin
CardLink.clearAllCardLinkData(context)
```

> **Warning**: This clears all session data, tokens, and saved cards. Cannot be undone.

## Complete Example

```kotlin
class MyActivity : AppCompatActivity() {

    private val cardLinkListener = object : CardLinkListener {
        override fun onConsentAccepted(setPhoneNumber: (String) -> Unit) {
            // Optionally set phone number after consent
            setPhoneNumber("+49123456789")
        }

        override fun onConsentDeclined() {
            Log.d("CardLink", "User declined consent")
        }

        override fun onSessionCreated(session: CardLinkSession) {
            Log.d("CardLink", "Session: $session")
        }

        override fun onPrescriptionsRedeemed(prescriptions: String) {
            Log.d("CardLink", "Redeemed: $prescriptions")
            // Navigate to cart or show success
        }

        override fun onGoToCart() {
            // Navigate to your cart screen
            startActivity(Intent(this@MyActivity, CartActivity::class.java))
        }

        override fun openTermsAndConditions() {
            // Open your terms URL
        }

        override fun onSaveHealthCard(card: InsuranceCard) {
            Log.d("CardLink", "Card saved: ${card.name}")
        }

        override fun reportAnalytics(event: String) {
            // Send to your analytics service
        }

        override fun failedToInitializeCardlink() {
            Toast.makeText(this@MyActivity, "CardLink init failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun startCardLink() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toast.makeText(this, "CardLink requires Android 11", Toast.LENGTH_SHORT).show()
            return
        }

        val config = CardLink.Builder()
            .setSdkApiKey("your-api-key")
            .addPharmacyId("2163")
            .addPhoneNumber("+49123456789")
            .addConsentStatus(ConsentStatus.CONSENT_ACCEPTED)
            .addListener(cardLinkListener)
            .setCardLinkEnvironment(CardLinkSdkEnvironmentType.PRODUCTION)
            .addPrimaryColor(Color(0xFFF46300))
            .addButtonsColor(Color(0xFFBB846B))
            .addTextLinkColor(Color(0xFF9E923B))
            .addBottomNavigationColor(Color(0xFF8E4D2E))
            .addIllustrationColor(CardLinkIllustrationColor.RED)
            .setApplicationId("com.example.myapp")
            .build()

        CardLink.startCardLink(this, config)
    }
}
```

## Environment Types

| Environment | Description |
|-------------|-------------|
| `DEBUG` | Development/QA environment |
| `PRODUCTION` | Production environment (default) |
