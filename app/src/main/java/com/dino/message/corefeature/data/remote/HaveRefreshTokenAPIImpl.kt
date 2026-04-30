package com.dino.message.corefeature.data.remote

import android.util.Log
import com.dino.message.corefeature.data.remote.dto.GetTokenResponse
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.util.RefreshTokenExpiredException
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
 * @param sPrefManager The instance of SPrefManager used for managing shared preferences.
 */
class HaveRefreshTokenAPIImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager
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
                Log.e("HaveRefreshTokenAPI", "getToken: refresh token is empty")
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
                Log.e("HaveRefreshTokenAPI", "getToken: refresh failed with status=${response.status.value}")
                return ""
            }

            val tokenResponse = response.body<GetTokenResponse>()

            val newAccessToken = tokenResponse.mapResponseToResult()
                .trim()
                .removePrefix("Bearer ")
                .removePrefix("bearer ")
                .trim()

            if (newAccessToken.count { it == '.' } != 2) {
                Log.e("HaveRefreshTokenAPI", "getToken: received malformed access token")
                ""
            } else {
                newAccessToken
            }
        } catch (e: RefreshTokenExpiredException) {
            throw e
        } catch (e: Exception) {
            Log.e("HaveRefreshTokenAPI", "getToken: refresh request failed", e)
            ""
        }
    }

}
