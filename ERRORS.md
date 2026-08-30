# IA SDK Error Reference

This document lists the errors the IASDK can produce, how each one reaches your app, and what to
do about it.

Your app receives errors as typed values — sealed-class cases such as
`SdkEvent.InitError.ApiKeyNotValid` — through the listeners and state flow described in
[How errors reach your app](#how-errors-reach-your-app).

Most errors never reach your app: the SDK renders them in its own screens. Those are listed in
[Errors the SDK handles for you](#errors-the-sdk-handles-for-you) so you know what your users may
encounter.

## Contents

- [How errors reach your app](#how-errors-reach-your-app)
- [Initialization errors](#initialization-errors)
- [Pharmacy configuration errors](#pharmacy-configuration-errors)
- [Prescription transfer errors](#prescription-transfer-errors)
- [Prerequisite flow outcomes](#prerequisite-flow-outcomes)
- [Network and server errors](#network-and-server-errors)
- [CardLink error codes](#cardlink-error-codes)
- [Errors the SDK handles for you](#errors-the-sdk-handles-for-you)

---

## How errors reach your app

| Channel | How you receive it | What it carries |
|---------|--------------------|-----------------|
| `SdkEventListener` | Pass it to `IaSdk.init(...)`. Only the listener given to `init` receives these events; registering one with `IaSdk.setEventListener(...)` alone does not deliver them, and a later `init(...)` without a listener clears it. | `SdkEvent.InitError` cases for initialization failures, and `SdkEvent.InitStatus` for progress. |
| `IaSdk.initState` | Collect the `StateFlow<InitState>`. | `UNINITIALIZED`, `INITIALIZING`, `INITIALIZED`, `ERROR`. |
| `PharmacyConfigListener` | Pass it to `IaSdk.pharmacy.setPharmacyId(pharmacyId, listener)`. | `PharmacyConfigResult.Success`, `ValidationFailed`, `NotInitialized`. |
| `TransferPrescriptionListener` | Pass it to `IaSdk.ordering.transferPrescriptions(...)`. | `TransferPrescriptionEvent.Success`, `Failed(errorMessage)`, `Loading`. |
| `IaSdkScreen` / `IaPrerequisiteActivity` | `onPrerequisiteResult` callback, or the activity result. | `PrerequisiteResult.COMPLETED` / `CANCELLED`; `RESULT_OK` / `RESULT_CANCELED`. |
| SDK screens | Nothing — the SDK renders the error itself. | A full-screen view with retry, a bottom sheet, or a snackbar, depending on the screen. |
| Log output | Logcat, in debug builds. | Details of errors the SDK handled internally. |

> **Boolean return values are not success indicators.** `clearAllData()`, `resetPrerequisites()`,
> `deleteCart()` and `transferPrescriptions()` return `true` as soon as the SDK is initialized and
> the work has been scheduled. They do not wait for it, and a failure still returns `true`.
> `isPrerequisiteFlowCompleted()` likewise returns `false` both when prerequisites are pending and
> when the SDK is not initialized.

---

## Initialization errors

Delivered to your `SdkEventListener` as `SdkEvent.InitError`. Read the case, not `message` —
message text is English prose intended for logs and may change.

| Condition | Case | What it means | What to do |
|-----------|------|---------------|------------|
| API key rejected | `ApiKeyNotValid` | The backend rejected your API key when fetching feature permissions. | Check the key for the environment you are targeting. Treat this as fatal yourself — see the note below. |
| API key missing | `ApiKeyNotProvided` | `IaSdk.init` was called with a blank API key. Initialization stops immediately. | Supply a non-empty API key. `initState` stays `UNINITIALIZED`. |
| Client ID missing | `ClientIdNotProvided` | `IaSdk.init` was called with a blank client ID. Initialization stops immediately. | Supply a non-empty client ID. `initState` stays `UNINITIALIZED`. |
| Initialization failed | `Generic(message)` | Any other initialization failure — a failed configuration fetch, a network problem, or an unexpected error during bootstrap. `message` describes the cause. | Retry initialization. This is the only case that also sets `initState` to `ERROR`. |

> **`ApiKeyNotValid` does not stop initialization.** The SDK reports it and then continues, so your
> listener still receives `InitStatus.InitializationCompleted` and `initState` still reaches
> `INITIALIZED` — for a session with no backend-granted features. If you need to block on an invalid
> key, record this event and act on it yourself.

> **`initState` reaches `ERROR` only for `Generic`.** A missing API key or client ID leaves the
> state at `UNINITIALIZED`, and an invalid key still ends at `INITIALIZED`, so `initState` alone is
> not sufficient to detect every failure. Handle both the state and the events.

> **Do not branch on `SdkEvent.InitError.NotInitialized`.** The case is declared in the API but is
> never dispatched; a `when` branch for it is unreachable. Calling SDK functionality before
> initialization completes is logged and the call returns without effect.

---

## Pharmacy configuration errors

Delivered to the `PharmacyConfigListener` you pass to `IaSdk.pharmacy.setPharmacyId(...)`.

| Condition | Case | What it means | What to do |
|-----------|------|---------------|------------|
| Pharmacy ID rejected | `ValidationFailed(pharmacyId, reason)` | The ID is malformed or does not match an existing pharmacy. | Check the ID you passed. Do not depend on `reason` carrying a value. |
| SDK not initialized | `NotInitialized` | `setPharmacyId` was called before initialization completed. Delivered synchronously, on the calling thread. | Wait for `initState` to reach `INITIALIZED` before setting a pharmacy. |

---

## Prescription transfer errors

Delivered to the `TransferPrescriptionListener` you pass to `IaSdk.ordering.transferPrescriptions(...)`.

| Condition | Case | What it means | What to do |
|-----------|------|---------------|------------|
| Transfer failed | `Failed(errorMessage)` | The prescription transfer did not complete. `errorMessage` is free-form text describing the cause. | Surface a retry to the user. Your `HandlingDecision` return value is ignored for `Failed` — the SDK always applies its own handling, showing its error screen or closing the overlay. It is honoured only for `Success`. |

---

## Prerequisite flow outcomes

The prerequisite flow collects legal consent, onboarding and pharmacy selection before SDK screens
can be shown.

| Condition | Value | What it means | What to do |
|-----------|-------|---------------|------------|
| User cancelled | `PrerequisiteResult.CANCELLED`, or `RESULT_CANCELED` from `IaPrerequisiteActivity` | The user backed out before finishing. SDK screens requiring prerequisites cannot be shown. | Return `true` from `onPrerequisiteResult` to suppress the SDK's built-in retry screen and handle it yourself; return `false` to let the SDK show it. |

> `IaSdkActivity` does not return an activity result, so launching it with `registerForActivityResult`
> always yields `RESULT_CANCELED`. Use the `onResult` callback instead. This does not apply to
> `IaPrerequisiteActivity`, which does set a result.

---

## Network and server errors

The SDK classifies every backend failure internally and surfaces it to the user as a retryable
error on the affected screen. HTTP status codes are not delivered to your app — no listener or
result type carries one. They appear in the SDK's log output, which is what a support report or a
Report-a-Problem attachment will contain.

| Condition | What it means |
|-----------|---------------|
| Request not authorized | The backend returned HTTP `401`, or the SDK's stored token had expired and could not be refreshed. An expired token is refreshed once before the request is sent; a `401` on a request that carried a valid token is not retried — the stored session is cleared and the user is returned to sign-in. |
| Server error | The backend returned an HTTP `5xx` status. The SDK reports a server error and offers a retry. |
| Request rejected | The backend returned another `4xx` status. The affected feature maps backend messages it recognises to a specific error state; anything it does not recognise falls back to the generic error state. |
| Response could not be decoded | A response arrived but did not match the expected format. |
| Empty response | A successful response arrived with no content where content was expected. |
| Connection failed | The request never completed — no connectivity, DNS failure, connection refused, or a TLS problem. |
| Request timed out | The backend did not respond within the allowed time. |

---

## CardLink error codes

CardLink communicates with the Gedisa / Link4Health backend, which returns string error codes for
card and telematics failures.

These codes are not shown to users and are not delivered through a typed callback. CardLink maps
the card and terminal codes to a full-screen error state, and the session and account codes to an
error snackbar carrying its own message; `E008` alone produces no error UI and the flow continues.

The card and terminal codes appear in the SDK log with their code string. A recognised session or
account code is logged as prose without its code, so `E001`–`E014` cannot be found by searching a
log or a Report-a-Problem attachment.

### Session and account codes

| Code | What it means |
|------|---------------|
| `E001` | The card session id is already active elsewhere; the user must abandon or wait out the other session before rescanning. |
| `E002` | The per-phone-number SMS/OTP send quota at Gedisa is exhausted; retrying will keep failing until the quota window rolls over. |
| `E003` | The maximum number of eGK cards (10) has been used within one CardLink session. The user must wait for the session to end and verify the phone number again before scanning more. |
| `E004` | Gedisa has no card session for the id the SDK sent; the local session cache is stale and the flow must restart from phone verification. |
| `E005` | The OTP/SMS verification session no longer exists at Gedisa; a new SMS must be requested. |
| `E006` | The CardLink session passed its lifetime of 15 minutes; the user must re-verify the phone number. |
| `E007` | An operation requiring a verified phone number was attempted before OTP verification completed. |
| `E008` | The card was re-linked to a new holder but Gedisa could not notify the previous one. Non-blocking: no error is shown and the flow continues; it is reported to the host analytics only. |
| `E009` | The Gedisa backend timed out during the telematics/konnektor round-trip. Retryable. |
| `E010` | The card was read but the e-prescription (eRx) list could not be retrieved from the backend. |
| `E011` | The backend could not select the SMC-B (pharmacy institution card); a pharmacy-side/telematics infrastructure fault, not a user fault. |
| `E012` | The backend requires hash-check (HCV) data that was not supplied with the request. |
| `E013` | The eGK itself is blocked; unrecoverable in-app, the user must contact their insurer. |
| `E014` | The health-card verification (HCV) value did not match what the backend expected for this card. |

### Card and terminal codes

Returned when reading the health card through the pharmacy's telematics infrastructure.

| Code | What it means |
|------|---------------|
| `Card_Update` | The eGK requires an update before prescriptions can be read; the user must have the card updated (pharmacy/insurer terminal). The replacement for PN_Result3. |
| `Card_mismatchCertificates` | The certificates on the eGK do not match each other / the expected chain. Terminal for this card. |
| `Cert_Invalid` | The eGK certificate is invalid or expired; the card must be replaced by the insurer. |
| `Card_notFound` | No matching insurance number / card record found at the backend for the scanned eGK. |
| `Konnektor_notFound` | The pharmacy's konnektor (telematics gateway) is unreachable. Infrastructure outage on the pharmacy side; retry later. |
| `C2C_Error` | Card-to-card authentication between the eGK and the SMC-B failed. |
| `Kon_unexpectedError` | Unspecified konnektor failure while reading card data (readVSD). Catch-all for telematics faults; retryable. |
| `PN_notObtained` | The processing number (Vorgangsnummer) could not be retrieved from the card at all. |
| `PN_notReadable` | The processing number was obtained but could not be decoded, for example from a damaged or misread card; a rescan may help. |
| `PN_mismatchKVNR` | The KVNR (insurance number) in the processing number does not match the scanned card; the flow is aborted. |
| `PN_mismatchHcv` | The HCV component of the processing number does not match. The task-list-level equivalent of `E014`. |
| `PN_Incomplete` | The processing number arrived incomplete or malformed. |
| `PN_Result3` | Legacy signal that the health card needs an update. Superseded by `Card_Update`; still accepted for older backends. |

---

## Errors the SDK handles for you

The conditions below are handled inside the SDK: it usually renders the error itself and your app
is not notified. Some are only written to the log with no error UI at all, and initialization
failures are also delivered to your `SdkEventListener` — each section says which. They are listed
so you know what your users may encounter and what a support report is describing.

### Initialization and configuration

Most of these never reach your app. An unexpected failure during the asynchronous part of initialization arrives as `SdkEvent.InitError.Generic` and sets `initState` to `ERROR`; a rejected API key arrives as `SdkEvent.InitError.ApiKeyNotValid` and any other failed feature-permissions fetch as `SdkEvent.InitError.Generic`, neither of which stops initialization. The rest are logged only.

| Condition | What it means |
|-----------|---------------|
| Unknown error | The SDK could not classify the failure. This is the general fallback when no more specific condition applies. |
| Feature permissions fetch failed | The feature permissions for your API key could not be fetched during initialization. They are fetched once and never re-fetched, so the features stay unavailable for the rest of the session; the next app launch tries again. |
| Remote configuration fetch failed | The remote SDK configuration could not be loaded. The SDK presents a retry screen until the configuration is fetched successfully. |
| Client ID missing during configuration fetch | No client ID is available when the SDK fetches its remote configuration. Ensure `IaSdk.init` completed with a valid client ID before SDK content is shown. |
| Pharmacy missing and finder unavailable | No valid pharmacy is configured and the pharmacy finder is not available to choose one, so the SDK cannot proceed past its prerequisite flow. Provide a valid pharmacy ID or register the Apofinder module. |
| Duplicate initialization call | `IaSdk.init` was called while a previous initialization is still running or has already completed. The duplicate call is ignored. |
| SDK used before initialization | SDK functionality was called before initialization completed. Call `IaSdk.init` and wait for it to finish first. |
| No modules registered | No SDK modules are registered because `IaSdk.register` was never called. Call `IaSdk.register` before `IaSdk.init`, even when only the built-in modules are needed. |
| Prerequisite flow already running | The prerequisite flow was requested while a previous run is still in progress. The new request is ignored and the running flow continues. |
| Network request before initialization | A network request was attempted before the SDK credentials were configured. Complete `IaSdk.init` before using features that reach the backend. |
| SDK update required | The installed SDK version is below the minimum required by the backend. The SDK has already shown the update prompt to the user, so no additional error UI is needed; a hard requirement closes the SDK flow, while a soft one lets it continue. |
| Event listener threw an exception | Initialization fails when an exception escapes your `SdkEventListener` callback. Keep event listener implementations exception-safe. |
| Unsupported operation or value | A requested operation is not supported, or the SDK received a value from the backend it does not recognize. |

### Feature availability

A feature is usable only when its module is registered, the backend permits it, its feature flag is on, and — where applicable — the selected pharmacy supports it. When a gate blocks a feature, the SDK hides its entry points or shows a feature-not-available screen.

| Condition | What it means |
|-----------|---------------|
| Modules not registered before initialization | SDK modules were not registered before initialization. `IaSdk.register(...)` must be called before `IaSdk.init()` for any feature, including the built-in ones, to load. |
| Requested feature not licensed | The booked bundle for the provided API key does not grant the requested feature. This also occurs when bundle permissions could not be fetched during initialization, which leaves every feature — paid and free alike — unavailable for the rest of the session. |
| Over-the-counter shop not licensed | The booked bundle does not include the over-the-counter shop feature. Product search, category, and product-detail screens are unavailable. |
| Pharmacy feature not licensed | The booked bundle does not include the pharmacy feature. Pharmacy detail screens are unavailable. |
| Prescriptions feature not licensed | The booked bundle does not include the prescriptions feature. Prescription screens are unavailable and the prescription scanner section is hidden on the redeem screen. |
| CardLink feature not licensed | The booked bundle does not include the CardLink feature. Redeeming prescriptions with an electronic health card is unavailable. |
| Ordering feature not licensed | The booked bundle does not include the ordering feature. Cart, checkout, and prescription transfer are unavailable, and a prescription transfer attempt shows an in-app notice instead of starting. |
| Feature disabled by remote flag | A remote feature flag turns this capability off for your tenant even though the feature is licensed and registered. Flags are fetched from the backend during initialization and cover behaviors such as express delivery, online payment, CardLink, and appointment booking. |
| Selected pharmacy lacks capability | The selected pharmacy does not support the required capability, for example CardLink health-card redemption or appointment booking. Capability varies per pharmacy and is reported by the backend. |
| Navigation blocked by unavailable feature | Navigation targeted a screen whose feature is not available, and a feature-not-available screen is shown instead of the requested destination. The feature is either not registered with the SDK or not included in the booked bundle. |

### CardLink

Health-card redemption. CardLink surfaces most of these itself — an error screen, a bottom sheet or an error snackbar depending on the condition. The missing-configuration errors instead fail when CardLink is started, and `E008` is recorded with no error UI while the flow continues. The vendor codes behind them are listed under [CardLink error codes](#cardlink-error-codes).

| Condition | What it means |
|-----------|---------------|
| Missing CardLink API key | The CardLink configuration is missing the required SDK API key. CardLink cannot start without it. |
| Missing pharmacy identifier | The CardLink configuration is missing the pharmacy ID. CardLink cannot start without one. |
| CardLink authentication failed | CardLink could not be authenticated with the backend. The API key was rejected, the feature is not permitted, or the authentication request failed. |
| CardLink not initialized | CardLink functionality was called before CardLink initialization completed. Start CardLink before requesting its instance. |
| Invalid product number | A product's PZN is invalid or unrecognized. The backend rejected the cart update, order draft, or coupon check for that product. |
| Missing order draft | Cart operations require an order draft that hasn't been created or is no longer known to the backend. |
| Too many prescriptions in cart | The cart contains more prescriptions than a single order allows. The backend rejected the cart update. |
| Cart update contains no items | A cart update carried no products, prescriptions, or e-prescriptions. Every cart update must contain at least one item. |
| Cart draft already ordered | An order has already been placed for this cart draft. The draft can no longer be modified or resubmitted. |
| E-prescription already added | The e-prescription has already been scanned or is already in the cart. Each e-prescription can be added only once. |
| Online payment required | The cart's contents can only be paid online. Pay-at-pharmacy is not offered and checkout preselects online payment. |
| Card session already in use | The card session is already in use by another connection. Only one active session per card is allowed at a time. Vendor code `E001`. |
| Verification code send limit reached | The maximum number of SMS verification codes has been sent for this session. The user must wait before requesting another code. Vendor code `E002`. |
| Maximum health cards registered | The user has reached the maximum number of registered health cards. No further cards can be added. Vendor code `E003`. |
| Card session not found | The card session is not known to the CardLink service. A new session must be started. Vendor code `E004`. |
| Verification session not found | The SMS verification session is not known to the CardLink service. Phone number verification must be restarted. Vendor code `E005`. |
| CardLink session expired | The CardLink session has expired. The user must start a new session. Vendor code `E006`. |
| Phone verification not completed | The session was used before phone number verification completed. The user must finish verification first. Vendor code `E007`. |
| Previous card holder not notified | The previous holder of the health card could not be notified when the card was registered on a new device. Vendor code `E008`. |
| CardLink service timed out | The CardLink service reported a timeout. The operation did not complete in time. Vendor code `E009`. |
| E-prescription retrieval failed | E-prescriptions could not be retrieved from the CardLink service. Vendor code `E010`. |
| Pharmacy institution card not selected | The pharmacy's institution card (SMC-B) could not be selected. The prescription transfer cannot proceed. Vendor code `E011`. |
| Hash check data missing | Required hash-check data is missing, so the card read cannot be validated. Vendor code `E012`. |
| Health card blocked | The health card is blocked and cannot be used. Vendor code `E013`. |
| Card check value mismatch | Validation of the health card's check value (HCV) failed. The card data does not match what the service expects. Vendor code `E014`. |
| Health card update required | The health card (eGK) requires an update before it can be used with CardLink. The user must have the card updated by their health insurer. Vendor code `Card_Update`. |
| Card certificates do not match | The certificates on the health card do not match. The card cannot be authenticated. Vendor code `Card_mismatchCertificates`. |
| Invalid card certificate | The health card's certificate is invalid. The card cannot be authenticated. Vendor code `Cert_Invalid`. |
| Healthcare network unreachable | The connection to the healthcare network (Konnektor) failed. The service is unreachable. Vendor code `Konnektor_notFound`. |
| Card to card authentication failed | Card-to-card authentication between the health card and the pharmacy's institution card failed. Vendor code `C2C_Error`. |
| Unexpected healthcare network error | An unexpected error occurred in the healthcare network (Konnektor) while reading the card data. Vendor code `Kon_unexpectedError`. |
| Processing number not issued | A processing number could not be obtained for the card read. Prescription retrieval cannot proceed. Vendor code `PN_notObtained`. |
| Processing number not readable | The processing number returned for the card read is not readable. Vendor code `PN_notReadable`. |
| Processing number mismatches insurance number | The processing number does not match the card's insurance number (KVNR). Vendor code `PN_mismatchKVNR`. |
| Processing number mismatches card check value | The processing number does not match the card's check value (HCV). Vendor code `PN_mismatchHcv`. |
| Processing number incomplete | The processing number returned for the card read is incomplete. Vendor code `PN_Incomplete`. |
| Insurance number not found | No matching insurance number was found for the card in this session. Vendor code `Card_notFound`. |
| Card lost during scan | The health card left the NFC field before the scan completed. The user must hold the card steady against the device and scan again. |
| Incorrect card access number | The CAN entered does not match the health card. Card authentication failed and the user must enter the correct CAN and scan again. |
| Card access number wrong length | The entered CAN is not the required six digits. Input validation fails before the card scan starts. |
| Card scan cancelled by user | The user cancelled the card scan before it completed. The scan can be restarted at any time. |
| Incorrect verification code | The SMS verification code entered does not match the code sent to the phone number. The user must re-enter or request a new code. |
| Verification code expired | The SMS verification code expired before it was submitted. A new code must be requested. |
| Too many verification attempts | Too many incorrect SMS verification attempts were made. Verification is temporarily blocked and the user must wait before trying again. |
| Invalid mobile number | The entered phone number is not a valid mobile number. Verification cannot start until a valid number is provided. |
| Mobile number not German | The entered phone number is not a German mobile number. Verification requires a German (+49) number. |
| Prescription on card unreadable | A prescription read from the health card could not be parsed and is skipped. Any remaining prescriptions are still processed. |
| Prescription without product number | A prescription read from the health card carries no PZN and cannot be matched to a product. The prescription is skipped. |
| Pharmacy deactivated for CardLink | The selected pharmacy has been deactivated for CardLink by the service provider. Prescriptions cannot be redeemed at this pharmacy through CardLink. |
| Pharmacy transaction limit reached | The selected pharmacy has reached its CardLink transaction limit. Prescriptions cannot be redeemed at this pharmacy until the limit resets. |
| Realtime connection lost | The realtime connection to the CardLink service could not be established or was lost. The card link session cannot proceed until the connection is restored. |
| Saved card update failed | Deleting, renaming, or reordering a saved health card failed. The stored card data could not be updated. |
| Saved card no longer exists | The saved health card selected for editing no longer exists in storage. It may have been deleted from another screen. |
| Prescriptions not added to cart | Prescriptions were retrieved from the health card but could not be added to the cart. The redemption did not complete. |
| Transferred prescription data unreadable | The prescription data handed over from the card link flow could not be parsed. No prescriptions were added to the cart. |
| CardLink service unavailable | The CardLink service is temporarily unavailable. The user should try again later. |
| Unrecoverable CardLink error | An unrecoverable error occurred during the card link process and the flow cannot continue. The user must restart the process. |

### Ordering and cart

Cart, checkout and prescription transfer. The SDK shows a full-screen error with retry, or an inline message on the affected screen.

| Condition | What it means |
|-----------|---------------|
| Cart not loaded yet | A cart operation was called before the cart had been fetched. The cart must be loaded before it can be modified. |
| Product not in cart | The referenced product is not in the cart. A quantity change requires a product that is already in the cart. |
| No prescription added to cart | No prescription could be added to the cart. None of the provided prescriptions were uploaded successfully. |
| Unknown order draft | The backend does not recognize the order draft. The draft may have expired or been deleted. |
| Conflicting draft owner identity | Order draft creation supplied both a signed-in user and an anonymous user. Only one identity can own a draft. |
| Coupon not valid | The applied coupon was rejected during validation. The coupon is invalid, expired, or not applicable to the cart's contents. |
| Cart item validation failed | The backend rejected one or more cart items at order confirmation. A product's availability, price, or quantity changed. |
| Delivery unavailable for ZIP code | The pharmacy does not deliver to the provided ZIP code. A different delivery option must be chosen. |
| Delivery selection rejected | Confirming the delivery selection failed. The backend rejected the delivery type or the delivery address — for example the address is out of the delivery range, the order is below the delivery minimum, or the street is missing. |
| Payment selection rejected | Confirming the payment selection failed. The backend rejected the payment type or billing details. |
| Prescription upload failed | Uploading a prescription image or e-prescription to the backend failed. The affected prescription is not added to the cart. |
| Unreadable e-prescription code | A scanned e-prescription could not be parsed. The code is malformed or is not a valid e-prescription. |
| Unspecified checkout failure | Checkout failed for an unspecified reason. The underlying cause is a network or server failure during order placement. |
| Cart error reported by server | The backend attached an error-level message to the cart. The message text comes from the server and describes the specific problem. |
| Online payment unavailable | Online payment is not available for this cart. It is disabled by configuration or the payment provider does not allow it for this order. |

### Products, pharmacies and appointments

Catalogue and booking failures. The SDK shows a full-screen error with retry when content cannot be loaded, or a snackbar when a booking or inquiry submission fails.

| Condition | What it means |
|-----------|---------------|
| Product not found | The requested product does not exist or is no longer available. |
| Product details unavailable | Product details could not be fetched from the server. |
| Frequently asked questions unavailable | The frequently-asked-questions content for a product could not be fetched. |
| Unreadable product barcode | A scanned product barcode could not be parsed into a valid product number. |
| Scanned product could not be loaded | The barcode was scanned and parsed successfully, but the matching product could not be fetched. |
| Product inquiry not sent | A product inquiry could not be sent to the pharmacy. |
| Pharmacy details unavailable | Details for the selected pharmacy could not be fetched from the server. |
| Express delivery availability unknown | Express-delivery availability for the pharmacy could not be determined. |
| Device location unavailable | The device location could not be determined for a nearby pharmacy search. |
| No pharmacies found | A pharmacy search returned no results. This is an empty result set rather than a failure. |
| Appointment booking failed | The appointment could not be booked. The session may have expired or the server rejected the request. |
| Appointment data unavailable | Appointment or event data could not be fetched from the server. Possible causes include a network failure, an expired session, or a server error. |
| No appointment type selected | The appointment booking flow was entered without an appointment type selected. |
| Medical history questionnaire unavailable | The medical-history questionnaire could not be loaded. |
| Incomplete medical history answers | Required medical-history answers are missing or invalid. The booking cannot continue until all mandatory questions are answered. |

### Runtime, files and storage

Integration mistakes, file handling and local storage. Backend and connectivity failures are listed
under [Network and server errors](#network-and-server-errors).

| Condition | What it means |
|-----------|---------------|
| Guest profile missing or incomplete | An operation requires a guest user profile, but none is stored or the stored profile is incomplete. The user must complete guest registration before continuing. |
| No pharmacy selected | No active pharmacy is set. Most SDK operations require a pharmacy to be selected first, for example via `IaSdk.pharmacy.setPharmacyId`. |
| Unknown pharmacy identifier | The provided pharmacy ID could not be validated or does not match an existing pharmacy. Check the ID passed to `IaSdk.pharmacy.setPharmacyId`. |
| Invalid product quantity | The requested product quantity is invalid or exceeds the maximum quantity allowed in the cart. |
| No foreground activity available | No visible activity is available to present UI from. Alerts and the CardLink flow cannot be shown without a foreground activity. |
| No app can open link | The link could not be opened because no installed app can handle it. |
| Attachment too large | A selected attachment or image exceeds the maximum allowed file size. |
| Image could not be read | A selected image could not be read or processed for upload. |
| Request data could not be encoded | Request data could not be encoded for transmission, for example a phone country code that is not numeric. |
| Malformed request URL | A URL could not be constructed or parsed, so the request cannot be made. |
| Module not registered | A feature was used without its module being registered. Register optional modules with `IaSdk.register` before using their screens or components; embedded components render nothing when their module is missing. |
| File compression failed | Compressing report attachments or log files failed, for example because the input file is missing, unreadable, or the compressed output is empty. |
| Not enough device storage | There is not enough storage space on the device to save a captured photo. |
| File read or write failed | Reading or writing a file failed while capturing or processing a photo. |
| Invalid launch entry point | The SDK activity was launched without a valid entry point. Launch it through `IaSdkActivity.start` with one of the provided screens. |
| Navigation callback timed out | The `onNavigateToTarget` callback did not complete in time or threw an exception. The SDK performs the navigation itself when this happens. |
| Local data could not be saved | Saving data to the SDK's encrypted local storage failed, for example when device storage is full. |
| Stored data could not be read | Encrypted data stored on the device could not be read back. This can happen when the data is corrupted or the encryption key changed, for example after a backup restore. |

### Camera and scanner

Prescription and product scanning. The SDK shows an inline scanner error or a snackbar.

| Condition | What it means |
|-----------|---------------|
| No usable camera available | The device has no usable camera or the camera is unavailable. Photo capture and scanning cannot start. |
| Photo capture failed | Capturing the photo failed. The image could not be taken or saved. |
| Unexpected camera error | An unexpected camera error occurred that does not match a more specific case. |
| Scanned content not recognized | The scanned content was not recognized as a valid prescription, product, or pharmacy code. |
| Unsupported image format | The selected image is not a supported type. Only JPEG and PNG images are accepted. |
| Barcode could not be read | The barcode or QR code could not be read or matched to a known prescription, product, or pharmacy. |

### User accounts and sign-in

Guest checkout and registered-user sessions. Validation problems appear as inline field errors; session problems return the user to sign-in.

| Condition | What it means |
|-----------|---------------|
| Email already registered | The email address entered during guest checkout already belongs to a registered account. The user is redirected to log in instead of continuing as a guest. |
| No stored user session | A session token refresh was attempted, but no signed-in user session is stored. The user must sign in again. |
| Authentication token missing or invalid | A request was rejected because the user's authentication token is missing or invalid. The user must sign in before retrying. |
| Guest registration failed | Registering the guest user with the backend failed due to a server or network error. Guest checkout cannot continue until registration succeeds. |
| Session token refresh failed | The stored session token has expired and could not be refreshed. The request fails as unauthorized and the user must sign in again. |
| Still unauthorized after refresh | The backend rejected a request even though a freshly issued token was attached. The stored session is cleared and the user must sign in again. |
| Invalid email address | The entered email address failed local validation. It is empty, too long, or contains whitespace, non-English letters, or other characters that are not allowed. |
| Invalid first or last name | The entered first or last name failed local validation. It is empty or contains digits or characters that are not allowed. |

### Device and platform

Runtime permissions, NFC and OS requirements, plus SDK integration mistakes.

| Condition | What it means |
|-----------|---------------|
| Camera permission denied | The user declined camera access when prompted. Scanning is unavailable until camera permission is granted, and the user can be asked again. |
| Camera permission permanently denied | The user permanently declined camera access ("Don't ask again"). Camera permission can only be restored through the system Settings app. |
| Location permission denied | The user declined location access. Nearby pharmacy search cannot use the device position and falls back to manual address search. |
| Location permission permanently denied | The user permanently declined location access ("Don't ask again"). Location permission can only be restored through the system Settings app. |
| NFC hardware not available | The device has no NFC hardware. Health-card (CardLink) redemption is unavailable on this device. |
| NFC turned off in settings | NFC is present but switched off in the device settings. The user must enable NFC before a health card can be scanned. |
| Android version too old | The device runs an Android version below 11 (API 30), which health-card (CardLink) redemption requires. CardLink features are unavailable on this device. |
| Missing internal dependency | An internal SDK dependency could not be resolved. SDK functionality was called before `IaSdk.init` completed, or a required module was not registered. |
| Screen flow not started | An internal component was requested before the screen flow that owns it was set up. Enter SDK screens through their documented entry points rather than navigating into an intermediate screen directly. |
| Screen flow already closed | An internal component was accessed after its owning screen flow was closed. Do not retain references to SDK screen state beyond the SDK's own navigation scope. |
| Missing Activity context | An SDK view was created with a context that has no Activity in its chain. SDK views must be created from an Activity context, not an application or service context. |
