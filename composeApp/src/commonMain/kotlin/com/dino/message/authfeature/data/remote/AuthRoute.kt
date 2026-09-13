package com.dino.message.authfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

/**
 * AuthRoute
 *
 * Sealed class that represents different routes related to authentication.
 *
 * @property url The URL associated with the route.
 */
sealed class AuthRoute(url: String) : BaseRoute(url) {
    /**
     * Login
     *
     * Object representing the login route.
     */
    object Login : AuthRoute("api/auth/login")

    /**
     * SignUp
     *
     * Object representing the sign-up route.
     */
    object RegisterStart : AuthRoute("api/auth/register/start")
    object RegisterConfirm : AuthRoute("api/auth/register/confirm")
    object UpdateDeviceId : AuthRoute("api/auth/users/device")
    object RotateKey : AuthRoute("api/auth/keys/rotate")
    object KeyStatus : AuthRoute("api/auth/keys/status")

    // Kept for backward compatibility with older clients that still call /register.
    object RegisterLegacy : AuthRoute("api/auth/register")
}
