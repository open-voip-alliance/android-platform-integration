package org.openvoipalliance.androidphoneintegration.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.di.di
import org.openvoipalliance.androidphoneintegration.logWithContext
import org.openvoipalliance.androidphoneintegration.telecom.AndroidCallFramework

internal class FcmService : FirebaseMessagingService() {

    private val pil: PIL by lazy { di.koin.get() }
    private val androidCallFramework: AndroidCallFramework by lazy { di.koin.get() }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (pil.app.middleware?.inspect(remoteMessage) == false) {
            log("Client has inspected push message and determined this is not a call")
            return
        }

        if (!PIL.isInitialized) return

        log("Received FCM push message")

        if (androidCallFramework.isInCall) {
            log("Currently in call, rejecting incoming call")
            pil.app.middleware?.respond(remoteMessage, false)
            return
        }

        if (!androidCallFramework.canHandleIncomingCall) {
            log("The android call framework cannot handle incoming call, responding as unavailable")
            pil.app.middleware?.respond(remoteMessage, false)
            return
        }

        pil.start { success ->
            pil.app.middleware?.respond(remoteMessage, success)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (!PIL.isInitialized) return

        log("Received new FCM token")

        pil.app.middleware?.tokenReceived(token)
    }

    private fun log(message: String) = logWithContext(message, "FCM-SERVICE")
}
