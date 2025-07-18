package de.ihreapotheke.iasdkexample

import android.app.Application
import de.ihreapotheken.sdk.integrations.IntegrationsModule
import de.ihreapotheken.sdk.integrations.api.IaSdk
import de.ihreapotheken.sdk.ordering.OrderingModule
import de.ihreapotheken.sdk.otc.OtcModule
import de.ihreapotheken.sdk.pharmacy.PharmacyModule
import de.ihreapotheken.sdk.rx.RxModule

class IASDKexampleApp : Application() {

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
                apiKey = "api_key"
            )
    }
}
