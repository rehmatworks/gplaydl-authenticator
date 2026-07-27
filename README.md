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
| Consent | Explains sharing and enrols the device |
| Accounts | Per-account visibility switch, health, re-sign-in and remove |
| Sign in | Google's embedded setup WebView |
| Open on the web | One-shot pairing code for reaching the dashboard in a browser |
| Settings | Update check, API key, dispenser URL, disconnect |

## Building

Requires the Android SDK with platform 36 and **JDK 21** — AGP does not support
JDK 25. If `java` on your machine is newer, point Gradle at a JDK 21 in
`~/.gradle/gradle.properties` rather than in this repo, which has to stay
portable for CI:

```properties
org.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

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

## Releasing

Pushing a `vMAJOR.MINOR.PATCH` tag is the whole release process. `.github/workflows/release.yml`
derives the version from the tag, builds and signs the APK, attaches it to a
GitHub release, and publishes it to the dispenser.

```bash
git tag v1.1.3
git push origin v1.1.3
```

`versionCode` is computed as `major * 10000 + minor * 100 + patch`, so `v1.1.3`
becomes `10103`. The workflow rejects a tag whose minor or patch reaches 100,
because that would stop the codes increasing in order. `build.gradle.kts` still
carries a version for local builds; only the tag matters for published ones.

Before the first tagged release, add these repository secrets:

| Secret | Value |
|--------|-------|
| `SIGNING_KEYSTORE_BASE64` | `base64 -i gplaydl-authenticator.keystore` |
| `SIGNING_STORE_PASSWORD` | from `signing.properties` |
| `SIGNING_KEY_ALIAS` | from `signing.properties` |
| `SIGNING_KEY_PASSWORD` | from `signing.properties` |
| `DEPLOY_SSH_KEY` | the dispenser deploy key, same value the dispenser repo uses |
| `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_KNOWN_HOSTS` | as in the dispenser repo |

The workflow refuses to publish an APK that is not signed with the expected
certificate (`EXPECTED_CERT_SHA256` in the workflow), since a different key
produces a build that existing installs cannot update to.

Publishing on the server goes through `deploy/gplaydl-publish-apk`, which is
installed root-owned at `/usr/local/sbin/` and is the only command the deploy
user may run as root besides restarting the service. It re-checks the checksum,
refuses to move `versionCode` backwards, updates `/etc/gplaydl-dispenser/env`,
restarts the dispenser, and rolls back if it does not come back serving the new
release. See the header of that script for the install steps.

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
