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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import java.util.concurrent.Executors

class HistoryProvider: PreviewParameterProvider<List<HistoryDao.WordWithMeaning>> {
    override val values = sequenceOf(
        listOf(
            HistoryDao.WordWithMeaning(
                word = "History",
                definition = "Something of the past"
            )
        )
    )
}
class HistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())


        executor.execute {
            val database = AppDatabase.getDatabase(this)
            val historyDao = database.historyDao()
            val entries = historyDao.getAllEntriesWithMeaning()

            handler.post {
                setContent{
                    HistoryContent(entries)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun HistoryContent(@PreviewParameter(HistoryProvider::class) wordsHistory: List<HistoryDao.WordWithMeaning>){
    var navigationSelectedItem by remember {mutableIntStateOf(3)}
    val navController = rememberNavController()
    MaterialTheme {
        Scaffold( topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }, bottomBar = {
            NavigationBar {
                NavigationBar().bottomNavigationItems().forEachIndexed { index, navigationItem ->
                    NavigationBarItem(
                        selected = index == navigationSelectedItem,
                        label = { Text(navigationItem.label)},
                        icon = { Icon(navigationItem.icon, contentDescription = navigationItem.label) },
                        onClick = { navigationSelectedItem = index
                            navController.navigate(navigationItem.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }){ innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(wordsHistory) { favorite ->
                    ElevatedCard(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)){
                        favorite.word?.let {
                            Text(modifier = Modifier.padding(10.dp, 3.dp), fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = FontWeight.Medium, text = it) }
                        favorite.definition?.let {
                            Text(modifier = Modifier.padding(10.dp, 3.dp), text = it) }
                    }
                }
            }
        }
    }
}