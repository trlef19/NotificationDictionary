/*
 * Copyright (c) 2024, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.LibraryDefaults
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.*
import java.io.File
import java.util.Locale


class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val sharedPref = applicationContext.getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val defaultDatabaseKey = getString(R.string.default_database)
        val selectedLanguageKey = getString(R.string.selected_language)
        var defaultLanguageValue = "en"
        var defaultDatabaseValue = "dictionary.db"
        var selectedLanguage = sharedPref.getString(selectedLanguageKey, "UNSET") as String
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
        createNotificationChannel()
        requestNotificationPermission()
        setContent {MainScreen()}
    }
    override fun onDestroy() {
        super.onDestroy()
        applicationContext.cacheDir.deleteRecursively() // Delete cache on exit
    }
    private fun requestNotificationPermission() {
        val notificationRequestCode = 11
        val notificationManager: NotificationManager= getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 32 && !notificationManager.areNotificationsEnabled()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequestCode)
        }
    }
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("Dictionary", name, importance)
        channel.description = descriptionText
        channel.setShowBadge(false)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainScreen() {
    var navigationSelectedItem by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    var expanded by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(false) }
    val activity = LocalContext.current
    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.primary),
                    title = { Text("Notification Dictionary") },
                    actions = {
                        Text(modifier = Modifier.padding(horizontal = 2.dp),
                            color= MaterialTheme.colorScheme.primary, text = "Switch Sound")
                        Switch(checked = soundOn, onCheckedChange = { soundOn = it })
                        Box(contentAlignment = Alignment.TopStart) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.Menu, contentDescription = "Switch Sound")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                offset = DpOffset(0.dp, 0.dp),
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.about)) },
                                    onClick = { activity.startActivity(Intent(activity, AboutActivity::class.java)) })
                                DropdownMenuItem(
                                    onClick = { /*LibrariesContainer()*/}, //todo new libraries activity
                                    text = { Text(stringResource(R.string.license)) })
                                DropdownMenuItem(
                                    onClick = { activity.startActivity(Intent(activity, HistoryActivity::class.java)) },
                                    text = { Text(stringResource(R.string.history)) })
                                DropdownMenuItem(
                                    onClick = { activity.startActivity(Intent(activity, FavouriteActivity::class.java)) },
                                    text = { Text(stringResource(R.string.favourite)) })
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    //getting the list of bottom navigation items for our data class
                    NavigationBar().bottomNavigationItems().forEachIndexed { index, navigationItem ->
                        //iterating all items with their respective indexes
                        NavigationBarItem(
                            selected = index == navigationSelectedItem,
                            label = { Text(navigationItem.label)},
                            icon = { Icon(navigationItem.icon, contentDescription = navigationItem.label) },
                            onClick = { navigationSelectedItem = index
                                navController.navigate(navigationItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InitializeLanguageMenu(Modifier.padding(10.dp,3.dp))
                ElevatedCard(//ElevatedCard for the first card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
                )
                {
                Text(
                    modifier = Modifier.padding(10.dp, 3.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Medium,
                    text = stringResource(id = R.string.info1_summary)
                )
                Text(
                    modifier = Modifier.padding(10.dp, 3.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Normal,
                    text = stringResource(id = R.string.info1_description)
                )
            }
            ElevatedCard(//ElevatedCard for the second card
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
            )
            {
                Text(
                    modifier = Modifier.padding(10.dp, 3.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Medium,
                    text = stringResource(id = R.string.info2_summary)
                )
                Text(
                    modifier = Modifier.padding(10.dp, 3.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Normal,
                    text = stringResource(id = R.string.info2_description)
                )
            }
                ElevatedCard(//ElevatedCard for the third card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
                )
                {
                    Text(
                        modifier = Modifier.padding(10.dp, 3.dp),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Medium,
                        text = stringResource(id = R.string.info3_summary)
                    )
                    Text(
                        modifier = Modifier.padding(10.dp, 3.dp),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Normal,
                        text = stringResource(id = R.string.info3_description)
                    )
                }
                ElevatedCard(//ElevatedCard for the fourth card
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
                )
                {
                    Text(
                        modifier = Modifier.padding(10.dp, 3.dp),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Medium,
                        text = stringResource(id = R.string.info4_summary)
                    )
                    Text(
                        modifier = Modifier.padding(10.dp, 3.dp),
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Normal,
                        text = stringResource(id = R.string.info4_description)
                    )
                }
        }
            NavHost(navController = navController, startDestination = Screens.Home.route, modifier = Modifier.padding(paddingValues = paddingValues)) {
            composable(Screens.Home.route) {
                LocalContext.current.startActivity(Intent(LocalContext.current, MainActivity::class.java))
            }
            composable(Screens.Search.route) {
                LocalContext.current.startActivity(Intent(LocalContext.current, SearchActivity::class.java))
            }
            composable(Screens.History.route) {
                LocalContext.current.startActivity(Intent(LocalContext.current, HistoryActivity::class.java))
            }
            composable(Screens.Favorites.route) {
                LocalContext.current.startActivity(Intent(LocalContext.current, FavouriteActivity::class.java))
            }
        }
            }
        }


}
@Composable
private fun InitProgressIndicator() {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth())
    {
        Text("Downloading database for initial offline usage")
        Button(onClick = { loading = true
            scope.launch {
                loadProgress { progress -> currentProgress = progress }
                loading = false // Reset loading when the coroutine finishes
            }
        }, enabled = !loading) {
            Text("Download")
        }
        LinearProgressIndicator(progress = {currentProgress}, modifier = Modifier.fillMaxWidth())
    }
}
suspend fun loadProgress(updateProgress: (Float) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100)
        delay(100)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitializeLanguageMenu(modifier : Modifier) {
    val languageMenuExpanded = remember { mutableStateOf(false) }
    val languages = listOf("English", "French", "German", "Polish")
    var text by remember { mutableStateOf(languages[0]) }
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = languageMenuExpanded.value,
        onExpandedChange = { languageMenuExpanded.value = it }) {
        TextField(
            modifier = Modifier.menuAnchor(),
            value = text,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Languages") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded.value) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer, focusedContainerColor = MaterialTheme.colorScheme.inversePrimary)
        )
        ExposedDropdownMenu(
            expanded = languageMenuExpanded.value,
            onDismissRequest = { languageMenuExpanded.value = false },
        ) {
            languages.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        text = option
                        languageMenuExpanded.value = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
