# gplaydl Authenticator

Android app that signs into a Google account, mints a long-lived AAS token, and
syncs it to a [gplaydl dispenser](https://dispenser.gplaydl.com) so the
community can keep downloading from Google Play.

It replaces the copy-the-token-by-hand workflow: the app enrols itself with the
dispenser on first launch, so there is no signup, no password and no manual
paste step.

## How it works

```
consent  ->  enrol device  ->  Google sign-in  ->  mint AAS token  ->  sync
             (API key)         (WebView)          (AC2DM)            (dispenser)
```

1. **Consent.** The first screen spells out exactly what is uploaded (the token
   and the account's email address) and what is not (password, 2FA, cookies).
   The wording is versioned in `CONSENT_VERSION` and that version is recorded
   server-side against both the device and every shared account.
2. **Enrolment.** `POST /api/v1/devices/enroll` exchanges a locally generated
   32-byte device secret for an API key. The secret never leaves the app's
   private storage and doubles as the recovery credential, so re-installing
   recovers the same identity instead of orphaning shared accounts.
3. **Sign-in.** A WebView loads `accounts.google.com/EmbeddedSetup`, the only
   flow that yields an `oauth_token` cookie an unofficial Play client can use.
4. **Mint.** That cookie is POSTed to `android.clients.google.com/auth` with
   `service=ac2dm`, which returns the `aas_et/...` token.
5. **Sync.** `POST /api/v1/accounts` stores the token encrypted on the
   dispenser, public or private depending on the user's choice.

## Screens

| Screen | Purpose |
|--------|---------|
| Consent | Explains sharing, sets the default visibility, enrols the device |
| Accounts | Pool stats, per-account share switch, test and remove |
| Sign in | Google's embedded setup WebView |
| Open on the web | One-shot pairing code for reaching the dashboard in a browser |
| Settings | Default visibility, device name, dispenser URL, update check, sign out |

## Building

Requires the Android SDK with platform 36 and **JDK 21** — AGP does not support
JDK 25, so `gradle.properties` pins `org.gradle.java.home`. Adjust that path if
your JDK lives elsewhere.

```bash
# debug build
./gradlew :app:assembleDebug

# debug build pointed at a dispenser on your machine
./gradlew :app:assembleDebug -PdebugDispenserUrl=http://10.0.2.2:8099

# release build
./gradlew :app:assembleRelease
```

### Release signing

Create a keystore and a `signing.properties` in the project root (both are
gitignored). Without it, release builds fall back to the debug key and are not
suitable for distribution.

```bash
keytool -genkeypair -v -keystore gplaydl-authenticator.keystore \
  -alias gplaydl -keyalg RSA -keysize 4096 -validity 10000
```

```properties
storeFile=gplaydl-authenticator.keystore
storePassword=...
keyAlias=gplaydl
keyPassword=...
```

## Privacy

- The AAS token and the account email are the only things uploaded.
- The device secret and API key are stored in DataStore and excluded from
  cloud backup and device transfer (`res/xml/*_rules.xml`).
- Users can flip an account back to private, or delete it from the dispenser,
  from the accounts screen at any time. Revoking the app under Google account
  settings invalidates the token outright.

## Attribution

The AC2DM token exchange follows the approach used by Aurora Store's
Authenticator. This is an independent implementation: Compose UI, OkHttp
networking, and dispenser sync are new here.
