package nl.vialer.voip.android.example.ui.call

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import kotlinx.android.synthetic.main.activity_call.*
import nl.vialer.voip.android.R
import nl.vialer.voip.android.VoIPPIL
import nl.vialer.voip.android.events.Event
import nl.vialer.voip.android.events.EventListener

class CallActivity : AppCompatActivity(), EventListener {

    private val voip by lazy { VoIPPIL.instance }

    private val renderUi = {
        displayCall()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        endCallButton.setOnClickListener {
            voip.endCall()
        }

        holdButton.setOnClickListener {
            voip.actions.toggleHold()
        }

        muteButton.setOnClickListener {
            voip.audio.toggleMute()
        }
    }

    override fun onResume() {
        super.onResume()

        displayCall()

        voip.events.listen(this)

        Handler().postDelayed(renderUi, 1000)
    }

    override fun onPause() {
        super.onPause()
        voip.events.stopListening(this)
        Handler().removeCallbacks(renderUi)
    }

    private fun displayCall() {
        val call = voip.call ?: return

        callTitle.text = call.remotePartyHeading
        callSubtitle.text = call.remotePartySubheading
        callDuration.text = call.prettyDuration

        holdButton.text = if (call.isOnHold) "unhold" else "hold"
        muteButton.text = if (voip.audio.isMicrophoneMuted) "unmute" else "mute"

        callStatus.text = call.state.name

        Handler().postDelayed(renderUi, 1000)
    }

    override fun onEvent(event: Event) {
        if (event == Event.CALL_ENDED) {
            finish()
        }

        if (event == Event.CALL_UPDATED) {
            displayCall()
        }
    }
}