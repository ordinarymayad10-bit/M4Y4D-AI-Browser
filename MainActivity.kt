package com.m4yad.browser

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("m4yad_browser", Context.MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private val tabs = mutableListOf<WebView>()
    private var currentTab = 0
    private var incognito = false

    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var webContainer: FrameLayout
    private lateinit var tabLabel: TextView

    private val bg = Color.rgb(13, 17, 35)
    private val panel = Color.rgb(25, 31, 57)
    private val accent = Color.rgb(112, 92, 255)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        addTab("https://www.google.com")
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(10, 12, 10, 8)
        }

        val brand = TextView(this).apply {
            text = "✦  M4Y4D A!  •  AI BROWSER"
            setTextColor(white); textSize = 18f; gravity = Gravity.CENTER
            setPadding(4, 8, 4, 12)
        }
        root.addView(brand, matchWidth())

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(panel)
            setPadding(8, 4, 8, 4)
        }
        address = EditText(this).apply {
            hint = "Search or enter website"
            setHintTextColor(Color.LTGRAY); setTextColor(white)
            textSize = 15f; singleLine = true
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate(text.toString()); true
                } else false
            }
        }
        addressRow.addView(address, LinearLayout.LayoutParams(0, dp(48), 1f))
        addressRow.addView(button("GO") { navigate(address.text.toString()) })
        root.addView(addressRow, matchWidth())

        tabLabel = TextView(this).apply {
            setTextColor(Color.LTGRAY); textSize = 12f
            setPadding(5, 7, 5, 7)
        }
        root.addView(tabLabel, matchWidth())

        webContainer = FrameLayout(this)
        root.addView(webContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val row1 = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row1.addView(button("←") { if (activeWeb()?.canGoBack() == true) activeWeb()?.goBack() })
        row1.addView(button("→") { activeWeb()?.goForward() })
        row1.addView(button("↻") { activeWeb()?.reload() })
        row1.addView(button("+ Tab") { addTab("https://www.google.com") })
        row1.addView(button("Tabs") { showTabs() })
        root.addView(row1, matchWidth())

        val row2 = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        row2.addView(button("AI") { showAiChat() })
        row2.addView(button("Save") { saveBookmark() })
        row2.addView(button("Marks") { showBookmarks() })
        row2.addView(button("History") { showHistory() })
        row2.addView(button(if (incognito) "Private ✓" else "Private") { toggleIncognito() })
        row2.addView(button("⚙") { showSettings() })
        root.addView(row2, matchWidth())

        setContentView(root)
        updateTabLabel()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label; textSize = 11f; isAllCaps = false
            setTextColor(white); setBackgroundColor(panel)
            setPadding(5, 2, 5, 2)
            setOnClickListener { action() }
        }

    private fun activeWeb(): WebView? = tabs.getOrNull(currentTab)

    private fun createWebView(privateMode: Boolean): WebView {
        return WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = !privateMode
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            if (privateMode) {
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                clearCache(true)
                clearHistory()
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    if (view === activeWeb()) address.setText(url ?: "")
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (view === activeWeb()) address.setText(url ?: "")
                    if (!privateMode && !url.isNullOrBlank()) addHistory(url)
                }
            }
            webChromeClient = WebChromeClient()
            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setMimeType(mimeType)
                        .addRequestHeader("User-Agent", userAgent)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
                    (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    toast("Download started")
                } catch (_: Exception) { toast("Download could not start") }
            })
        }
    }

    private fun addTab(url: String) {
        val web = createWebView(incognito)
        tabs.add(web)
        currentTab = tabs.lastIndex
        webContainer.removeAllViews()
        webContainer.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.loadUrl(url)
        updateTabLabel()
    }

    private fun switchTab(index: Int) {
        if (index !in tabs.indices) return
        currentTab = index
        webContainer.removeAllViews()
        webContainer.addView(tabs[index], FrameLayout.LayoutParams(-1, -1))
        address.setText(tabs[index].url ?: "")
        updateTabLabel()
    }

    private fun showTabs() {
        if (tabs.isEmpty()) return
        val names = tabs.mapIndexed { i, w -> "${i + 1}. ${w.title ?: w.url ?: "New tab"}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Open tabs")
            .setItems(names) { _, which -> switchTab(which) }
            .setPositiveButton("New tab") { _, _ -> addTab("https://www.google.com") }
            .setNegativeButton("Close current") { _, _ ->
                if (tabs.size > 1) {
                    tabs.removeAt(currentTab).apply { stopLoading(); destroy() }
                    switchTab((currentTab - 1).coerceAtLeast(0))
                } else {
                    activeWeb()?.loadUrl("about:blank")
                }
            }.show()
    }

    private fun updateTabLabel() {
        tabLabel.text = "${if (incognito) "PRIVATE" else "NORMAL"}  •  ${tabs.size} tab(s)"
    }

    private fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val target = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + Uri.encode(value)
        }
        activeWeb()?.loadUrl(target)
    }

    private fun toggleIncognito() {
        incognito = !incognito
        tabs.forEach { it.stopLoading(); it.destroy() }
        tabs.clear()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        addTab("https://www.google.com")
        updateTabLabel()
        toast(if (incognito) "Private tab mode on" else "Private tab mode off")
    }

    private fun readList(key: String): JSONArray =
        try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }

    private fun writeList(key: String, list: JSONArray) {
        prefs.edit().putString(key, list.toString()).apply()
    }

    private fun addHistory(url: String) {
        val list = readList("history")
        if (list.length() == 0 || list.optString(list.length() - 1) != url) list.put(url)
        while (list.length() > 200) list.remove(0)
        writeList("history", list)
    }

    private fun saveBookmark() {
        val url = activeWeb()?.url ?: return
        val list = readList("bookmarks")
        if ((0 until list.length()).none { list.optString(it) == url }) list.put(url)
        writeList("bookmarks", list)
        toast("Bookmark saved")
    }

    private fun showSavedList(key: String, title: String) {
        val list = readList(key)
        if (list.length() == 0) { toast("Nothing saved yet"); return }
        val values = Array(list.length()) { list.optString(it) }.reversedArray()
        AlertDialog.Builder(this).setTitle(title).setItems(values) { _, which ->
            navigate(values[which])
        }.setNeutralButton("Clear") { _, _ ->
            writeList(key, JSONArray()); toast("$title cleared")
        }.show()
    }

    private fun showBookmarks() = showSavedList("bookmarks", "Bookmarks")
    private fun showHistory() = showSavedList("history", "History")

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 8, 22, 0)
        }
        val endpoint = EditText(this).apply {
            hint = "API endpoint (OpenAI-compatible)"
            setText(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions"))
        }
        val model = EditText(this).apply {
            hint = "Model name (e.g. gpt-4o-mini)"
            setText(prefs.getString("model", "gpt-4o-mini"))
        }
        val key = EditText(this).apply {
            hint = "API key"; setText(prefs.getString("api_key", ""))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        box.addView(endpoint); box.addView(model); box.addView(key)
        AlertDialog.Builder(this).setTitle("M4Y4D AI Settings")
            .setMessage("Set or change your AI provider details.")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("endpoint", endpoint.text.toString().trim())
                    .putString("model", model.text.toString().trim())
                    .putString("api_key", key.text.toString().trim()).apply()
                toast("AI settings saved")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showAiChat() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0)
        }
        val output = TextView(this).apply {
            text = "M4Y4D AI is ready. Ask a question."; setTextColor(white)
            setPadding(8, 8, 8, 8)
        }
        val scroll = ScrollView(this).apply { addView(output) }
        val input = EditText(this).apply { hint = "Ask M4Y4D AI..." }
        layout.addView(scroll, LinearLayout.LayoutParams(-1, dp(220)))
        layout.addView(input, matchWidth())
        val dialog = AlertDialog.Builder(this).setTitle("✦ M4Y4D AI")
            .setView(layout).setNegativeButton("Close", null)
            .setPositiveButton("Send", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) {
                    output.text = "Thinking...\n\n"
                    input.setText("")
                    askAi(question) { answer ->
                        runOnUiThread { output.text = answer }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun askAi(question: String, callback: (String) -> Unit) {
        val endpoint = prefs.getString("endpoint", "") ?: ""
        val model = prefs.getString("model", "") ?: ""
        val key = prefs.getString("api_key", "") ?: ""
        if (endpoint.isBlank() || model.isBlank() || key.isBlank()) {
            callback("Open Settings (⚙) and add your API endpoint, model and API key first.")
            return
        }
        executor.execute {
            var result = "AI request failed. Check endpoint, key and internet connection."
            var connection: HttpURLConnection? = null
            try {
                connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $key")
                val body = JSONObject().put("model", model)
                    .put("messages", JSONArray().put(JSONObject()
                        .put("role", "user").put("content", question)))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                result = if (connection.responseCode in 200..299) {
                    json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                } else "API error ${connection.responseCode}: ${json.optString("error", response)}"
            } catch (e: Exception) {
                result = "Error: ${e.message ?: "Could not connect"}"
            } finally { connection?.disconnect() }
            callback(result)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onBackPressed() {
        val web = activeWeb()
        if (web?.canGoBack() == true) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.stopLoading(); it.destroy() }
        executor.shutdown()
        super.onDestroy()
    }
}
