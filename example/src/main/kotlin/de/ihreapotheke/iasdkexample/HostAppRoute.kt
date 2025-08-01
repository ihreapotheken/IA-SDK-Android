package de.ihreapotheke.iasdkexample

import kotlinx.serialization.Serializable

@Serializable
sealed class HostAppRoute  {
    @Serializable
    data object StartHostApp : HostAppRoute()

    @Serializable
    data object DefaultScreen : HostAppRoute()

    @Serializable
    data object StartSdkScreen : HostAppRoute()
}
