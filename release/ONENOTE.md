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

Add and grant admin consent if prompted:

| Permission | Purpose |
|---|---|
| `Notes.ReadWrite` | Create pages in OneNote |
| `User.Read` | Show connected account email |
| `offline_access` | Refresh tokens for background sync |

## 2. Local developer config

Add to **`local.properties`** (not committed):

```properties
onenote.clientId=YOUR_AZURE_APPLICATION_CLIENT_ID
```

Rebuild the app. `BuildConfig.ONENOTE_SYNC_AVAILABLE` becomes `true` when the client ID is present.

Without a client ID, the app builds normally and OneNote sync stays disabled (Settings shows setup instructions).

## 3. Verify in the app

1. **Settings → Integrations → OneNote** — Connect Microsoft account.
2. On first connect, the app creates a **Serene Interval** section in your default OneNote notebook.
3. Save a toolkit entry (NVC, Refactoring, etc.) — it should appear in OneNote when auto-sync is on.
4. Use **Sync now** to drain the offline queue after reconnecting.

Optional: during onboarding, a **Connect OneNote** step appears after notifications (skippable).

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

Journal content is sent to the user’s Microsoft cloud when sync is enabled. Audio recordings are **not** uploaded in MVP — pages note that audio was recorded in the app only.

## Troubleshooting

| Symptom | Check |
|---|---|
| Connect fails immediately | Client ID in `local.properties`; config redirect matches MSAL **Expected** value (not Azure’s fully encoded copy); Azure has matching redirect URI; hash uses `MI6` not `Ml6` |
| Sync pending forever | Network, account session (try Reconnect in Settings), Graph throttling |
| Section not found | Reconnect to recreate **Serene Interval** section |
| Build works but no OneNote UI actions | `onenote.clientId` missing from `local.properties` |

### `unauthorized_client: The client does not exist or is not enabled for consumers`

This error usually means the Azure app registration does not match what MSAL expects for personal Microsoft accounts (`@outlook.com`, `@hotmail.com`, etc.). The app uses **AzureADandPersonalMicrosoftAccount** (MSAL config spelling), which requires the registration to support both work/school and personal accounts.

**Fix in Azure Portal** ([App registrations](https://entra.microsoft.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)):

1. Open your app → **Authentication** (or **Branding & properties** → **Supported account types**).
2. **Supported account types** must include personal Microsoft accounts:
   - Choose **Accounts in any organizational directory and personal Microsoft accounts (Microsoft Entra ID – Multitenant + personal Microsoft accounts)**.
   - If the app was created as **single-tenant** (work/school only), change this setting or create a new registration with the correct account type.
3. Under **Authentication** → **Advanced settings**:
   - **Allow public client flows** = **Yes** (required for native Android / MSAL).
4. Verify **Application (client) ID** matches `onenote.clientId` in your local `local.properties` exactly (no extra spaces or quotes).
5. Confirm the redirect URI from section 1 is registered under **Mobile and desktop applications**.
6. Rebuild and reinstall the app after changing `local.properties` or Azure settings.

If you only need work/school accounts, you can use a single-tenant registration — but personal OneNote users must use the multitenant + personal account type above.

