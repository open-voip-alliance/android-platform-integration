package org.openvoipalliance.androidphoneintegration.call

import android.annotation.SuppressLint
import org.openvoipalliance.androidphoneintegration.PIL
import org.openvoipalliance.androidphoneintegration.audio.LocalDtmfToneGenerator
import org.openvoipalliance.androidphoneintegration.events.Event
import org.openvoipalliance.androidphoneintegration.telecom.AndroidCallFramework
import org.openvoipalliance.androidphoneintegration.telecom.Connection
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplib.model.Call

class CallActions internal constructor(
    private val pil: PIL,
    private val phoneLib: VoIPLib,
    private val callManager: CallManager,
    private val androidCallFramework: AndroidCallFramework,
    private val localDtmfToneGenerator: LocalDtmfToneGenerator
) {

    fun hold() {
        connection {
            it.onHold()
        }
    }

    fun unhold() {
        connection {
            it.onUnhold()
        }
    }

    fun toggleHold() {
        connection {
            it.toggleHold()
        }
    }

    /**
     * Send a string of DTMF, no local tone will be played.
     *
     */
    fun sendDtmf(dtmf: String) {
        callExists {
            phoneLib.actions(it).sendDtmf(dtmf)
        }
    }

    /**
     * Send a single DTMF character.
     *
     * @param playToneLocally If true, will play the requested DTMF tone to the local user.
     */
    fun sendDtmf(dtmf: Char, playToneLocally: Boolean = true) {
        if (playToneLocally) {
            localDtmfToneGenerator.play(dtmf)
        }

        sendDtmf(dtmf.toString())
    }

    @SuppressLint("MissingPermission")
    fun beginAttendedTransfer(number: String) {
        callExists {
            callManager.transferSession = phoneLib.actions(it).beginAttendedTransfer(number)
        }
    }

    fun completeAttendedTransfer() {
        callManager.transferSession?.let {
            phoneLib.actions(it.from).finishAttendedTransfer(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun answer() {
        connection {
            it.onAnswer()
        }
    }

    @SuppressLint("MissingPermission")
    fun decline() {
        connection {
            it.onReject()
        }
    }

    fun end() {
        connection {
            it.onDisconnect()
        }
    }

    private fun connection(callback: (connection: Connection) -> Unit) {
        val connection = androidCallFramework.connection ?: return

        callback.invoke(connection)

        pil.events.broadcast(Event.CallEvent.CallUpdated(pil.calls.active))
    }

    /**
     * An easy way to perform a null safety check and log whether there was no call found.
     *
     */
    private fun callExists(callback: (call: Call) -> Unit) {
        var call = callManager.call ?: return

        callManager.transferSession?.let {
            call = it.to
        }

        callback.invoke(call)
        pil.events.broadcast(Event.CallEvent.CallUpdated(pil.calls.active))
    }
}
