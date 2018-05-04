package moe.kiriko.hioridroid

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.webkit.CookieManager
import android.widget.Toast

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, PreferencesFragment()).commit()
    }

    class PreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)

            val localePreference = findPreference("pref_key_locale")
            localePreference.setOnPreferenceChangeListener { preference, newValue ->
                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val sharedPrefsEditor = sharedPrefs.edit()
                sharedPrefsEditor.putLong("update_time", 0)//force update resources
                sharedPrefsEditor.apply()
                true
            }

            val clearCookies = findPreference("pref_key_clear_cookies")
            clearCookies.setOnPreferenceClickListener {
                CookieManager.getInstance().removeAllCookies(null)
                Toast.makeText(activity, "Cookie cleared", Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}
