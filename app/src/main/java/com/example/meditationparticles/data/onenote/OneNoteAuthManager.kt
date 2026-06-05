package com.example.meditationparticles.data.onenote

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.example.meditationparticles.BuildConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OneNoteAuthManager(
    private val context: Context,
) {
    val isAvailable: Boolean = BuildConfig.ONENOTE_SYNC_AVAILABLE &&
        BuildConfig.ONENOTE_CLIENT_ID.isNotBlank()

    private val appContext = context.applicationContext
    private var clientApplication: ISingleAccountPublicClientApplication? = null

    suspend fun initialize(): Result<Unit> {
        if (!isAvailable) {
            return Result.failure(IllegalStateException("OneNote sync is not configured"))
        }
        if (clientApplication != null) return Result.success(Unit)
        return runCatching {
            clientApplication = createApplication()
        }
    }

    /**
     * Aligns MSAL cache with app preferences. SINGLE-account MSAL can retain a session after
     * prefs were cleared, which causes "signed in account does not match" on the next connect.
     */
    suspend fun reconcileConnectionState(preferences: OneNotePreferences) {
        if (!isAvailable) return
        initialize().getOrElse { return }
        val prefs = preferences.load()
        val msalEmail = currentAccountEmail()
        when {
            msalEmail == null && !prefs.accountEmail.isNullOrBlank() -> {
                preferences.clearConnection()
            }
            msalEmail != null && prefs.accountEmail.isNullOrBlank() -> {
                clearMsalSession()
            }
        }
    }

    suspend fun signIn(activity: Activity): OneNoteAuthResult {
        initialize().getOrElse { return OneNoteAuthResult.failure(it.message ?: "Not configured") }
        clearMsalSession()
        return acquireTokenInteractive(activity, allowMismatchRetry = true)
    }

    suspend fun acquireAccessToken(): String {
        initialize().getOrElse { throw it }
        val app = clientApplication ?: throw IllegalStateException("MSAL not initialized")
        val account = getCurrentAccount()
            ?: throw OneNoteAuthRequiredException("No Microsoft account connected")
        return suspendCancellableCoroutine { continuation ->
            app.acquireTokenSilentAsync(
                AcquireTokenSilentParameters.Builder()
                    .withScopes(SCOPES.toList())
                    .forAccount(account)
                    .fromAuthority(account.authority)
                    .withCallback(object : com.microsoft.identity.client.SilentAuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                            val token = authenticationResult?.accessToken
                            if (token.isNullOrBlank()) {
                                continuation.resumeWithException(
                                    OneNoteAuthRequiredException("Could not acquire access token"),
                                )
                            } else {
                                continuation.resume(token)
                            }
                        }

                        override fun onError(exception: MsalException) {
                            if (exception is MsalUiRequiredException) {
                                continuation.resumeWithException(OneNoteAuthRequiredException())
                            } else {
                                continuation.resumeWithException(exception)
                            }
                        }
                    })
                    .build(),
            )
        }
    }

    suspend fun currentAccountEmail(): String? = getCurrentAccount()?.username

    suspend fun signOut() {
        if (!isAvailable) return
        clearMsalSession()
    }

    private suspend fun clearMsalSession() {
        if (!isAvailable) return
        initialize().getOrElse { return }
        val app = clientApplication ?: return
        suspendCancellableCoroutine { continuation ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    continuation.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    // Still drop the client so the next sign-in starts from a clean MSAL instance.
                    clientApplication = null
                    continuation.resume(Unit)
                }
            })
        }
        clientApplication = null
    }

    private suspend fun acquireTokenInteractive(
        activity: Activity,
        allowMismatchRetry: Boolean,
    ): OneNoteAuthResult {
        initialize().getOrElse { return OneNoteAuthResult.failure(it.message ?: "Not configured") }
        val app = clientApplication ?: return OneNoteAuthResult.failure("MSAL not initialized")
        return suspendCancellableCoroutine { continuation ->
            app.acquireToken(
                AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES.toList())
                    .withPrompt(Prompt.SELECT_ACCOUNT)
                    .withCallback(object : com.microsoft.identity.client.AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                            val result = authenticationResult
                                ?: return continuation.resume(
                                    OneNoteAuthResult.failure("Sign in returned no account"),
                                )
                            continuation.resume(
                                OneNoteAuthResult.success(
                                    email = result.account.username,
                                ),
                            )
                        }

                        override fun onError(exception: MsalException) {
                            if (allowMismatchRetry && isAccountMismatch(exception)) {
                                continuation.resume(OneNoteAuthResult.retryAfterMismatch())
                            } else {
                                continuation.resume(
                                    OneNoteAuthResult.failure(friendlyAuthError(exception)),
                                )
                            }
                        }

                        override fun onCancel() {
                            continuation.resume(OneNoteAuthResult.cancelled())
                        }
                    })
                    .build(),
            )
        }.let { result ->
            if (result.retryAfterAccountMismatch) {
                clearMsalSession()
                acquireTokenInteractive(activity, allowMismatchRetry = false)
            } else {
                result
            }
        }
    }

    private fun isAccountMismatch(exception: MsalException): Boolean {
        if (exception is MsalClientException &&
            exception.errorCode == MsalClientException.CURRENT_ACCOUNT_MISMATCH
        ) {
            return true
        }
        return exception.message?.contains("does not match", ignoreCase = true) == true
    }

    private fun friendlyAuthError(exception: MsalException): String {
        if (isAccountMismatch(exception)) {
            return "A different Microsoft account is still signed in. Tap Disconnect, then Connect again."
        }
        return exception.message ?: "Sign in failed"
    }

    private suspend fun getCurrentAccount(): IAccount? {
        val app = clientApplication ?: return null
        return suspendCancellableCoroutine { continuation ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    continuation.resume(activeAccount)
                }

                override fun onAccountChanged(
                    priorAccount: IAccount?,
                    currentAccount: IAccount?,
                ) {
                    continuation.resume(currentAccount)
                }

                override fun onError(exception: MsalException) {
                    continuation.resume(null)
                }
            })
        }
    }

    private suspend fun createApplication(): ISingleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            val configFile = writeMsalConfigFile()
            PublicClientApplication.create(
                appContext,
                configFile,
                object : IPublicClientApplication.ApplicationCreatedListener {
                    override fun onCreated(application: IPublicClientApplication?) {
                        val singleAccountApp = application as? ISingleAccountPublicClientApplication
                        if (singleAccountApp == null) {
                            continuation.resumeWithException(
                                IllegalStateException("Expected single-account MSAL application"),
                            )
                        } else {
                            continuation.resume(singleAccountApp)
                        }
                    }

                    override fun onError(exception: MsalException?) {
                        continuation.resumeWithException(
                            exception ?: IllegalStateException("MSAL initialization failed"),
                        )
                    }
                },
            )
        }

    private fun writeMsalConfigFile(): File {
        val file = File(appContext.cacheDir, "msal_onenote_config.json")
        file.writeText(buildMsalConfig().toString())
        return file
    }

    private fun buildMsalConfig(): JSONObject {
        // Must match MSAL verifyRedirectUriWithAppSignature(): Uri.Builder scheme/host/appendPath(hash).
        // Do NOT fully URL-encode (+ and = stay literal); a "/" in the hash becomes %2F only.
        val redirectUri = Uri.Builder()
            .scheme("msauth")
            .authority(appContext.packageName)
            .appendPath(BuildConfig.ONENOTE_REDIRECT_SIGNATURE_HASH)
            .build()
            .toString()
        return JSONObject().apply {
            put("client_id", BuildConfig.ONENOTE_CLIENT_ID)
            put("authorization_user_agent", "DEFAULT")
            put("redirect_uri", redirectUri)
            put("account_mode", "SINGLE")
            put(
                "authorities",
                JSONArray().put(
                    JSONObject().apply {
                        put("type", "AAD")
                        put("default", true)
                        put(
                            "audience",
                            JSONObject().apply {
                                // MSAL 6.x enum: AzureADandPersonalMicrosoftAccount (not AzureAdAnd…)
                                put("type", "AzureADandPersonalMicrosoftAccount")
                                // Explicit tenant for consumer + work/school sign-in (avoids unauthorized_client)
                                put("tenant_id", "common")
                            },
                        )
                    },
                ),
            )
        }
    }

    companion object {
        // MSAL Android always adds openid, profile, and offline_access — do not include them here.
        val SCOPES = arrayOf("User.Read", "Notes.ReadWrite")
    }
}

data class OneNoteAuthResult(
    val success: Boolean,
    val email: String? = null,
    val errorMessage: String? = null,
    val cancelled: Boolean = false,
    val retryAfterAccountMismatch: Boolean = false,
) {
    companion object {
        fun success(email: String?) = OneNoteAuthResult(success = true, email = email)

        fun failure(message: String) = OneNoteAuthResult(
            success = false,
            errorMessage = message,
        )

        fun cancelled() = OneNoteAuthResult(success = false, cancelled = true)

        fun retryAfterMismatch() = OneNoteAuthResult(
            success = false,
            retryAfterAccountMismatch = true,
        )
    }
}

class OneNoteAuthRequiredException(
    message: String = "Microsoft sign-in required",
) : Exception(message)
