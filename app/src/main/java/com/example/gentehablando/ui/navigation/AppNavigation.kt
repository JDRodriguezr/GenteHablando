package com.example.gentehablando.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gentehablando.model.UserRole
import com.example.gentehablando.ui.screens.LoginScreen
import com.example.gentehablando.ui.screens.GuidesScreen
import com.example.gentehablando.viewmodel.AuthViewModel
import com.example.gentehablando.ui.screens.ChatScreen
import com.example.gentehablando.ui.screens.FeedbackScreen
import com.example.gentehablando.ui.screens.HomeScreen
import com.example.gentehablando.ui.screens.MatchmakingScreen
import com.example.gentehablando.ui.screens.RoleGuidelinesScreen
import com.example.gentehablando.viewmodel.ChatViewModel
import com.example.gentehablando.viewmodel.MatchmakingViewModel
import com.example.gentehablando.utils.AdManager
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R

object Destinations {
    const val HOME = "home" // i18n: technical Navigation route or argument key; never displayed.
    const val GUIDELINES = "guidelines" // i18n: technical Navigation route or argument key; never displayed.
    const val MATCHMAKING = "matchmaking" // i18n: technical Navigation route or argument key; never displayed.
    const val CHAT = "chat" // i18n: technical Navigation route or argument key; never displayed.
    const val FEEDBACK = "feedback" // i18n: technical Navigation route or argument key; never displayed.
    const val LOGIN = "login" // i18n: technical Navigation route or argument key; never displayed.
    const val PROFILE = "profile" // i18n: technical Navigation route or argument key; never displayed.
    const val GUIDES = "guides" // i18n: technical Navigation route or argument key; never displayed.
    const val COURSE_DETAIL = "course_detail" // i18n: technical Navigation route or argument key; never displayed.
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        modifier = modifier
    ) {
        composable(Destinations.HOME) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val profileViewModel: com.example.gentehablando.viewmodel.ProfileViewModel = hiltViewModel()
            
            // Inyectamos el cerebro de los anuncios para poder pasárselo a la pantalla de Home
            val adManager = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
                // Este Hilt nos devuelve la instancia Singleton real
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    ctx.applicationContext,
                    com.example.gentehablando.di.AdManagerEntryPoint::class.java
                ).getAdManager()
            }
            
            HomeScreen(
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                adManager = adManager,
                onConnectClick = { role ->
                    navController.navigate("${Destinations.GUIDELINES}/${role.name}") // i18n: technical Navigation route or argument key; never displayed.
                },
                onLoginClick = { intendedRole ->
                    navController.navigate("${Destinations.LOGIN}/${intendedRole.name}") // i18n: technical Navigation route or argument key; never displayed.
                },
                onProfileClick = {
                    navController.navigate(Destinations.PROFILE)
                },
                onGuidesClick = {
                    navController.navigate(Destinations.GUIDES)
                }
            )
        }

        composable(Destinations.GUIDES) {
            val courseViewModel: com.example.gentehablando.viewmodel.CourseViewModel = hiltViewModel()
            GuidesScreen(
                viewModel = courseViewModel,
                onCourseClick = { courseId ->
                    navController.navigate("${Destinations.COURSE_DETAIL}/$courseId") // i18n: technical Navigation route or argument key; never displayed.
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Destinations.COURSE_DETAIL}/{courseId}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }) // i18n: technical Navigation route or argument key; never displayed.
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable // i18n: technical Navigation route or argument key; never displayed.
            val courseViewModel: com.example.gentehablando.viewmodel.CourseViewModel = hiltViewModel()
            
            com.example.gentehablando.ui.screens.CourseDetailScreen(
                courseId = courseId,
                viewModel = courseViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Login ahora recibe el parámetro del rol intencionado
        composable(
            route = "${Destinations.LOGIN}/{role}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(navArgument("role") { type = NavType.StringType }) // i18n: technical Navigation route or argument key; never displayed.
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name // i18n: technical Navigation route or argument key; never displayed.
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // Si el login tiene éxito, lo mandamos a las recomendaciones de su rol
                    // y limpiamos el BackStack para que el botón "Atrás" no lo regrese al Login
                    navController.navigate("${Destinations.GUIDELINES}/${selectedRole.name}") { // i18n: technical Navigation route or argument key; never displayed.
                        popUpTo(Destinations.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.GUIDELINES}/{role}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(navArgument("role") { type = NavType.StringType }) // i18n: technical Navigation route or argument key; never displayed.
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name // i18n: technical Navigation route or argument key; never displayed.
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            RoleGuidelinesScreen(
                role = selectedRole,
                onProceedToMatchmaking = {
                    // El matchmaking arranca ÚNICAMENTE cuando se aceptan las pautas
                    navController.navigate("${Destinations.MATCHMAKING}/${selectedRole.name}") { // i18n: technical Navigation route or argument key; never displayed.
                        popUpTo(Destinations.GUIDELINES) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.MATCHMAKING}/{role}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(navArgument("role") { type = NavType.StringType }) // i18n: technical Navigation route or argument key; never displayed.
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name // i18n: technical Navigation route or argument key; never displayed.
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val matchmakingViewModel: MatchmakingViewModel = hiltViewModel()

            MatchmakingScreen(
                role = selectedRole,
                viewModel = matchmakingViewModel,
                onCancel = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                },
                onMatchFound = { roomId ->
                    navController.navigate("${Destinations.CHAT}/$roomId/${selectedRole.name}") { // i18n: technical Navigation route or argument key; never displayed.
                        popUpTo(Destinations.MATCHMAKING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.CHAT}/{roomId}/{role}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }, // i18n: technical Navigation route or argument key; never displayed.
                navArgument("role") { type = NavType.StringType } // i18n: technical Navigation route or argument key; never displayed.
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "room_temp" // i18n: technical Navigation route or argument key; never displayed.
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name // i18n: technical Navigation route or argument key; never displayed.
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val partnerAlias = stringResource(if (selectedRole == UserRole.SPEAKER) R.string.listener_label else R.string.speaker_label)
            val chatViewModel: ChatViewModel = hiltViewModel()

            // Quitamos el LaunchedEffect de aquí, la responsabilidad del inicio 
            // pasa a ser de ChatScreen, quien controlará los permisos primero.
            
            ChatScreen(
                roomId = roomId,
                partnerAlias = partnerAlias,
                viewModel = chatViewModel,
                onLeaveChat = {
                    navController.navigate("${Destinations.FEEDBACK}/${selectedRole.name}/$roomId") { // i18n: technical Navigation route or argument key; never displayed.
                        popUpTo(Destinations.CHAT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.FEEDBACK}/{role}/{roomId}", // i18n: technical Navigation route or argument key; never displayed.
            arguments = listOf(
                navArgument("role") { type = NavType.StringType }, // i18n: technical Navigation route or argument key; never displayed.
                navArgument("roomId") { type = NavType.StringType } // i18n: technical Navigation route or argument key; never displayed.
            )
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name // i18n: technical Navigation route or argument key; never displayed.
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "" // i18n: technical Navigation route or argument key; never displayed.
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val partnerAlias = stringResource(if (selectedRole == UserRole.SPEAKER) R.string.listener_label else R.string.speaker_label)
            
            val feedbackViewModel: com.example.gentehablando.viewmodel.FeedbackViewModel = hiltViewModel()

            // Inyectamos el cerebro de los anuncios para poder pasárselo a la pantalla de Feedback
            val adManager = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    ctx.applicationContext,
                    com.example.gentehablando.di.AdManagerEntryPoint::class.java
                ).getAdManager()
            }

            FeedbackScreen(
                partnerAlias = partnerAlias,
                role = selectedRole,
                roomId = roomId,
                viewModel = feedbackViewModel,
                adManager = adManager,
                onFinish = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.PROFILE) {
            val profileViewModel: com.example.gentehablando.viewmodel.ProfileViewModel = hiltViewModel()
            com.example.gentehablando.ui.screens.ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogOutSuccess = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
