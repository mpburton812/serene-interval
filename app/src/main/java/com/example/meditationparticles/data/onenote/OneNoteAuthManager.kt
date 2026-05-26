package com.example.meditationparticles.data.onenote

import android.app.Activity
import android.content.Context
import com.example.meditationparticles.BuildConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
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

    suspend fun signIn(activity: Activity): OneNoteAuthResult {
        initialize().getOrElse { return OneNoteAuthResult.failure(it.message ?: "Not configured") }
        val app = clientApplication ?: return OneNoteAuthResult.failure("MSAL not initialized")
        return suspendCancellableCoroutine { continuation ->
            app.acquireToken(
                AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES.toList())
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
                            continuation.resume(
                                OneNoteAuthResult.failure(exception.message ?: "Sign in failed"),
                            )
                        }

                        override fun onCancel() {
                            continuation.resume(OneNoteAuthResult.cancelled())
                        }
                    })
                    .build(),
            )
        }
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
        val app = clientApplication ?: return
        suspendCancellableCoroutine { continuation ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    continuation.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
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
        val redirectUri =
            "msauth://${appContext.packageName}/${BuildConfig.ONENOTE_REDIRECT_SIGNATURE_HASH}"
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
                        put(
                            "audience",
                            JSONObject().apply {
                                put("type", "PersonalMicrosoftAccount")
                                put("tenant_id", "consumers")
                            },
                        )
                    },
                ),
            )
        }
    }

    companion object {
        val SCOPES = arrayOf("User.Read", "Notes.ReadWrite", "offline_access")
    }
}

data class OneNoteAuthResult(
    val success: Boolean,
    val email: String? = null,
    val errorMessage: String? = null,
    val cancelled: Boolean = false,
) {
    companion object {
        fun success(email: String?) = OneNoteAuthResult(success = true, email = email)

        fun failure(message: String) = OneNoteAuthResult(
            success = false,
            errorMessage = message,
        )

        fun cancelled() = OneNoteAuthResult(success = false, cancelled = true)
    }
}

class OneNoteAuthRequiredException(
    message: String = "Microsoft sign-in required",
) : Exception(message)
