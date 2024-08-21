/*
 * Copyright (c) 2021, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.huxq17.download.Pump
import com.huxq17.download.config.DownloadConfig
import com.huxq17.download.core.DownloadListener
import com.mikepenz.aboutlibraries.LibsBuilder
import com.suddenh4x.ratingdialog.AppRating
import com.suddenh4x.ratingdialog.preferences.RatingThreshold
import de.cketti.library.changelog.ChangeLog
import io.sentry.Sentry
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.zip.ZipFile


class MainActivityOld : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog
    private val channelId = "Dictionary"
    private val notificationRequestCode = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = applicationContext.getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val defaultDatabaseKey = getString(R.string.default_database)
        val selectedLanguageKey = getString(R.string.selected_language)
        var defaultLanguageValue = "en"
        var defaultDatabaseValue = "dictionary.db"
        val selectedTheme = "selected_theme"
        var selectedLanguage = sharedPref.getString(selectedLanguageKey, "UNSET") as String
        val theme = sharedPref.getInt(selectedTheme, R.style.Theme_NotificationDictionary)
        // https://stackoverflow.com/questions/4212320/get-the-current-language-in-device
        val currentLocale = Locale.getDefault().language
        // On first run if the current locale is one of supported language then use it for
        // better onboarding experience. Example french users on start will have french selected.
        // Current locale might be fr but user might have selected english. In that case check for
        // preference to be UNSET
        if (selectedLanguage == "UNSET") {
            if (currentLocale.startsWith("fr", ignoreCase = true)) {
                defaultLanguageValue = currentLocale
                defaultDatabaseValue = "dictionary_fr.db"
            } else if (currentLocale.startsWith("de", ignoreCase = true)) {
                defaultLanguageValue = currentLocale
                defaultDatabaseValue = "dictionary_de.db"
            } else if (currentLocale.startsWith("pl", ignoreCase = true)) {
                defaultLanguageValue = currentLocale
                defaultDatabaseValue = "dictionary_pl.db"
            }
            // Set values here so that
            with(sharedPref.edit()) {
                putString(defaultDatabaseKey, defaultDatabaseValue)
                putString(selectedLanguageKey, defaultLanguageValue)
                apply()
                commit()
            }
        }

        selectedLanguage = sharedPref.getString(selectedLanguageKey, defaultLanguageValue) as String
        val databaseName = sharedPref.getString(defaultDatabaseKey, defaultDatabaseValue) as String
        val packageDataDirectory = Environment.getDataDirectory().absolutePath + "/data/" + packageName
        val file = File("$packageDataDirectory/databases/$databaseName")
        if (theme == R.style.Theme_NotificationDictionary)
            setTheme(R.style.Theme_NotificationDictionary)
        else
            setTheme(R.style.Theme_NotificationDictionary_Dark)

        setContentView(R.layout.activity_main)
        setLocale(selectedLanguage)
        setIMEAction()
        createNotificationChannel()

        if (!file.exists())
            initialize_database(databaseName)

        val mRecyclerView = findViewById<RecyclerView>(R.id.meaningRecyclerView)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = linearLayoutManager

        val mListAdapter = RoomAdapter(
            listOf(
                Word(1, "", getString(R.string.info1_summary), 1, 1, getString(R.string.info1_description)),
                Word(1, "", getString(R.string.info2_summary), 1, 1, getString(R.string.info2_description)),
                Word(1, "", getString(R.string.info3_summary), 1, 1, getString(R.string.info3_description)),
                Word(1, "", getString(R.string.info4_summary), 1, 1, getString(R.string.info4_description)),
                ), this
            )
        mRecyclerView.adapter = mListAdapter

        initializeSpinner(databaseName)
        // show_changelog()
        show_rating()
        onNewIntent(intent)
        requestNotificationPermission()
    }

    private fun setIMEAction() {
        val wordEdit = findViewById<EditText>(R.id.wordInput)
        wordEdit.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                sendMessage(v)
                return@OnEditorActionListener true
            }
            false
        })
    }

    fun initializeSpinner(databaseName: String) {
        val spinner = findViewById<View>(R.id.spinner) as Spinner
        val languages = arrayOf("English", "French", "German", "Polish")
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this@MainActivityOld,
            android.R.layout.simple_spinner_item, languages
        )


        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set spinner selection after setting adapter. https://stackoverflow.com/a/1484546/2610955
        // Pass animated as false so that callback is not triggered. https://stackoverflow.com/a/17336944/2610955
        if (databaseName == "dictionary_fr.db")
            spinner.setSelection(1, false)
        else if (databaseName == "dictionary_de.db")
            spinner.setSelection(2, false)
        else if (databaseName == "dictionary_pl.db")
            spinner.setSelection(3, false)
        else
            spinner.setSelection(0, false)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            //https://stackoverflow.com/questions/31497712/get-previously-selected-item-from-spinner-onitemselectedlistener-event
            var previous: Int = spinner.selectedItemPosition
            var startup_selected = spinner.selectedItem

            // https://stackoverflow.com/questions/5124835/spinner-onitemselected-called-erroneously-without-user-action/10102356#10102356
            // Show dialog initially. Then on clicking no set it false so that it doesn't trigger next time during which reset to true.
            var show_dialog: Boolean = true

            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View?,
                arg2: Int,
                arg3: Long
            ) {
                // animate false doesn't work in oreo. So compare selection and don't trigger
                // This handles startup dialog issue. Then set previous_selected as null so that
                // it's not used for later stages in app lifecycle
                val current_item = spinner.selectedItem
                if (current_item == startup_selected) {
                    startup_selected = null
                    return
                }

                if (show_dialog) {
                    val item = spinner.selectedItem.toString()
                    AlertDialog.Builder(this@MainActivityOld)
                        .setTitle(getString(R.string.confirmation))
                        .setMessage(getString(R.string.change_confirmation))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                val sharedPref = applicationContext.getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
                                val default_database_key = getString(R.string.default_database)
                                val selected_language_key = getString(R.string.selected_language)
                                var database_name = "database_en.db"
                                var selected_language = "en"
                                // TODO: Need to organize mapping somewhere. This is not scalable on introducing new languages.
                                if (item == "English") {
                                    database_name = "dictionary.db"
                                    selected_language = "en"
                                    setLocale("en")
                                } else if (item == "French") {
                                    database_name = "dictionary_fr.db"
                                    selected_language = "fr"
                                    setLocale("fr")
                                } else if (item == "German") {
                                    database_name = "dictionary_de.db"
                                    selected_language = "de"
                                } else if (item == "Polish") {
                                    database_name = "dictionary_pl.db"
                                    selected_language = "pl"
                                }

                                with(sharedPref.edit()) {
                                    putString(default_database_key, database_name)
                                    putString(selected_language_key, selected_language)
                                    apply()
                                    commit()
                                }

                                // As soon as the preference is changed if the file doesn't exist then download
                                val package_data_directory = Environment.getDataDirectory().absolutePath + "/data/" + packageName
                                val file = File("$package_data_directory/databases/$database_name")

                                if (!file.exists())
                                    initialize_database(database_name)
                                previous = spinner.selectedItemPosition
                            }
                        )
                        .setNegativeButton(android.R.string.no,
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                spinner.setSelection(previous, false)
                                show_dialog = false
                            }).show()
                } else {
                    show_dialog = true
                }

            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    // https://stackoverflow.com/questions/2900023/change-app-language-programmatically-in-android
    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = this.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Setting locale changes the values only on startup. We need to call
        // recreate() but it can end in a loop as we do it in app startup.
        // Call these manually to refresh but this needs a fix.
        val wordEdit = findViewById<EditText>(R.id.wordInput)
        val searchButton = findViewById<TextView>(R.id.searchButton)
        val word: String? = intent?.extras?.getString("NotificationWord")

        searchButton.text = getString(R.string.search)
    }

    /*fun show_changelog() {
        val changelog = ChangeLog(this)
        if (changelog.isFirstRun) {
            changelog.logDialog.show()
        }
    }*/

    fun show_rating() {
        AppRating.Builder(this)
            .setMinimumLaunchTimes(10)
            .setMinimumDays(2)
            .setMinimumLaunchTimesToShowAgain(15)
            .setMinimumDaysToShowAgain(10)
            .setRatingThreshold(RatingThreshold.FIVE)
            .showIfMeetsConditions()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)

        val switchSoundItem = menu!!.findItem(R.id.switch_sound)
        val soundView = MenuItemCompat.getActionView(switchSoundItem)
        val sharedPref = applicationContext.getSharedPreferences(
            getString(R.string.preference_file_key), MODE_PRIVATE
        )

        val switch_sound = soundView.findViewById<View>(R.id.sound_switch_button) as Switch
        var switch_sound_value = sharedPref.getBoolean(
            "read_definition",
            false
        )

        switch_sound.isChecked = switch_sound_value


        // https://stackoverflow.com/questions/32091709/how-to-get-set-action-event-in-android-actionbar-switch
        // https://stackoverflow.com/questions/8811594/implementing-user-choice-of-theme
        // https://stackoverflow.com/questions/2482848/how-to-change-current-theme-at-runtime-in-android
        // recreate needs to be called as per stackoverflow answers after initial theme is set though it's not documented.
        switch_sound.setOnClickListener { buttonView ->
            val sound_button = findViewById<View>(R.id.sound_switch_button) as Switch
            switch_sound_value = !switch_sound_value
            with(sharedPref.edit()) {
                putBoolean("read_definition", switch_sound_value)
                apply()
                commit()
            }

            sound_button.isChecked = switch_sound_value
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_us -> {
                val about_activity = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(about_activity, 0)
            }
            R.id.license -> {
                LibsBuilder().withActivityTitle("Open Source Licenses").withLicenseShown(true).start(this)
            }
            R.id.history -> {
                val history_activity = Intent(applicationContext, HistoryActivity::class.java)
                startActivityForResult(history_activity, 0)
            }
            R.id.favourite -> {
                val favourite_activity = Intent(applicationContext, FavouriteActivity::class.java)
                startActivityForResult(favourite_activity, 0)
            }
        }
        return true
    }//TODO DONE


    private fun initProgressDialog(): ProgressDialog {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Downloading database for initial offline usage")
        progressDialog.progress = 0
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(false)
        return progressDialog
    }//TODO DONE

    private fun initialize_database(database_name: String) {
        // declare the dialog as a member field of your activity
        // ProgressDialog is deprecated in documentation to use ProgressBar.
        // But we don't want the user to cancel this. It's one time and takes a couple of seconds

        // TODO: Make this configurable based on environment?
        val url = "https://xtreak.sfo3.cdn.digitaloceanspaces.com/dictionaries/v2/$database_name.zip"
        // val url = "http://192.168.0.105:8000/$database_name.zip" // for local mobile testing
        // val url = "http://10.0.2.2:8000/$database_name.zip" // for local emulator testing

        val progressDialog = initProgressDialog()
        val package_data_directory = Environment.getDataDirectory().absolutePath + "/data/" + packageName
        val zip_path = File("$package_data_directory/$database_name.zip").absolutePath

        // https://github.com/huxq17/Pump/blob/master/kotlin_app/src/main/java/com/huxq17/download/demo/MainActivity.kt
        DownloadConfig.newBuilder().setMaxRunningTaskNum(1).setMinUsableStorageSpace(140 * 1024L * 1024) // 140MB as per database size
            .build()
        progressDialog.progress = 0
        progressDialog.show()
        Pump.newRequest(url, zip_path)
            .listener(object : DownloadListener() {

                override fun onProgress(progress: Int) {
                    progressDialog.progress = progress
                }

                fun copy_and_unzip(source: String, destination: String) {
                    val zipfile = ZipFile(source)
                    val entry = zipfile.entries().toList().first()

                    // The zip file only has one entry which is the database. So use it as an
                    // input stream and copy the unzipped file to output stream. Delete the source
                    // zip file to save space.
                    val input_stream = zipfile.getInputStream(entry)
                    val output_stream = FileOutputStream(destination)
                    input_stream.copyTo(output_stream, 1024 * 1024 * 2)
                    File(zip_path).delete()
                }

                override fun onSuccess() {
                    val destination_folder = File("$package_data_directory/databases")
                    val destination_path = File("$package_data_directory/databases/$database_name").absolutePath
                    val source_path = downloadInfo.filePath

                    if (!destination_folder.exists())
                        destination_folder.mkdirs()

                    copy_and_unzip(source_path, destination_path)
                    progressDialog.dismiss()
                    Snackbar.make(findViewById(R.id.mainLayout), "Download finished", Snackbar.LENGTH_SHORT).show()
                }

                override fun onFailed() {
                    progressDialog.dismiss()
                    Snackbar.make(
                        findViewById(R.id.mainLayout),
                        "Download failed. Please check your internet connection and relaunch the app.",
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            })
            .forceReDownload(false)
            .threadNum(3)
            .setRetry(3, 200)
            .submit()
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.cacheDir.deleteRecursively() // Delete cache on exit
    }

    override fun onNewIntent(intent: Intent) {
        // Launch the activity from notification with word filled if present.
        // Fresh start of app won't have NotificationWord value since it's only
        // set as part of notification creation.
        super.onNewIntent(intent)
        val extras = intent.extras

        if (extras != null) {
            val word = extras.getString("NotificationWord")
            if (word != null) {
                val wordEdit = findViewById<EditText>(R.id.wordInput)
                val searchButton = findViewById<TextView>(R.id.searchButton)

                // Fill the text box with word and emulate click to get all meanings
                wordEdit.setText(word)
                searchButton.performClick()
            }
        }
    }
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }//TODO DONE

    private fun requestNotificationPermission() {
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 33 && !notificationManager.areNotificationsEnabled()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationRequestCode
            )
        }
    } //TODO DONE

    fun sendMessage(view: View) {
        val wordEdit = findViewById<EditText>(R.id.wordInput)
        // https://stackoverflow.com/questions/18414804/android-edittext-remove-focus-after-clicking-a-button
        wordEdit.clearFocus()
        val word = wordEdit.text.toString().trim().lowercase()
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val dao = database.dictionaryDao()
            val historyDao = database.historyDao()
            var meanings: List<Word>

            try {
                meanings = dao.getAllMeaningsByWord(word)
                if (meanings.isNotEmpty()) {
                    addHistoryEntry(historyDao, word)
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                Log.d("ndict:", e.toString())
                meanings = listOf(
                    Word(
                        1, "", "Error", 1, 1,
                        "There was an error while trying to fetch the meaning. The app tries to download the database at first launch for offline usage." +
                                "The error usually occurs if the database was not downloaded properly due to network issue during start or changing language." +
                                "Please turn on your internet connection and restart the app to download the database."
                    )
                )
            }

            try {
                resolveRedirectMeaning(meanings, dao)
            } catch (e: Exception) {
                Sentry.captureException(e)
            }

            handler.post {
                val mRecyclerView = findViewById<RecyclerView>(R.id.meaningRecyclerView)
                var mListadapter = RoomAdapter(listOf(Word(1, "", "Unknown", 1, 1, "No meaning found")), this)

                if (meanings.isNotEmpty())
                    mListadapter = RoomAdapter(meanings, this)

                mRecyclerView.adapter = mListadapter
                mListadapter.notifyItemRangeChanged(1, 100)
            }
        }
    }
}