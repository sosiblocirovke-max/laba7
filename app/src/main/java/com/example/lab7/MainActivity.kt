package com.example.lab7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab7.data.local.AppDatabase
import com.example.lab7.data.remote.NetworkModule
import com.example.lab7.data.repository.PostRepository
import com.example.lab7.data.repository.UserRepository
import com.example.lab7.ui.theme.Lab7Theme
import com.example.lab7.ui.users.UserDetailScreen
import com.example.lab7.ui.users.UserDetailViewModel
import com.example.lab7.ui.users.UserDetailViewModelFactory
import com.example.lab7.ui.users.UsersScreen
import com.example.lab7.ui.users.UsersViewModel
import com.example.lab7.ui.users.UsersViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab7Theme {
                val context = LocalContext.current
                val userRepository = remember {
                    val app = context.applicationContext
                    UserRepository(
                        NetworkModule.create(),
                        AppDatabase.getInstance(app).userDao()
                    )
                }
                val postRepository = remember {
                    val app = context.applicationContext
                    PostRepository(
                        NetworkModule.create(),
                        AppDatabase.getInstance(app).postDao()
                    )
                }
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "users",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("users") {
                        val usersViewModel: UsersViewModel = viewModel(
                            factory = UsersViewModelFactory(userRepository, postRepository)
                        )
                        UsersScreen(
                            viewModel = usersViewModel,
                            onUserClick = { userId ->
                                navController.navigate("user/$userId")
                            }
                        )
                    }
                    composable(
                        route = "user/{id}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType }
                        )
                    ) { entry ->
                        val userId = entry.arguments?.getInt("id") ?: return@composable
                        val detailViewModel: UserDetailViewModel = viewModel(
                            key = "user_detail_$userId",
                            factory = UserDetailViewModelFactory(
                                userRepository,
                                postRepository,
                                userId
                            )
                        )
                        UserDetailScreen(
                            viewModel = detailViewModel,
                            onBack = { navController.popBackStack() },
                            onUserDeleted = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
