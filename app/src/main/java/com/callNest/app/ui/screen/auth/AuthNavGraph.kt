package com.callNest.app.ui.screen.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

/**
 * Drop this into the app's main NavHost:
 *
 * NavHost(startDestination = if (signedIn) "main_graph" else AuthDestinations.GRAPH) {
 *     authGraph(navController, onAuthenticated = { navController.navigate("main_graph") { popUpTo(0) } })
 *     // ... your existing main_graph
 * }
 */
/** Installs the auth nav graph; call from a top-level [androidx.navigation.compose.NavHost]. */
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthenticated: () -> Unit,
) {
    navigation(startDestination = AuthDestinations.WELCOME, route = AuthDestinations.GRAPH) {
        composable(AuthDestinations.WELCOME) {
            WelcomeAuthScreen(
                onSignIn = { navController.navigate(AuthDestinations.LOGIN) },
                onSignUp = { navController.navigate(AuthDestinations.SIGNUP) },
            )
        }
        composable(AuthDestinations.LOGIN) {
            LoginScreen(
                onAuthenticated = onAuthenticated,
                onForgotPassword = { navController.navigate(AuthDestinations.FORGOT) },
                onCreateAccount = {
                    navController.navigate(AuthDestinations.SIGNUP) {
                        popUpTo(AuthDestinations.WELCOME)
                    }
                },
            )
        }
        composable(AuthDestinations.SIGNUP) {
            SignupScreen(
                onAccountCreated = { email ->
                    navController.navigate(AuthDestinations.verify(email)) {
                        popUpTo(AuthDestinations.WELCOME)
                    }
                },
                onAuthenticated = onAuthenticated,
                onSignInInstead = {
                    navController.navigate(AuthDestinations.LOGIN) {
                        popUpTo(AuthDestinations.WELCOME)
                    }
                },
            )
        }
        composable(AuthDestinations.FORGOT) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = AuthDestinations.VERIFY,
            arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            VerifyEmailScreen(
                email = email,
                onAuthenticated = onAuthenticated,
                onBackToSignIn = {
                    navController.navigate(AuthDestinations.LOGIN) {
                        popUpTo(AuthDestinations.WELCOME)
                    }
                },
            )
        }
        composable(AuthDestinations.RESET) {
            ResetPasswordScreen(onPasswordUpdated = onAuthenticated)
        }
    }
}
