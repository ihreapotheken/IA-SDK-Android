package de.ihreapotheke.iasdkexample

import android.app.Application
import android.util.Log
import de.ihreapotheken.sdk.core.data.EnvironmentType
import de.ihreapotheken.sdk.core.data.PrerequisiteFlowConfiguration
import de.ihreapotheken.sdk.core.data.model.sdk.SdkEvent
import de.ihreapotheken.sdk.core.data.model.sdk.SdkEventListener
import de.ihreapotheken.sdk.integrations.IntegrationsModule
import de.ihreapotheken.sdk.integrations.api.IaSdk
import de.ihreapotheken.sdk.integrations.api.IaSdkConfiguration
import de.ihreapotheken.sdk.ordering.OrderingModule
import de.ihreapotheken.sdk.otc.OtcModule
import de.ihreapotheken.sdk.pharmacy.PharmacyModule
import de.ihreapotheken.sdk.rx.RxModule

private const val TAG = "IASDKExampleApp"

class IASDKExampleApp : Application(), SdkEventListener {

    override fun onCreate() {
        super.onCreate()

        IaSdk
            .register(
                IntegrationsModule,
                OtcModule,
                OrderingModule,
                PharmacyModule,
                RxModule
            )
            .init(
                context = applicationContext,
                environmentType = EnvironmentType.STAGING,
                apiKey = BuildConfig.APPSDK_ACCESS_KEY,
                clientId = "6001",
                configuration = IaSdkConfiguration(
                    shouldFetchThemeFromRemote = true,
                    prerequisiteFlowConfiguration = PrerequisiteFlowConfiguration(
                        shouldRunLegal = true,
                        shouldRunOnboarding = true,
                    )
                ),
                sdkEventListener = this,
            )
    }

    override fun onSdkEvent(event: SdkEvent) {
        when (event) {
            SdkEvent.InitError.ApiKeyNotProvided -> Log.e(TAG, "Api key not provided")
            SdkEvent.InitError.ApiKeyNotValid -> Log.e(TAG, "Api key not valid")
            SdkEvent.InitError.ClientIdNotProvided -> Log.e(TAG, "Client id not provided")
            is SdkEvent.InitError.Generic -> Log.e(TAG, "Generic error: ${event.message}")
            SdkEvent.InitError.NotInitialized -> Log.e(TAG, "IA SDK not initialized")
            SdkEvent.InitStatus.AlreadyInitialized -> Log.d(TAG, "IA SDK already initialized")
            SdkEvent.InitStatus.InitializationCompleted -> Log.d(TAG, "IA SDK initialization completed")
            SdkEvent.InitStatus.Initializing -> Log.d(TAG, "IA SDK initializing")
        }
    }
}
