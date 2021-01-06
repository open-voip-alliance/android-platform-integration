package org.openvoipalliance.androidplatformintegration

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.openvoipalliance.androidplatformintegration.audio.AudioManager
import org.openvoipalliance.androidplatformintegration.call.CallActions
import org.openvoipalliance.androidplatformintegration.call.PILCall
import org.openvoipalliance.androidplatformintegration.call.PILCallFactory
import org.openvoipalliance.androidplatformintegration.configuration.ApplicationSetup
import org.openvoipalliance.androidplatformintegration.configuration.Auth
import org.openvoipalliance.androidplatformintegration.configuration.Preferences
import org.openvoipalliance.androidplatformintegration.di.di
import org.openvoipalliance.androidplatformintegration.events.EventsManager
import org.openvoipalliance.androidplatformintegration.events.PILEventListener
import org.openvoipalliance.androidplatformintegration.exception.NoAuthenticationCredentialsException
import org.openvoipalliance.androidplatformintegration.exception.PermissionException
import org.openvoipalliance.androidplatformintegration.logging.LogLevel
import org.openvoipalliance.androidplatformintegration.telecom.AndroidCallFramework
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.RegistrationState.FAILED
import org.openvoipalliance.phonelib.model.RegistrationState.REGISTERED
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PIL internal constructor(internal val app: ApplicationSetup) {

    private val callFactory: PILCallFactory by di.koin.inject()
    private val callManager: CallManager by di.koin.inject()
    private val androidCallFramework: AndroidCallFramework by di.koin.inject()

    private val phoneLib: PhoneLib by di.koin.inject()
    private val phoneLibHelper: PhoneLibHelper by di.koin.inject()

    val actions: CallActions by di.koin.inject()
    val audio: AudioManager by di.koin.inject()
    val events: EventsManager by di.koin.inject()

    var preferences: Preferences = Preferences.DEFAULT
        set(preferences) {
            field = preferences
            if (!phoneLib.isInitialised) return

            start(forceInitialize = true, forceReregister = true)
        }

    var auth: Auth? = null
        set(auth) {
            if (auth?.isValid != true) {
                writeLog("Attempting to set an invalid auth object", LogLevel.ERROR)
                return
            }

            field = auth
            start(forceInitialize = false, forceReregister = true)
        }

    val call: PILCall?
        get() = run {
            if (isInTransfer) {
                return@run callFactory.make(callManager.transferSession?.to)
            }

            callManager.call?.let { callFactory.make(it) }
        }

    val transferCall: PILCall?
        get() = callManager.transferSession?.let { callFactory.make(it.from) }

    val isInTransfer: Boolean
        get() = callManager.transferSession != null

    init {
        instance = this

        arrayOf<PILEventListener>(callFactory).forEach {
            events.listen(it)
        }
    }

    /**
     * Attempt to boot and register to see if user credentials are correct. This can be used
     * to check the user's credentials after they have supplied them.
     *
     */
    suspend fun performRegistrationCheck(): Boolean = suspendCoroutine { continuation ->
        try {
            if (auth?.isValid == false) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            phoneLibHelper.initialise()
            phoneLib.register { registrationState ->
                when (registrationState) {
                    REGISTERED, FAILED -> {
                        continuation.resume(registrationState == REGISTERED)
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            continuation.resume(false)
        }
    }

    /**
     * Place a call to the given number, this will also boot the voip library
     * if it is not already booted.
     *
     */
    fun call(number: String) {
        performPermissionCheck()

        start {
            androidCallFramework.placeCall(number)
        }
    }

    /**
     * Start the PIL, unless the force options are provided, the method will not restart or re-register.
     *
     */
    fun start(
        forceInitialize: Boolean = false,
        forceReregister: Boolean = false,
        callback: (() -> Unit)? = null
    ) {
        val auth = auth ?: throw NoAuthenticationCredentialsException()

        if (!auth.isValid) throw NoAuthenticationCredentialsException()

        if (forceInitialize) {
            phoneLib.destroy()
        }

        phoneLibHelper.apply {
            initialise(forceInitialize)
            register(auth, forceReregister) {
                writeLog("The VoIP library has been initialised and the user has been registered!")
                callback?.invoke()
            }
        }
    }

    private fun performPermissionCheck() {
        if (ContextCompat.checkSelfPermission(app.application, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_DENIED) {
            throw PermissionException(Manifest.permission.CALL_PHONE)
        }
    }

    internal fun writeLog(message: String, level: LogLevel = LogLevel.INFO) {
        app.logger?.onLogReceived(message, level)
    }

    companion object {
        lateinit var instance: PIL

        /**
         * Check whether the PIL has been initialized, this should only be needed when calling the PIL
         * from a service class that may run before your application onCreate method.
         *
         */
        val isInitialized: Boolean
            get() = ::instance.isInitialized
    }
}
