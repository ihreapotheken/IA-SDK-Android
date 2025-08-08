package de.ihreapotheke.iasdkexample3

import android.app.Application
import de.ihreapotheken.sdk.integrations.IntegrationsModule
import de.ihreapotheken.sdk.integrations.api.IaSdk
import de.ihreapotheken.sdk.ordering.OrderingModule

class IASDKexampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        IaSdk
            .register(
                IntegrationsModule,
                OrderingModule,
            )
            .init(
                context = applicationContext,
                apiKey = "api_key",
                clientID = "103"
            )
    }
}
