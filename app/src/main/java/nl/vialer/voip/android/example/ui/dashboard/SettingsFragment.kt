package nl.vialer.voip.android.example.ui.dashboard

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.preference.*
import com.android.volley.Request
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.vialer.voip.android.R
import nl.vialer.voip.android.VoIPPIL
import nl.vialer.voip.android.example.ui.VoIPGRIDMiddleware
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject

class SettingsFragment : PreferenceFragmentCompat() {

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(activity)
    }

    private val voip by lazy { VoIPPIL.instance }

    private val voIPGRIDMiddleware by lazy { VoIPGRIDMiddleware(requireActivity()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference<EditTextPreference>("username")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            prefs.getString("username", "")
        }

        findPreference<Preference>("voipgrid_middleware_token")?.summaryProvider = Preference.SummaryProvider<Preference> {
            VoIPGRIDMiddleware.token
        }

        findPreference<EditTextPreference>("voipgrid_username")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            prefs.getString("voipgrid_username", "")
        }

        findPreference<EditTextPreference>("password")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { prefs.getString(
                "password",
                ""
            ) }
        }

        findPreference<EditTextPreference>("voipgrid_password")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { prefs.getString(
                "voipgrid_password",
                ""
            ) }
        }

        findPreference<EditTextPreference>("domain")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_TEXT_VARIATION_URI
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { prefs.getString(
                "domain",
                ""
            ) }

        }

        findPreference<EditTextPreference>("port")?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> { prefs.getString(
                "port",
                ""
            ) }
        }

        arrayOf("username", "password", "domain", "port").forEach {
            findPreference<EditTextPreference>(it)?.setOnPreferenceChangeListener { _, _ ->
                Handler().postDelayed({
                    activity?.runOnUiThread { updateAuthenticationStatus() }
                }, 1000)
                true
            }
        }

        arrayOf("voipgrid_username", "voipgrid_password").forEach {
            findPreference<EditTextPreference>(it)?.setOnPreferenceChangeListener { _, _ ->
                Handler().postDelayed({
                    activity?.runOnUiThread { updateVoipgridAuthenticationStatus() }
                }, 1000)
                true
            }
        }

        findPreference<Preference>("status")?.setOnPreferenceClickListener {
            updateAuthenticationStatus()
            true
        }

        findPreference<Preference>("voipgrid_middleware_register")?.setOnPreferenceClickListener {
            GlobalScope.launch {
                val message = if (voIPGRIDMiddleware.register()) "Registered!" else "Registration failed..."
                requireActivity().runOnUiThread { Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show() }
            }
            true
        }

        findPreference<Preference>("voipgrid_middleware_unregister")?.setOnPreferenceClickListener {
            GlobalScope.launch {
                val message = if (voIPGRIDMiddleware.unregister()) "Unregistered!" else "Unregistration failed..."
                requireActivity().runOnUiThread { Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show() }
            }
            true
        }
    }

    private fun updateVoipgridAuthenticationStatus() {
        val queue = Volley.newRequestQueue(requireActivity())

        val url = "https://partner.voipgrid.nl/api/permission/apitoken/"

        val requestData = JSONObject().apply {
            put("email", prefs.getString("voipgrid_username", ""))
            put("password", prefs.getString("voipgrid_password", ""))
        }

        val request = JsonObjectRequest(Request.Method.POST, url, requestData, { response ->
            val apiToken = response.getString("api_token")
            updateVoipgridSummary(true, apiToken)
            prefs.edit().putString("voipgrid_api_token", apiToken).apply()
        }, { error ->
            Toast.makeText(
                requireContext(),
                error.networkResponse.statusCode.toString(),
                Toast.LENGTH_LONG
            ).show()
            updateVoipgridSummary(false)
            prefs.edit().remove("voipgrid_api_token").apply()
        }
        )

        queue.add(request)
    }

    private fun updateVoipgridSummary(authenticated: Boolean, token: String? = null) {
        val summary = if (authenticated) "Authenticated (${token})" else "Authentication failed"

        activity?.runOnUiThread {
            findPreference<Preference>("voipgrid_status")?.summaryProvider = Preference.SummaryProvider<Preference> {
                summary
            }

            findPreference<Preference>("voipgrid_middleware_register")?.isEnabled = authenticated
            findPreference<Preference>("voipgrid_middleware_unregister")?.isEnabled = authenticated
        }
    }

    /**
     * Updates the authentication status field.
     *
     */
    private fun updateAuthenticationStatus() {
        findPreference<Preference>("status")?.summaryProvider = Preference.SummaryProvider<Preference> {
            "Checking authentication..."
        }

        GlobalScope.launch(Dispatchers.IO) {
            val summary = if (voip.canAuthenticate()) "Authenticated" else "Authentication failed"

            activity?.runOnUiThread {
                findPreference<Preference>("status")?.summaryProvider = Preference.SummaryProvider<Preference> {
                    summary
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAuthenticationStatus()
        updateVoipgridAuthenticationStatus()
    }
}