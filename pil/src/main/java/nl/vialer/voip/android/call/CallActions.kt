package nl.vialer.voip.android.call

import android.annotation.SuppressLint
import android.util.Log
import nl.vialer.voip.android.CallManager
import nl.vialer.voip.android.VoIPPIL
import nl.vialer.voip.android.audio.AudioRoute
import nl.vialer.voip.android.events.Event
import nl.vialer.voip.android.logging.LogLevel
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.AttendedTransferSession
import org.openvoipalliance.phonelib.model.Call

class CallActions internal constructor(private val pil: VoIPPIL, private val phoneLib: PhoneLib, private val callManager: CallManager) {

    fun hold() {
        callExists {
            phoneLib.actions(it).hold(true)
        }
    }

    fun unhold() {
        callExists {
            phoneLib.actions(it).hold(false)
        }
    }

    fun toggleHold() {
        callExists {
            phoneLib.actions(it).hold(!it.isOnHold)
        }
    }

    fun sendDtmf(dtmf: String) {
        callExists {
            phoneLib.actions(it).sendDtmf(dtmf)
        }
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

    /**
     * An easy way to perform a null safety check and log whether there was no call found.
     *
     */
    private fun callExists(callback: (call: Call) -> Unit) {
        val call = callManager.call ?: return

        callback.invoke(call)
        pil.events.broadcast(Event.CALL_UPDATED)
    }
}