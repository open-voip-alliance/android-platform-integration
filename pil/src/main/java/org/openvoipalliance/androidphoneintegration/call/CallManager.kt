package org.openvoipalliance.androidphoneintegration.call

import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import org.openvoipalliance.androidphoneintegration.CallSessionState
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.events.Event
import org.openvoipalliance.androidphoneintegration.events.Event.CallSessionEvent.AttendedTransferEvent.AttendedTransferAborted
import org.openvoipalliance.androidphoneintegration.events.Event.CallSessionEvent.AttendedTransferEvent.AttendedTransferEnded
import org.openvoipalliance.androidphoneintegration.events.Event.CallSessionEvent.CallEnded
import org.openvoipalliance.androidphoneintegration.helpers.startCallActivity
import org.openvoipalliance.androidphoneintegration.helpers.startVoipService
import org.openvoipalliance.androidphoneintegration.helpers.stopVoipService
import org.openvoipalliance.androidphoneintegration.logging.LogLevel
import org.openvoipalliance.androidphoneintegration.service.VoIPService
import org.openvoipalliance.androidphoneintegration.telecom.AndroidCallFramework
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplib.model.AttendedTransferSession
import org.openvoipalliance.voiplib.repository.initialise.CallListener

internal class CallManager(private val pil: PIL, private val androidCallFramework: AndroidCallFramework, val voIPLib: VoIPLib) : CallListener {

    internal var mergeRequested = false
    internal var voipLibCall: VoipLibCall? = null
    internal var transferSession: AttendedTransferSession? = null

    private val isInCall
        get() = this.voipLibCall != null

    private val isInTransfer
        get() = this.transferSession != null

    override fun incomingCallReceived(call: VoipLibCall) {
        pil.writeLog("Incoming call has been received")

        if (!isInCall) {
            pil.writeLog("There is no active call so setting up our new incoming call")
            this.voipLibCall = call
            pil.events.broadcast(Event.CallSessionEvent.IncomingCallReceived::class)
            androidCallFramework.addNewIncomingCall(call.phoneNumber)
        }
    }

    override fun callConnected(call: VoipLibCall) {
        super.callConnected(call)
        pil.writeLog("A call has connected!")
        pil.writeLog(voIPLib.actions(call).callInfo())

        if (!VoIPService.isRunning) {
            pil.writeLog("The VoIP service is not running, starting it.")
            pil.app.application.startVoipService()
        }

        if (isInTransfer) {
            pil.events.broadcast(Event.CallSessionEvent.AttendedTransferEvent.AttendedTransferConnected::class)
        } else {
            pil.events.broadcast(Event.CallSessionEvent.CallConnected::class)
            pil.app.application.startCallActivity()
            androidCallFramework.connection?.setActive()
        }
    }

    override fun outgoingCallCreated(call: VoipLibCall) {
        pil.writeLog("An outgoing call has been created")

        if (!isInCall) {
            pil.writeLog("There is no active call yet so we will setup our outgoing call")
            this.voipLibCall = call
            pil.events.broadcast(Event.CallSessionEvent.OutgoingCallStarted::class)

            if (androidCallFramework.connection == null) {
                pil.writeLog("There is no connection object!", LogLevel.ERROR)
            }

            androidCallFramework.connection?.setCallerDisplayName(
                pil.calls.active?.remotePartyHeading,
                TelecomManager.PRESENTATION_ALLOWED
            )

            pil.app.application.startCallActivity()
        } else {
            pil.events.broadcast(Event.CallSessionEvent.AttendedTransferEvent.AttendedTransferStarted::class)
        }
    }

    override fun callEnded(call: VoipLibCall) {
        pil.writeLog("Call has ended")
        pil.writeLog(voIPLib.actions(call).callInfo())

        // Copying the session state before we start assigning calls so these
        // events can contain the call objects.
        val sessionState = pil.sessionState

        if (!pil.calls.isInTransfer) {
            pil.writeLog("We are not in a call so tearing down VoIP")
            this.voipLibCall = null
            pil.app.application.stopVoipService()
            androidCallFramework.connection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            androidCallFramework.connection?.destroy()
            mergeRequested = false
            pil.events.broadcast(
                if (mergeRequested) AttendedTransferEnded(sessionState) else CallEnded(sessionState)
            )
        } else {
            transferSession = null
            pil.events.broadcast(AttendedTransferAborted(sessionState))
        }
    }

    override fun callUpdated(call: VoipLibCall) {
        super.callUpdated(call)
        pil.events.broadcast(Event.CallSessionEvent.CallStateUpdated::class)
    }

    override fun error(call: VoipLibCall) {
        pil.writeLog("There was an error, sending a call ended event.")
        callEnded(call)
    }
}
