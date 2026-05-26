# OneNote 365 sync setup

Optional one-way sync of saved toolkit journal entries to Microsoft OneNote. The app remains the source of truth; OneNote receives HTML pages when sync is connected and enabled.

## Prerequisites

- A Microsoft account with OneNote (personal `@outlook.com` / `@hotmail.com` or Microsoft 365)
- An app registration in [Microsoft Entra admin center](https://entra.microsoft.com/) (Azure Portal)

## 1. Register the Android app

1. Open **App registrations** → **New registration**.
2. Name: `Serene Interval` (or your choice).
3. Supported account types: **Accounts in any organizational directory and personal Microsoft accounts**.
4. Redirect URI: **Public client/native (mobile & desktop)**  
   - Package: `com.example.meditationparticles`  
   - Signature hash (standard Base64 from sideload keystore SHA-1): `wnyLuNCKNp+EU4eMI6tuS0f+G/I=`  
     - Note **`MI6`** uses capital **I**, not lowercase **l** (`Ml6`).
   - Azure accepts either form; register **one or both**:
     ```
     msauth://com.example.meditationparticles/wnyLuNCKNp+EU4eMI6tuS0f+G/I=
     msauth://com.example.meditationparticles/wnyLuNCKNp%2BEU4eMI6tuS0f%2BG%2FI%3D
     ```
   - Do **not** use URL-safe Base64 (`-` / `_`, no padding). MSAL Android expects standard Base64 (`+` / `/` / `=`).
   - This hash matches the project **sideload** keystore (`keystore/sideload.jks`). All debug/release builds in this repo use that key (see `release/SIGNING.md`).

5. Copy the **Application (client) ID**.

### MSAL redirect URI encoding (important)

MSAL validates the in-app config against the redirect URI it generates from your package name + signing certificate. If connect fails immediately, read the exception text:

```
We expected '…' and we received '…'.
```

- **Expected** = MSAL runtime value (what your config must use)
- **Received** = what is currently in the generated `msal_onenote_config.json`

The app builds config with `Uri.Builder` (same as MSAL). For this hash that is:

```
msauth://com.example.meditationparticles/wnyLuNCKNp+EU4eMI6tuS0f+G%2FI=
```

(`+` and `=` stay literal; the `/` inside the hash becomes `%2F`.)

Do **not** paste Azure’s fully URL-encoded redirect URI into app code (`%2B`, `%3D`, etc.) — that causes a mismatch.

`AndroidManifest.xml` `BrowserTabActivity` uses the **raw** hash path (not URL-encoded):

```
/wnyLuNCKNp+EU4eMI6tuS0f+G/I=
```

### API permissions (delegated)

In **API permissions** → **Add a permission** → **Microsoft Graph** → **Delegated permissions**, add:

| Permission | Purpose |
|---|---|
| `Notes.ReadWrite` | Create pages in OneNote |
| `User.Read` | Show connected account email |

Use **Delegated** permissions only (not Application). Both must be under **Microsoft Graph**, not legacy OneNote or Office APIs.

For work/school tenants, click **Grant admin consent for [tenant]** if your org requires admin approval. Personal Microsoft accounts consent during the sign-in prompt.

**Refresh tokens:** MSAL Android automatically requests `openid`, `profile`, and `offline_access`. Do **not** add `offline_access` to app code scopes or as a Graph API permission — it is not listed in Graph permissions and including it in code can cause scope-decline errors.

## 2. Local developer config

Add to **`local.properties`** (not committed):

```properties
onenote.clientId=YOUR_AZURE_APPLICATION_CLIENT_ID
```

Rebuild the app. `BuildConfig.ONENOTE_SYNC_AVAILABLE` becomes `true` when the client ID is present.

Without a client ID, the app builds normally and OneNote sync stays disabled (Settings shows setup instructions).

## 3. Verify in the app

1. **Settings → Integrations → OneNote** — Connect Microsoft account.
2. On first connect, the app creates a **Serene Interval** section in your default OneNote notebook (first notebook in the account).
3. Choose a different **notebook** or **section** if you prefer; the current target is shown as `Syncing to: {notebook} → {section}`.
4. Toggle which **entry types** sync (NVC, Refactoring, Center of Gravity, Thought Dump, Anxiety Log, Future Self).
5. Save a toolkit entry — it syncs when **Auto-sync on save** is on and that entry type is enabled.
6. Use **Sync existing entries** to queue all local history for enabled types (also runs automatically on first connect).
7. Open a saved entry in Toolkit history and tap **Sync to OneNote** for a one-off sync.
8. Use **Sync now** to drain the offline queue after reconnecting.
9. Deleting an entry locally removes its mapped OneNote page when connected.

Optional: during onboarding, a **Connect OneNote** step appears after notifications (skippable).

## Settings reference

| Control | Purpose |
|---|---|
| Connect / Disconnect | Microsoft sign-in via MSAL |
| Choose notebook / section | Target location for new pages |
| Sync entry types | Per-tool auto-sync toggles |
| Auto-sync on save | Queue entries when saved in Toolkit |
| Sync existing entries | Backfill local history |
| Sync now | Process pending queue immediately |

## Redirect URI if you change signing keys

If you sign with a different keystore, regenerate the redirect hash:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$keytool = "$env:JAVA_HOME\bin\keytool.exe"
# Replace store path/password for your keystore
& $keytool -list -v -keystore keystore\sideload.jks -storepass serene-sideload
```

Take the **SHA1** fingerprint, remove colons, convert hex → bytes → **standard Base64** (with `+`, `/`, and `=` padding). Update:

- Azure app registration redirect URI (either encoded or unencoded form)
- `AndroidManifest.xml` `BrowserTabActivity` intent filter `android:path` (raw hash with leading `/`)
- `BuildConfig.ONENOTE_REDIRECT_SIGNATURE_HASH` in `app/build.gradle.kts` (raw Base64 hash only)

## Privacy

When sync is connected and enabled, **journal text** from toolkit entries is sent to the user's Microsoft OneNote account in the chosen notebook and section. The app stores notebook/section IDs and sync mappings locally (encrypted preferences and Room). **Audio recordings are not uploaded** — OneNote pages note when audio was recorded in the app only. Disconnecting clears local connection state and sync mappings (not remote OneNote pages). Users can limit which entry types sync and turn off auto-sync while staying connected.

## Troubleshooting

| Symptom | Check |
|---|---|
| Connect fails immediately | Client ID in `local.properties`; config redirect matches MSAL **Expected** value (not Azure’s fully encoded copy); Azure has matching redirect URI; hash uses `MI6` not `Ml6` |
| Sync pending forever | Network, account session (try Reconnect in Settings), Graph throttling |
| Section not found | Reconnect to recreate **Serene Interval** section |
| Build works but no OneNote UI actions | `onenote.clientId` missing from `local.properties` |

### `Some or all required scopes have been declined by the Server`

MSAL raises this (`MsalDeclinedScopeException`) when Azure or the signed-in user does not grant every scope the app requests. Common causes and fixes:

**1. Azure API permissions missing or wrong type**

- Open [App registrations](https://entra.microsoft.com/) → your app → **API permissions**.
- Ensure **Microsoft Graph** delegated permissions **`Notes.ReadWrite`** and **`User.Read`** are listed (not Application permissions).
- Do not add legacy **OneNote** or **Office 365** API permissions — this app uses Microsoft Graph (`/me/onenote/...`).
- For work/school accounts in a managed tenant: click **Grant admin consent for [tenant]** so users are not blocked by org policy.
- Status should show a green check for each permission after consent.

**2. User did not accept the consent prompt**

- During Connect, Microsoft shows a permissions screen. The user must tap **Accept** (not Cancel or back).
- If consent was previously denied, remove the app under [Microsoft account permissions](https://account.microsoft.com/privacy/app-access) or your org’s enterprise app consent page, then reconnect.

**3. Work vs personal account**

- Personal accounts (`@outlook.com`, `@hotmail.com`, `@live.com`) need supported account types **Accounts in any organizational directory and personal Microsoft accounts** (see [unauthorized_client](#unauthorized_client-the-client-does-not-exist-or-is-not-enabled-for-consumers) section).
- `Notes.ReadWrite` and `User.Read` are valid for personal Microsoft accounts; if only org accounts work, the registration likely lacks personal account support or admin consent is pending.

**4. Do not request `offline_access` in app code**

- MSAL Android always sends `openid`, `profile`, and `offline_access` automatically.
- Listing `offline_access` in the app’s scope array can trigger this exact error even when Azure looks correct.
- The app requests only `User.Read` and `Notes.ReadWrite`; refresh tokens still work via MSAL’s built-in scopes.

**5. Client ID mismatch**

- Confirm `onenote.clientId` in `local.properties` matches the registration where you added Graph permissions (see [Verify client ID](#verify-client-id-matches-app-registration)).

After fixing Azure or reinstalling an updated build, disconnect and **Connect** again in Settings.

### `unauthorized_client: The client does not exist or is not enabled for consumers`

This error usually means the Azure app registration does not match what MSAL expects for personal Microsoft accounts (`@outlook.com`, `@hotmail.com`, etc.). The app uses **AzureADandPersonalMicrosoftAccount** with **`tenant_id`: `common`** in MSAL config, which requires the registration to support both work/school and personal accounts.

**Fix in Azure Portal** ([App registrations](https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)):

1. Open your app → **Authentication** (or **Branding & properties** → **Supported account types**).
2. **Supported account types** must include personal Microsoft accounts (see [Verify supported account types](#verify-supported-account-types-in-azure) below).
3. Under **Authentication** → **Advanced settings**:
   - **Allow public client flows** = **Yes** (required for native Android / MSAL).
4. Verify **Application (client) ID** matches `onenote.clientId` in your local `local.properties` exactly (see [Verify client ID](#verify-client-id-matches-app-registration)).
5. Confirm the redirect URI from section 1 is registered under **Mobile and desktop applications**.
6. Rebuild and reinstall the app after changing `local.properties` or Azure settings.

**Signing in with `@outlook.com` / `@hotmail.com` / `@live.com`:** personal Microsoft accounts are only allowed when the registration’s supported account types include personal accounts (multitenant + personal). Work/school-only (single-tenant) registrations reject consumer sign-in with this error.

If you only need work/school accounts, you can use a single-tenant registration — but personal OneNote users must use the multitenant + personal account type above.

#### Verify supported account types in Azure

In [App registrations](https://entra.microsoft.com/) → your app → **Overview** (or **Authentication** / **Branding & properties**), find **Supported account types**. The label must match **exactly** one of these (copy/paste when comparing):

| What you need | Exact Azure label |
|---|---|
| Personal + work/school (required for `@outlook.com`) | **Accounts in any organizational directory and personal Microsoft accounts** |
| Work/school only (wrong for consumer OneNote) | **Accounts in this organizational directory only (Single tenant)** |
| Any org, no personal | **Accounts in any organizational directory (Multitenant)** |

The full long form shown in some blades is:

**Accounts in any organizational directory and personal Microsoft accounts (Microsoft Entra ID – Multitenant + personal Microsoft accounts)**

If your app shows **Single tenant** only, consumer accounts are not enabled — fix or replace the registration (next section).

#### If single-tenant cannot be changed: new app registration

Some older or misconfigured registrations cannot switch from single-tenant to multitenant + personal. Create a **new** registration:

1. **App registrations** → **New registration**.
2. Name: e.g. `Serene Interval OneNote` (distinct from any old app).
3. **Supported account types**: select **Accounts in any organizational directory and personal Microsoft accounts** (not single-tenant).
4. Redirect URI: **Public client/native** — add both redirect URIs from [section 1](#1-register-the-android-app) for package `com.example.meditationparticles`.
5. **Register** → copy the new **Application (client) ID** (Overview).
6. **API permissions** → add delegated `Notes.ReadWrite` and `User.Read` under Microsoft Graph (same as section 1).
7. **Authentication** → **Advanced settings** → **Allow public client flows** = **Yes**.
8. Update `local.properties`: `onenote.clientId=<new GUID>` (replace old value entirely).
9. Rebuild and reinstall the app. In Settings, disconnect/reconnect OneNote if you had a previous failed sign-in.

You can leave or delete the old registration; only the client ID in `local.properties` must match the app you configure.

#### Verify client ID matches app registration

A common mistake is copying the client ID from the wrong app (another project, an old registration, or **Directory (tenant) ID** instead of **Application (client) ID**).

1. Azure → **App registrations** → open the app that has your redirect URIs and API permissions.
2. **Overview** → copy **Application (client) ID** (a GUID like `12345678-1234-1234-1234-123456789abc`).
3. Compare character-for-character with `onenote.clientId` in `local.properties` (no quotes, no spaces, no trailing newline).
4. After any change, run a clean rebuild (`assembleDebug` / reinstall) so `BuildConfig.ONENOTE_CLIENT_ID` updates.

If the ID in the app does not match the registration where you enabled personal accounts and public client flows, MSAL will still report `unauthorized_client` even when Azure looks correct for a different app.

