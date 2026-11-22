# finget-kotlin

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Official **Kotlin** SDK for the [Finget](https://finget.ir) public API (`/api/v1`).

Use it on your **server** (Ktor, Spring, plain JVM) to:

- Start **passwordless login** (“Sign in with Finget”)
- Exchange the one-time callback `code` for a Finget user
- Send **push notifications** to linked users
- Inspect site and notification delivery status

Zero third-party dependencies (JDK `HttpURLConnection` + a tiny built-in JSON helper).

> Keep your API key on the server only. Never put `FINGET_API_KEY` in Android/iOS app binaries or client-side code.

## Links

| | |
|---|---|
| Website | [finget.ir](https://finget.ir) |
| Plugins & SDKs | [finget.ir/plugins](https://finget.ir/plugins) |
| API docs | [Provider panel docs](https://finget.ir/provider/panel/docs) |
| Repository | [amirsafaridevs/finget-kotlin](https://github.com/amirsafaridevs/finget-kotlin) |
| JitPack | [jitpack.io/#amirsafaridevs/finget-kotlin](https://jitpack.io/#amirsafaridevs/finget-kotlin) |

## Requirements

- **JDK 11+**
- Kotlin **1.9+** (or any JVM language that can call the library)

## Installation

### JitPack (Gradle)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.amirsafaridevs:finget-kotlin:1.0.0")
}
```

### Local / ZIP

Download the ZIP from [finget.ir/plugins](https://finget.ir/plugins), then:

```kotlin
implementation(project(":finget-kotlin"))
// or publishToMavenLocal() and use implementation("ir.finget:sdk:1.0.0")
```

## Quick start

```bash
export FINGET_API_KEY=fg_live_...
```

```kotlin
import ir.finget.sdk.Client

val finget = Client.fromEnv()
// or: Client(apiKey = System.getenv("FINGET_API_KEY"))

val site = finget.getSite()
// site["site"] → domain, displayDomain, name, verifiedAt
```

## Authentication (Sign in with Finget)

1. In the Finget provider panel, copy your **site client ID** and register the callback URL.
2. Redirect the user to Finget with a CSRF `state` stored in the session.
3. On callback, verify `state`, then exchange `code`.

```kotlin
import ir.finget.sdk.ApiException
import ir.finget.sdk.Client

val finget = Client.fromEnv()
val clientId = System.getenv("FINGET_CLIENT_ID")

// Start login
val login = finget.loginUrl(clientId)
// store login["state"] in the session, then redirect to login["url"]

// Callback
try {
    val result = finget.exchangeCode(code)
    val user = result["user"] as Map<*, *>
    val isNew = result["isNew"] as Boolean
} catch (e: ApiException) {
    // handle e.status / e.errorCode
}
```

### `loginUrl()`

```kotlin
val login = finget.loginUrl(clientId)                 // generates state
val login = finget.loginUrl(clientId, state = state)  // reuse your own state
```

## Notifications

```kotlin
val result = finget.sendNotification(
    mapOf(
        "title" to "Order shipped",
        "body" to "Your order #1234 is on the way.",
        "userIds" to listOf("usr_..."),
        // optional: "url", "imageUrl", "data", ...
    )
)
```

## API

| Method | Description |
|---|---|
| `Client.fromEnv(apiKeyEnv = "FINGET_API_KEY")` | Build from environment variables |
| `getSite()` | Validate the API key; return site info |
| `loginUrl(clientId, state = null)` | Auth redirect URL + state |
| `createState(nbytes = 16)` | Random hex CSRF state |
| `exchangeCode(code)` | Exchange callback code → `{user, isNew}` |
| `getUser(userId)` | Refresh a linked user’s profile |
| `sendNotification(payload)` | Create a notification (HTTP 201) |
| `getNotification(id)` | Delivery status |

```kotlin
val finget = Client(
    apiKey = "fg_live_...",
    baseUrl = "https://finget.ir/api/v1",
    authUrl = "https://finget.ir/auth",
    timeoutMs = 20_000,
)
```

## Environment

| Variable | Required | Description |
|---|---|---|
| `FINGET_API_KEY` | yes (for `fromEnv`) | Site API key (`fg_live_…`) |
| `FINGET_BASE_URL` | no | Override API base (default `https://finget.ir/api/v1`) |
| `FINGET_AUTH_URL` | no | Override auth page (default `https://finget.ir/auth`) |

## License

MIT © Finget — see [LICENSE](LICENSE).
