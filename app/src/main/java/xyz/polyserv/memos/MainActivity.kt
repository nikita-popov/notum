package xyz.polyserv.memos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import xyz.polyserv.memos.data.model.Memo
import xyz.polyserv.memos.presentation.ui.screens.CreateMemoScreen
import xyz.polyserv.memos.presentation.ui.screens.MemoDetailScreen
import xyz.polyserv.memos.presentation.ui.screens.MemoListScreen
import xyz.polyserv.memos.presentation.ui.screens.SettingsScreen
import xyz.polyserv.memos.presentation.ui.theme.MemosTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContent {
            MemosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "memos_list"
                    ) {
                        composable("memos_list") {
                            MemoListScreen(
                                onMemoClick = { memo ->
                                    val encodedId = URLEncoder.encode(memo.id, StandardCharsets.UTF_8.toString())
                                    navController.navigate("memo_detail/$encodedId")
                                },
                                onCreateClick = {
                                    navController.navigate("create_memo")
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }

                        composable("create_memo") {
                            CreateMemoScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "memo_detail/{memoId}",
                            arguments = listOf(
                                navArgument("memoId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val memoId = backStackEntry.arguments?.getString("memoId") ?: return@composable
                            MemoDetailScreen(
                                memo = Memo(id = memoId, content = ""),
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditClick = { memo ->
                                    navController.navigate("edit_memo/${memo.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
