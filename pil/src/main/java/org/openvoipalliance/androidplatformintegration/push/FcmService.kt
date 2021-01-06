package org.openvoipalliance.androidplatformintegration.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.openvoipalliance.androidplatformintegration.PIL
import org.openvoipalliance.androidplatformintegration.di.di
import org.openvoipalliance.androidplatformintegration.telecom.AndroidCallFramework

internal class FcmService : FirebaseMessagingService() {

    private val pil: PIL by di.koin.inject()
    private val androidCallFramework: AndroidCallFramework by di.koin.inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (!PIL.isInitialized) return

        if (androidCallFramework.isInCall) {
            pil.app.middleware?.respond(remoteMessage, false)
            return
        }

        pil.start {
            pil.app.middleware?.respond(remoteMessage, true)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (!PIL.isInitialized) return

        pil.app.middleware?.tokenReceived(token)
    }
}
