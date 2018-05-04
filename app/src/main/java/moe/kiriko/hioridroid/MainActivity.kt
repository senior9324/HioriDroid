package moe.kiriko.hioridroid

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

class MainActivity : Activity() {
    lateinit var mWebView: WebView
    lateinit var versionName: String
    lateinit var userLocale: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val locale: Locale
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = resources.configuration.locales.get(0)
        } else {
            locale = resources.configuration.locale
        }

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val supportLangs = resources.getStringArray(R.array.support_langs)
        if (!sharedPrefs.contains("user_locale")) {
            userLocale = locale.toLanguageTag().replace("-", "_")

            if (!supportLangs.contains(userLocale))
                userLocale = "en_US"
            val sharedPrefsEditor = sharedPrefs.edit()
            sharedPrefsEditor.putString("pref_key_locale", userLocale)
            sharedPrefsEditor.apply()
        } else {
            userLocale = sharedPrefs.getString("pref_key_locale", "en_US")
        }
        versionName = sharedPrefs.getString("version", "0.0.0")

        WebView.setWebContentsDebuggingEnabled(true)
        mWebView = findViewById(R.id.main_webview)
        mWebView.setWebViewClient(HioriWebViewClient(this))
        mWebView.apply {
            setWebChromeClient(WebChromeClient())
            settings.databaseEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.userAgentString = settings.userAgentString.replace("wv", "");
        }
        var intentTime: Long = 0
        mWebView.setOnTouchListener { v, event ->
            val action = event.actionMasked
            if (event.getPointerCount() > 1) {
                if (action == MotionEvent.ACTION_MOVE) {
                    if (intentTime + 1000 < Date().time) {
                        intentTime = Date().time
                        val settingsIntent = Intent(this, PreferencesActivity::class.java)
                        startActivity(settingsIntent)
                    }

                }
            }
            super.onTouchEvent(event)
        }

        val latestUpdate = sharedPrefs.getLong("update_time", 0);
        if (Date().time - 21600000 < latestUpdate) {
            runSC()
        } else {
            Toast.makeText(this, "Loading...", Toast.LENGTH_LONG).show()
            Thread {
                updateResource()
            }.start()
        }
    }

    fun updateResource() {
        try {
            val releaseConnection = URL("https://api.github.com/repos/shinycolors/hiori/releases").openConnection() as HttpURLConnection
            releaseConnection.connectTimeout = 6000
            releaseConnection.readTimeout = 8 * 6000

            val releaseInputStream = releaseConnection.inputStream
            var bufferedReader = BufferedReader(InputStreamReader(releaseInputStream))
            var releaseResult = ""
            var line: String? = null
            while ({ line = bufferedReader.readLine(); line }() != null) {
                releaseResult += line
            }
            releaseInputStream.close()

            val releaseList = JSONArray(releaseResult)
            val latestVersionName = releaseList.getJSONObject(0).getString("tag_name")
            if (latestVersionName != versionName) {
                //download latest resource if new version out
                //source codes are managed separately
                val assetDirectory = File(filesDir.absolutePath + "/assets/")
                deleteDireectory(assetDirectory)

                val imageDirectory = File(filesDir.absolutePath + "/assets/image/")
                imageDirectory.mkdirs()

                val imageListConnection = URL("https://api.github.com/repos/shinycolors/hiori/contents/src/modules/replacer/ui/" + userLocale.split("_")[0] + "/").openConnection() as HttpURLConnection
                imageListConnection.connectTimeout = 6000
                imageListConnection.readTimeout = 8 * 6000
                imageListConnection.connect()

                val imageListInputStream = imageListConnection.inputStream
                val imageListBufferedReader = BufferedReader(InputStreamReader(imageListInputStream))
                var imageListResult = ""
                line = null
                while ({ line = imageListBufferedReader.readLine(); line }() != null) {
                    imageListResult += line
                }
                imageListInputStream.close()

                runOnUiThread {
                    Toast.makeText(this, "Load Images...", Toast.LENGTH_LONG).show();
                }
                val imageList = JSONArray(imageListResult)
                for (i in 0..(imageList.length() - 1)) {
                    val image = imageList.getJSONObject(i)
                    val imageDownloadConnection = URL(image.getString("download_url")).openConnection() as HttpURLConnection
                    imageDownloadConnection.connectTimeout = 6000
                    imageDownloadConnection.readTimeout = 8 * 6000

                    val imageDownloadInputStream = imageDownloadConnection.inputStream
                    val imageFile = File(filesDir.absolutePath + "/assets/image/" + image.getString("name"))
                    val imageFileOutputStream = FileOutputStream(imageFile)

                    var buffer = ByteArray(8 * 1024)
                    var read = 0
                    while ({ read = imageDownloadInputStream.read(buffer, 0, buffer.size); read }() >= 0) {
                        imageFileOutputStream.write(buffer, 0, read)
                    }
                    imageDownloadInputStream.close()
                    imageFileOutputStream.close()
                }

                val dialogListConnection = URL("https://api.github.com/repos/shinycolors/imassc-translations/contents/" + userLocale.toLowerCase() + "/").openConnection() as HttpURLConnection
                dialogListConnection.connectTimeout = 6000
                dialogListConnection.readTimeout = 8 * 6000

                val dialogListInputStream = dialogListConnection.inputStream
                val dialogListBufferedReader = BufferedReader(InputStreamReader(dialogListInputStream))
                var dialogListResult = ""
                line = null
                while ({ line = dialogListBufferedReader.readLine(); line }() != null) {
                    dialogListResult += line
                }
                dialogListInputStream.close()

                val dialogList = JSONArray(dialogListResult)
                var injectsScript = "";

                val injectsScriptInputStream = assets.open("injects.js")
                val injectsScriptBufferedReader = BufferedReader(InputStreamReader(injectsScriptInputStream))
                line = null
                while ({ line = injectsScriptBufferedReader.readLine(); line }() != null) {
                    injectsScript += line + "\n"
                }

                runOnUiThread {
                    Toast.makeText(this, "Load Dialogs...", Toast.LENGTH_LONG).show();
                }
                for (i in 0..(dialogList.length() - 1)) {
                    val dialog = dialogList.getJSONObject(i)
                    var dialogName = dialog.getString("name")
                    if (dialogName.endsWith(".js")) continue;
                    dialogName = dialogName.split(".")[0]
                    val dialogDownloadConnection = URL(dialog.getString("download_url")).openConnection() as HttpURLConnection
                    dialogDownloadConnection.connectTimeout = 6000
                    dialogDownloadConnection.readTimeout = 8 * 6000

                    val dialogDownloadInputStream = dialogDownloadConnection.inputStream
                    val dialogDownloadBufferedReader = BufferedReader(InputStreamReader(dialogDownloadInputStream))
                    var dialogDownloadResult = ""
                    line = null
                    while ({ line = dialogDownloadBufferedReader.readLine(); line }() != null) {
                        dialogDownloadResult += line + "\n"
                    }
                    injectsScript += "tempMessages['" + dialogName + "'] = " + dialogDownloadResult + ";\nmessages.push(...tempMessages['" + dialogName + "'].messages);\n"
                }
                injectsScript += "function initHiori() {!!aoba?Hiori.init():setTimeout(3000, initHiori)} window.addEventListener('load', initHiori);";
                val injectsFile = File(filesDir.absolutePath + "/assets/injects.js")
                injectsFile.writeText(injectsScript)

                val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                val sharedPrefsEditor = sharedPrefs.edit()
                sharedPrefsEditor.putString("version", latestVersionName)
                sharedPrefsEditor.putLong("update_time", Date().time)
                sharedPrefsEditor.apply()

                runOnUiThread {
                    runSC()
                }
            } else {
                runOnUiThread {
                    runSC()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    fun deleteDireectory(file: File) {
        if (file.isDirectory()) {
            for (f in file.listFiles()) {
                deleteDireectory(f)
            }
        }
    }

    fun runSC() {
        mWebView.loadUrl("https://shinycolors.enza.fun")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    fun hideSystemUI() {
        var decorView = window.decorView;
        decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
