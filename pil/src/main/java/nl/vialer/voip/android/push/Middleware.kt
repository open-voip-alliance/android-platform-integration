package nl.vialer.voip.android.push

import com.google.firebase.messaging.RemoteMessage

interface Middleware {

    fun respond(remoteMessage: RemoteMessage, available: Boolean)

    fun tokenReceived(token: String)
}