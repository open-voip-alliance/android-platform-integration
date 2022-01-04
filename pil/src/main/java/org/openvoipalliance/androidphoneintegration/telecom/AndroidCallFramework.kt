package org.openvoipalliance.androidphoneintegration.telecom

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import org.openvoipalliance.androidphoneintegration.BuildConfig

internal class AndroidCallFramework(
    context: Context,
    private val telecomManager: TelecomManager
) {
    internal var connection: Connection? = null

    private val handle: PhoneAccountHandle = PhoneAccountHandle(
        ComponentName(context, ConnectionService::class.java),
        PHONE_ACCOUNT_HANDLE_ID
    )

    private val phoneAccount: PhoneAccount = PhoneAccount.builder(
        handle,
        BuildConfig.LIBRARY_PACKAGE_NAME,
    )
        .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
        .build()

    init {
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    @SuppressLint("MissingPermission")
    fun placeCall(number: String) {
        telecomManager.placeCall(
            Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null),
            Bundle().apply {
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                putInt(
                    TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_AUDIO_ONLY
                )
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
            }
        )
    }

    val isInCall
        @SuppressLint("MissingPermission")
        get() = telecomManager.isInCall

    val canMakeOutgoingCall
        get() = telecomManager.isOutgoingCallPermitted(handle)

    val canHandleIncomingCall
        get() = telecomManager.isIncomingCallPermitted(handle)

    fun addNewIncomingCall(from: String) {
        telecomManager.addNewIncomingCall(
            handle,
            Bundle().apply {
                putParcelable(
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, from, null)
                )
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
            }
        )
    }

    companion object {
        private const val PHONE_ACCOUNT_HANDLE_ID = BuildConfig.LIBRARY_PACKAGE_NAME
    }
}
