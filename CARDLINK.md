# CardLink SDK Integration Guide

## Overview

CardLink SDK enables users to scan their health insurance card (eGK) and redeem electronic prescriptions directly from your app.

## Dependencies

Add the CardLink module to your `build.gradle.kts`:

```kotlin
implementation("de.ihreapotheken.sdk:cardlink:2.0.0")
```

## Basic Usage

### Starting CardLink

```kotlin
import de.ihreapotheken.sdk.cardlink.CardLinkListener
import de.ihreapotheken.sdk.cardlink.CardLinkSdkEnvironmentType
import de.ihreapotheken.sdk.cardlink.ConsentStatus
import de.ihreapotheken.sdk.cardlink.api.CardLink
import de.ihreapotheken.sdk.cardlink.domain.model.CardLinkSession
import de.ihreapotheken.sdk.cardlink.domain.model.insurancecard.InsuranceCard

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
| `setCardLinkEnvironment(CardLinkSdkEnvironmentType)` | `DEBUG` or `PRODUCTION` (default) |
| `setApplicationId(String)` | App ID for PlayStore redirect from FAQ |

### Color Customization

Set colors individually:

```kotlin
CardLink.Builder()
    .addPrimaryColor(Color(0xFFF46300))
    .addButtonsColor(Color(0xFFBB846B))
    .addTextLinkColor(Color(0xFF9E923B))
    .addBottomNavigationColor(Color(0xFF8E4D2E))
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

Colors can be provided as `@ColorInt Int` or Compose `Color`.

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
        // SDK failed to initialize (e.g., invalid API key)
    }
}
```

## Additional APIs

### Saved Cards Management

```kotlin
// Get all saved cards for a user
val cards: List<InsuranceCard> = CardLink.getSavedCards(context, userId)

// Delete a saved card
CardLink.deleteCard(context, cardName, userId)
```

### Start with Saved Card

```kotlin
val config = CardLink.Builder()
    .setSdkApiKey("your-api-key")
    .addPharmacyId("pharmacy-id")
    .setUserId("user-id")
    .addConsentStatus(ConsentStatus.CONSENT_ACCEPTED)
    .addListener(cardLinkListener)
    // Use a saved card - skips consent screen
    // savedCardName should match InsuranceCard.name
    .build()
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
