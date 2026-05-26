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
   - Redirect URI (register the **URL-encoded** form — this is what MSAL sends at runtime):
     ```
     msauth://com.example.meditationparticles/wnyLuNCKNp%2BEU4eMI6tuS0f%2BG%2FI%3D
     ```
   - Equivalent unencoded form (standard Base64 hash from the signing cert):
     ```
     msauth://com.example.meditationparticles/wnyLuNCKNp+EU4eMI6tuS0f+G/I=
     ```
   - Do **not** use URL-safe Base64 (`-` / `_`, no padding). MSAL Android expects standard Base64 (`+` / `/` / `=`).
   - This hash matches the project **sideload** keystore (`keystore/sideload.jks`). All debug/release builds in this repo use that key (see `release/SIGNING.md`).

5. Copy the **Application (client) ID**.

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

- Azure app registration redirect URI
- `AndroidManifest.xml` `BrowserTabActivity` intent filter `android:path`
- `BuildConfig.ONENOTE_REDIRECT_SIGNATURE_HASH` in `app/build.gradle.kts`

## Privacy

Journal content is sent to the user’s Microsoft cloud when sync is enabled. Audio recordings are **not** uploaded in MVP — pages note that audio was recorded in the app only.

## Troubleshooting

| Symptom | Check |
|---|---|
| Connect fails immediately | Client ID in `local.properties`, Azure redirect URI matches MSAL (URL-encoded standard Base64, not URL-safe `-`/`_`) |
| Sync pending forever | Network, account session (try Reconnect in Settings), Graph throttling |
| Section not found | Reconnect to recreate **Serene Interval** section |
| Build works but no OneNote UI actions | `onenote.clientId` missing from `local.properties` |
