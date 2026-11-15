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
import com.example.gentehablando.viewmodel.AuthViewModel
import com.example.gentehablando.ui.screens.ChatScreen
import com.example.gentehablando.ui.screens.FeedbackScreen
import com.example.gentehablando.ui.screens.HomeScreen
import com.example.gentehablando.ui.screens.MatchmakingScreen
import com.example.gentehablando.ui.screens.RoleGuidelinesScreen
import com.example.gentehablando.viewmodel.ChatViewModel
import com.example.gentehablando.viewmodel.MatchmakingViewModel

object Destinations {
    const val HOME = "home"
    const val GUIDELINES = "guidelines"
    const val MATCHMAKING = "matchmaking"
    const val CHAT = "chat"
    const val FEEDBACK = "feedback"
    const val LOGIN = "login"

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
            HomeScreen(
                authViewModel = authViewModel,
                onConnectClick = { role ->
                    navController.navigate("${Destinations.GUIDELINES}/${role.name}")
                },
                onLoginClick = { intendedRole ->
                    // Navegamos al login pasándole el rol que quería el usuario
                    navController.navigate("${Destinations.LOGIN}/${intendedRole.name}")
                }
            )
        }

        // Login ahora recibe el parámetro del rol intencionado
        composable(
            route = "${Destinations.LOGIN}/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name
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
                    navController.navigate("${Destinations.GUIDELINES}/${selectedRole.name}") {
                        popUpTo(Destinations.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.GUIDELINES}/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            RoleGuidelinesScreen(
                role = selectedRole,
                onProceedToMatchmaking = {
                    // El matchmaking arranca ÚNICAMENTE cuando se aceptan las pautas
                    navController.navigate("${Destinations.MATCHMAKING}/${selectedRole.name}") {
                        popUpTo(Destinations.GUIDELINES) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.MATCHMAKING}/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name
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
                    navController.popBackStack()
                },
                onMatchFound = { roomId ->
                    navController.navigate("${Destinations.CHAT}/$roomId/${selectedRole.name}") {
                        popUpTo(Destinations.MATCHMAKING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.CHAT}/{roomId}/{role}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "room_temp"
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val partnerAlias = if (selectedRole == UserRole.SPEAKER) "Oyente" else "Hablante"
            val chatViewModel: ChatViewModel = hiltViewModel()

            // Quitamos el LaunchedEffect de aquí, la responsabilidad del inicio 
            // pasa a ser de ChatScreen, quien controlará los permisos primero.
            
            ChatScreen(
                roomId = roomId,
                partnerAlias = partnerAlias,
                viewModel = chatViewModel,
                onLeaveChat = {
                    navController.navigate("${Destinations.FEEDBACK}/${selectedRole.name}") {
                        popUpTo(Destinations.CHAT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "${Destinations.FEEDBACK}/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val roleParam = backStackEntry.arguments?.getString("role") ?: UserRole.SPEAKER.name
            val selectedRole = try {
                UserRole.valueOf(roleParam)
            } catch (_: Exception) {
                UserRole.SPEAKER
            }

            val partnerAlias = if (selectedRole == UserRole.SPEAKER) "Oyente" else "Hablante"

            FeedbackScreen(
                partnerAlias = partnerAlias,
                role = selectedRole,
                onFinish = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
