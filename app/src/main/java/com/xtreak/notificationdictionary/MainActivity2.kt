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

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screens(val route : String) {
    object Home : Screens("home_route")
    object Search : Screens("search_route")
    object Favorites : Screens("favorite_route")
    object History : Screens("history_route")
}

class MainActivity2: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }
}
data class BottomNavigationItem(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Home,
    val route : String = ""
) {

    //function to get the list of bottomNavigationItems
    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                label = "Home",
                icon = Icons.Filled.Home,
                route = Screens.Home.route
            ),
            BottomNavigationItem(
                label = "Search",
                icon = Icons.Filled.Search,
                route = Screens.Search.route
            ),
            BottomNavigationItem(
                label = "Favorites",
                icon = Icons.Filled.Star,
                route = Screens.Favorites.route
            ),
            BottomNavigationItem(
                label = "History",
                icon = Icons.Filled.DateRange,
                route = Screens.History.route
            )
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainScreen() {
    var navigationSelectedItem by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    MaterialTheme {
        Scaffold(topBar = {
                CenterAlignedTopAppBar(
                    colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.primary),
                    title = { Text("Notification Dictionary") }
                )
            },
            bottomBar = {
                NavigationBar {
                    //getting the list of bottom navigation items for our data class
                    BottomNavigationItem().bottomNavigationItems().forEachIndexed {index,navigationItem ->
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
            ElevatedCard(//ElevatedCard for the first card
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(paddingValues).padding(20.dp, 10.dp).fillMaxWidth(),
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
        }
            NavHost(navController = navController, startDestination = Screens.Home.route, modifier = Modifier.padding(paddingValues = paddingValues)) {
            composable(Screens.Home.route) {
                LocalContext.current.startActivity(Intent(LocalContext.current, MainActivity2::class.java))
            }
            composable(Screens.Search.route) {

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
