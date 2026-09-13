package com.dino.message.corefeature.data.remote

import com.dino.message.corefeature.data.remote.dto.GetTokenResponse
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.util.RefreshTokenExpiredException
import com.dino.message.corefeature.domain.logging.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * Implementation of the HaveRefreshTokenAPI interface.
 *
 * @param httpClient The instance of HttpClient used for making API requests.
 * @param sPrefManager The session store used for managing auth tokens.
 */
class HaveRefreshTokenAPIImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: AuthSessionStore,
    private val appLogger: AppLogger? = null,
) : HaveRefreshTokenAPI {

    /**
     * Retrieves a new access token using the refresh token.
     *
     * @return The new access token, or empty string when refresh fails.
     */
    override suspend fun getToken(): String {
        return try {
            val refreshToken = sPrefManager.getRefreshToken()
                .trim()
                .removePrefix("Bearer ")
                .removePrefix("bearer ")
                .trim()

            if (refreshToken.isBlank()) {
                appLogger?.error("HaveRefreshTokenAPI", "getToken: refresh token is empty")
                throw RefreshTokenExpiredException("Refresh token is missing")
            }

            val response = httpClient.post(CoreRoute.GetToken.url) {
                headers {
                    remove(HttpHeaders.Authorization)
                    append(HttpHeaders.Authorization, "Bearer $refreshToken")
                }
            }

            if (
                response.status == HttpStatusCode.Unauthorized ||
                response.status == HttpStatusCode.UnprocessableEntity
            ) {
                throw RefreshTokenExpiredException("Refresh token is expired or invalid")
            }

            if (!response.status.isSuccess()) {
                appLogger?.error("HaveRefreshTokenAPI", "getToken: refresh failed with status=${response.status.value}")
                return ""
            }

            val tokenResponse = response.body<GetTokenResponse>()

            val newAccessToken = tokenResponse.mapResponseToResult()
                .trim()
                .removePrefix("Bearer ")
                .removePrefix("bearer ")
                .trim()

            if (newAccessToken.count { it == '.' } != 2) {
                appLogger?.error("HaveRefreshTokenAPI", "getToken: received malformed access token")
                ""
            } else {
                newAccessToken
            }
        } catch (e: RefreshTokenExpiredException) {
            throw e
        } catch (e: Exception) {
            appLogger?.error("HaveRefreshTokenAPI", "getToken: refresh request failed", e)
            ""
        }
    }

}
