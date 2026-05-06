package com.callvault.app.ui.screen.auth

/** Routes for the auth nav graph. Kept as a flat object for easy navigation. */
object AuthDestinations {
    const val GRAPH = "auth_graph"

    const val WELCOME = "auth/welcome"
    const val LOGIN = "auth/login"
    const val SIGNUP = "auth/signup"
    const val FORGOT = "auth/forgot"
    const val VERIFY = "auth/verify?email={email}"
    const val RESET = "auth/reset"
    const val PROFILE = "auth/profile"

    fun verify(email: String): String = "auth/verify?email=${android.net.Uri.encode(email)}"
}
