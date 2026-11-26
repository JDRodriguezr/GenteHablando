# GenteHablando

GenteHablando is an Android prototype for peer conversations. You choose whether to speak or listen, read the guidelines for that role, and request a conversation. The app also includes an "Academia" with short lessons and quizzes about listening, emotions, and communication.

The product UI is in Spanish. This repository's documentation and contribution workflow use English.

**Prototype status:** the source includes Firebase-backed messaging and matchmaking, Google sign-in, and an Agora voice adapter. These integrations do not establish production readiness. The voice adapter currently joins a shared test channel instead of the requested conversation room. Do not use this build for private conversations.

## What the source includes

| Area | Current implementation and limits |
| --- | --- |
| Home and roles | Speaker/listener selection, access to the academy, and a sign-in prompt before starting a conversation. |
| Guidelines | Different guidance for each role and a ten-second reading countdown before searching. The guidance describes peer accompaniment, not professional therapy. |
| Authentication | Google sign-in through Firebase Auth. The data layer also contains local identity fallbacks; a local identifier does not prove Firebase authentication. |
| Conversations | Firestore matchmaking, room messages, typing indicators, and session-ending actions. Multi-user reliability needs verification and the corrections in the architecture report. |
| Voice | Microphone and noise-filter controls plus speaking indicators. The adapter uses a shared test channel and an embedded test token; room isolation and token provisioning need correction. |
| Feedback and profile | Ratings, conduct reports, connection-issue feedback, appeals, reputation, sign-out, and account-deletion actions. The repository does not establish an operating moderation service or complete data-erasure process. |
| Academy | Three bundled lessons, randomized five-question quizzes, a passing score of four, and a ten-minute retry cooldown after a failed attempt. The implementation stores progress locally and attempts Firebase synchronization. |

The app requests an email scope during Google sign-in and stores messages, feedback, appeals, and progress through its data adapters. Generic partner labels do not guarantee anonymous authentication, confidential storage, or a retention policy.

## Screenshots

Runtime screenshots are not available. Include only genuine captures from an authorized physical-device run, without account details, private conversations, or credentials. Screen coverage, successful login, working conversations, and audio quality remain unverified.

## Stack

| Purpose | Technology present in the source |
| --- | --- |
| Android application | Kotlin, one `app` module |
| UI | Jetpack Compose and Material 3 |
| Navigation and state | Navigation Compose, Android ViewModels, coroutines, and Flow/StateFlow |
| Dependency injection | Hilt |
| Identity and remote data | Firebase Auth and Cloud Firestore |
| Google login | Google Play services sign-in |
| Voice | Agora RTC |
| Advertising | Google Mobile Ads |
| Local persistence | SharedPreferences for identity, course scores, and cooldowns |

The source review targets `56145e0945c21dffb0d008157f3d750b7f978e76`. A successful baseline build and toolchain compatibility remain unverified. Version metadata alone does not establish a stable release.

## Setup

Start from `develop`:

```powershell
git clone --branch develop https://github.com/JDRodriguezr/GenteHablando.git
Set-Location GenteHablando
```

Open the checkout in Android Studio. Before building or running it, verify the required JDK, Android SDK, Gradle, and Android plugin versions against the checked-out build configuration in an approved CI environment. Exact toolchain prerequisites remain unverified; do not assume compatible versions.

Use an owner-approved test environment for Firebase and Agora configuration. Google sign-in requires the matching Firebase/OAuth client configuration and the approved build's signing registration. Do not copy tokens, account details, signing secrets, or runtime logs into documentation.

Run builds and automated tests in the approved CI environment rather than through local Gradle tests. Use an authorized physical Android device for runtime checks; do not use Android emulators for this workflow. Obtain separate approval before production writes or deployment. A successful build alone does not verify Firebase rules, matchmaking, moderation, or voice isolation.

## Architecture

The app uses repository contracts, Hilt bindings, and ViewModels within a single module:

```text
ui/screens + ui/navigation
          |
          v
      viewmodel
          |
          v
  domain/repository + domain/model
          ^
          |
  data/repository -- Firebase / Agora / local preferences
          ^
          |
      di bindings
```

`RepositoryModule` binds the real Firebase implementations and the Agora adapter. The source also includes fake chat and matchmaking implementations, but the current bindings do not provide an isolated demo mode.

The matchmaking contract currently imports a ViewModel UI state, so that boundary still needs correction. Keep the single-module structure while addressing the concrete defects in the [architecture review](docs/architecture.md).

## Contribution workflow

Use `feature/*` branches based on `develop`. Keep commits and PR descriptions short and in English, and preserve the Spanish product localization.

Submit a PR to `develop`; merging, publishing tags or releases, and deploying require separate approval.

## Verification limits

The architecture review records source observations and proposed acceptance criteria, not passing checks. Baseline build compatibility, end-to-end conversations, accessibility conformance, and performance remain unverified. Do not treat the prototype as a stable release or evidence of operational readiness.
