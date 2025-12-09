package de.ihreapotheke.iasdkexample

import kotlinx.serialization.Serializable

@Serializable
sealed class HostAppRoute  {
    @Serializable
    data object MainScreen : HostAppRoute()

    @Serializable
    data object SdkStartScreen : HostAppRoute()

    @Serializable
    data object SdkSearchScreen : HostAppRoute()

    @Serializable
    data object SdkCartScreen : HostAppRoute()

    @Serializable
    data object SdkPharmacyScreen : HostAppRoute()
}
