# Architecture review

## Scope and evidence

This review covers the domain models and repository contracts, data implementations, Hilt bindings, ViewModels, and their screen/navigation relationships at commit `56145e0945c21dffb0d008157f3d750b7f978e76`.

No production Kotlin changes accompany this report. The reviewer did not run Gradle, install dependencies, operate a device, contact Firebase or Agora, or measure runtime performance. Build/release configuration and physical-device captures belong to the coordinating review.

**Observed** means the cited source contains the behavior or missing guard. **Risk** describes its consequence under the stated conditions, not a reproduced production incident. Acceptance criteria below are proposed checks for a future correction; they are not passing test results.

Source links resolve beneath `app/src/main/java/com/example/gentehablando/`. Link labels give the remaining path and exact line range. Firebase rules, indexes, deployed cleanup jobs, Agora token validity, support staffing, and multi-user behavior remain unverified.

## Keep the existing structure

Keep the single `app` module, Hilt injection, domain contracts, data adapters, and UI/ViewModel separation. [di/RepositoryModule.kt:23-59](../app/src/main/java/com/example/gentehablando/di/RepositoryModule.kt#L23) binds real adapters to the contracts, and the ViewModels already receive those contracts through constructor injection.

Correct the inverted matchmaking dependency and session ownership before introducing more layers. The reviewed code does not justify a module split, a use-case class for each repository method, a second DI mechanism, or a new state-management framework. Preserve explicit test doubles; do not use them to hide live-service failures.

## Priorities

P0 blocks use for private conversations. P1 covers data correctness, session reliability, and eligibility. P2 covers maintainability and bounded resource use. IDs stay stable across the detailed findings below.

| Order | Priority | Improvement |
| --- | --- | --- |
| A01 | P0 | Isolate voice channels and remove the embedded test-token path. |
| A02 | P1 | Distinguish authenticated identity from a local identifier. |
| A03 | P1 | Revalidate and claim both matchmaking participants atomically. |
| A04 | P1 | Make feedback idempotent and preserve rooms until both participants finish. |
| A05 | P1 | Complete session/queue cleanup before leaving its owner. |
| A06 | P1 | Surface operation and stream failures without losing drafts. |
| A07 | P1 | Isolate course progress by identity and merge scores monotonically. |
| A12 | P1 | Gate navigation on loaded eligibility and make moderation time explicit. |
| A08 | P2 | Emit immutable course snapshots and cancel obsolete collectors. |
| A09 | P2 | Make inactivity and waiting timers follow actual session state. |
| A10 | P2 | Remove the domain contract's dependency on a ViewModel type. |
| A11 | P2 | Bound Firestore queries and avoid redundant typing writes. |
| A13 | P2 | Preserve quiz state across recreation and keep transitions out of composition. |

### A01. Isolate voice rooms

**Evidence:** [data/repository/AgoraVoiceRepositoryImpl.kt:111-141](../app/src/main/java/com/example/gentehablando/data/repository/AgoraVoiceRepositoryImpl.kt#L111) accepts `channelId` and `userId`, but joins the fixed `sala_prueba` channel with a random numeric UID and an embedded test token. [di/RepositoryModule.kt:41-46](../app/src/main/java/com/example/gentehablando/di/RepositoryModule.kt#L41) binds this adapter for the normal app.

**Observed / risk:** the adapter ignores the requested room identity. If the test configuration accepts these joins, people in separate text rooms can enter the same voice channel. The source alone does not establish whether the token remains valid.

**Smallest correction:** use the authorized room's channel and an owner-approved, short-lived token bound to that channel and participant. Remove the embedded credential; keep any test-channel behavior behind an explicit isolated demo binding. Until approved token provisioning exists, expose voice as unavailable instead of reporting a connection.

**Acceptance:** an adapter test confirms that two distinct room IDs produce distinct channel requests. An authorized non-production multi-user check confirms isolation, expiry handling, and muted entry. The UI must surface rejected joins. Credential revocation/provisioning requires the owner's separate authorization.

### A02. Separate identity from authentication

**Evidence:** [data/repository/FirebaseAuthRepositoryImpl.kt:31-46](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseAuthRepositoryImpl.kt#L31) returns `Resource.Success` with a local UID when anonymous sign-in fails. [The same file:67-103](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseAuthRepositoryImpl.kt#L67) synthesizes an ID for current-user and auth-state reads, including signed-out states. [viewmodel/ChatViewModel.kt:66-72](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L66) and [viewmodel/MatchmakingViewModel.kt:36-45](../app/src/main/java/com/example/gentehablando/viewmodel/MatchmakingViewModel.kt#L36) add another fallback.

**Observed / risk:** callers can receive a successful identity without a Firebase session. They can then attempt remote operations using an identifier that the backend may reject. The normal Home UI does require a non-anonymous account; the defect concerns the repository contract and alternate/later code paths, not proof of unauthorized backend access.

**Smallest correction:** have the live auth adapter return the actual Firebase UID or `null`, and return an explicit sign-in failure. Keep local/demo identities separate from the live contract. Remove the ViewModel-generated identities. Preserve coroutine cancellation when translating exceptions.

**Acceptance:** signed-out reads emit `null`; rejected and cancelled sign-in never emit success. Fake-backed tests confirm that chat and matchmaking make no remote request without an accepted authenticated identity.

### A03. Claim matchmaking candidates inside the transaction

**Evidence:** [data/repository/FirebaseMatchmakingRepositoryImpl.kt:61-92](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseMatchmakingRepositoryImpl.kt#L61) selects an unassigned listener from a query before the transaction. The transaction writes a new room and both assignment fields without reading either queue document.

**Observed / risk:** the transaction does not revalidate the candidate's availability. Concurrent speakers can select the same listener from stale results and attempt separate room assignments. This is a source-level race risk, not an observed multi-device result.

**Smallest correction:** read both queue documents in the transaction, verify that both still exist and remain eligible/unassigned, then create the room and assign both. Treat a lost claim as a retryable search outcome. Await the initial queue write before relying on its existence.

**Acceptance:** an approved isolated backend test with two simultaneous speakers and one listener produces one successful pairing for that listener. Cancelling either queued participant prevents a subsequent claim; denied writes reach an error state. Verify required rules and indexes separately.

### A04. Preserve feedback and conversation evidence

**Evidence:** [data/repository/FirebaseUserRepositoryImpl.kt:26-87](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseUserRepositoryImpl.kt#L26) reads the room, adds a feedback document with a generated ID, then updates reputation in a separate transaction. [The same file:94-105](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseUserRepositoryImpl.kt#L94) deletes messages and the room after one normal submission, without checking both participants' completion or the room's report flag.

**Observed / risk:** the second participant's later submission can fail because the first participant deleted the room. A retry can add another feedback record or reputation update. A normal submission can also attempt cleanup after the other participant reported the room; the cleanup path does not inspect that report.

**Smallest correction:** use one deterministic feedback identity per room/rater, and atomically guard its creation and aggregate update. Remove immediate destructive cleanup from the normal feedback path until an approved retention process checks both participants and report status. Preserve room evidence rather than guessing a retention duration.

**Acceptance:** both sequential submission orders succeed; retrying the same submission changes reputation once. A report concurrent with normal feedback retains the room and messages. Injecting a failure between feedback and reputation must not leave an unrecoverable partial update. Backend authorization and cleanup operations require separate verification.

### A05. Give cleanup a reliable owner

**Evidence:** [ui/screens/ChatScreen.kt:124-129](../app/src/main/java/com/example/gentehablando/ui/screens/ChatScreen.kt#L124) calls `leaveRoom()` and then navigates. [viewmodel/ChatViewModel.kt:190-215](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L190) launches the remote leave in `viewModelScope` and destroys the singleton voice engine on clearing. [ui/navigation/AppNavigation.kt:205-207](../app/src/main/java/com/example/gentehablando/ui/navigation/AppNavigation.kt#L205) requests a chat back-stack pop. [ui/screens/MatchmakingScreen.kt:38-50](../app/src/main/java/com/example/gentehablando/ui/screens/MatchmakingScreen.kt#L38) starts search without a BackHandler/disposal cancellation path; [data/repository/FirebaseMatchmakingRepositoryImpl.kt:101-110](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseMatchmakingRepositoryImpl.kt#L101) removes listeners/jobs on close but deletes the queue only through explicit cancellation, without awaiting it.

**Observed / risk:** navigation can dispose the chat owner while its suspending leave still runs. A system-back exit from matchmaking can remove local observers while leaving a remote queue entry. Actual back-stack disposal behavior needs a device check; the missing awaited cleanup and queue-removal path are visible in source.

**Smallest correction:** expose an explicit leaving/cancelling outcome, await required cleanup before successful navigation, and release microphone/listeners on each exit path even when the remote update fails. Own matchmaking work within its flow scope instead of a detached `CoroutineScope`. Handle system-back cancellation as well as the visible cancel button. Keep failures visible and cleanup idempotent.

**Acceptance:** synthetic delayed/failing repositories cover explicit exit, system back, and owner destruction. Each path releases local voice/listeners once, reports remote-cleanup failure, and leaves no active queue entry after a successful cancellation. Physical-device back-stack checks confirm the intended owner clears.

### A06. Surface failures and keep unsent text

**Evidence:** [viewmodel/ChatViewModel.kt:146-160](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L146) ignores typing/send results; [ui/screens/ChatScreen.kt:373-376](../app/src/main/java/com/example/gentehablando/ui/screens/ChatScreen.kt#L373) clears the draft immediately. [data/repository/FirebaseChatRepositoryImpl.kt:28-30](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseChatRepositoryImpl.kt#L28) closes a failed message stream, while [the same file:115-122](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseChatRepositoryImpl.kt#L115) emits an active room for missing/error snapshots. [data/repository/FirebaseMatchmakingRepositoryImpl.kt:39-48](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseMatchmakingRepositoryImpl.kt#L39) and [94-110](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseMatchmakingRepositoryImpl.kt#L94) do not expose async failures. [data/repository/AgoraVoiceRepositoryImpl.kt:42-44](../app/src/main/java/com/example/gentehablando/data/repository/AgoraVoiceRepositoryImpl.kt#L42) logs SDK errors without connection state.

**Observed / risk:** people can lose a draft after a failed send, see a missing/unreadable room as active, or wait without knowing the service failed. A closed flow can terminate its collecting job without a recoverable UI error state.

**Smallest correction:** reuse `Resource` for operation outcomes and add explicit connection/stream states where booleans cannot represent failure. Clear a draft only after accepted send; distinguish local pending data from remote acknowledgement if offline queuing remains supported. Collect errors into UI state, await tasks whose result matters, and rethrow `CancellationException` before broad exception translation. Preserve current Spanish error localization.

**Acceptance:** failing send retains the draft and offers retry; denied/deleted rooms never display a verified active state. Matchmaking and voice failures leave a visible recoverable outcome. Cancellation stops work without a fabricated success or replacement identity.

### A07. Isolate course progress and prevent score downgrades

**Evidence:** [data/repository/CourseRepositoryImpl.kt:23](../app/src/main/java/com/example/gentehablando/data/repository/CourseRepositoryImpl.kt#L23), [86-108](../app/src/main/java/com/example/gentehablando/data/repository/CourseRepositoryImpl.kt#L86), and [140-163](../app/src/main/java/com/example/gentehablando/data/repository/CourseRepositoryImpl.kt#L140) use device-wide score/cooldown keys. Login synchronization writes those cached scores to the current account without comparing the latest remote values. [viewmodel/AuthViewModel.kt:33-40](../app/src/main/java/com/example/gentehablando/viewmodel/AuthViewModel.kt#L33) publishes login success before synchronization completes.

**Observed / risk:** a later account on the device can inherit another account's cached course scores and cooldowns. A stale lower cached score can overwrite a higher remote score for the same course; `SetOptions.merge()` does not implement a maximum comparison.

**Smallest correction:** separate guest progress from per-account keys and define the guest-import decision explicitly. Rebind progress observation when authentication changes. Merge each score as `max(local, remote)` within a guarded remote update; expose sync failure without discarding local work.

**Acceptance:** account A, sign-out, guest use, and account B keep separate progress/cooldowns except for an explicit permitted guest import. A cached 4 never lowers a remote 5, including concurrent updates. Failed synchronization preserves progress and exposes a retryable state.

### A08. Use immutable snapshots and one collector per identity

**Evidence:** [data/repository/CourseRepositoryImpl.kt:88-113](../app/src/main/java/com/example/gentehablando/data/repository/CourseRepositoryImpl.kt#L88) emits a mutable map, mutates it, and emits the same object again. [viewmodel/CourseViewModel.kt:25-27](../app/src/main/java/com/example/gentehablando/viewmodel/CourseViewModel.kt#L25) stores that map in StateFlow. [viewmodel/ProfileViewModel.kt:39-55](../app/src/main/java/com/example/gentehablando/viewmodel/ProfileViewModel.kt#L39) cancels the old profile job but launches another untracked course collector for each eligible auth event. [ui/screens/ChatScreen.kt:60-69](../app/src/main/java/com/example/gentehablando/ui/screens/ChatScreen.kt#L60) uses `collectAsState()` rather than lifecycle-aware collection.

**Observed / risk:** mutating an already-emitted map can hide updates from equality-based state observation. Repeated auth events can accumulate course listeners and allow obsolete collectors to repopulate cleared state. Screen collection can continue while the host lifecycle is stopped.

**Smallest correction:** emit immutable copies from the course adapter and ensure local writes notify its observers. Use `flatMapLatest` or one tracked job for identity-dependent observation. Use lifecycle-aware screen collection with the existing Android stack; define separately whether a conversation should continue in the background.

**Acceptance:** two remote score updates produce two distinct observable snapshots. Repeated login/auth refresh leaves one course collector for the current identity, and sign-out cannot receive an old account's update. Guides and course detail agree after a local completion. Stopping a screen removes screen-only subscription work without silently terminating an approved active session.

### A09. Make timers match conversation activity

**Evidence:** [viewmodel/ChatViewModel.kt:48-52](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L48) exposes both speaking states, but [125-127](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L125) resets inactivity only for local speaking. [168-185](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L168) closes after five minutes plus a thirty-second warning. [viewmodel/MatchmakingViewModel.kt:51-74](../app/src/main/java/com/example/gentehablando/viewmodel/MatchmakingViewModel.kt#L51) launches a timer whose loop requires Searching before the asynchronous search necessarily emits it; both branches at 180 seconds perform the same action.

**Observed / risk:** a muted listener can time out while the partner speaks. A delayed initial search emission can leave the waiting counter at zero. The implementation does not provide a three-minute maximum or universal-role fallback despite its comments.

**Smallest correction:** count accepted remote voice activity as session activity and base elapsed time on a monotonic clock. Start/stop waiting-time observation from the search state transition instead of initial coroutine ordering. Remove the misleading timeout branch unless the product owner authorizes a real timeout/fallback policy.

**Acceptance:** fake-clock checks keep an active remote-speaking session open beyond five minutes and close a truly idle one after the existing warning interval. Delaying Searching still starts the counter; matching/cancelling stops it. Background/resume follows the explicitly chosen session policy.

### A10. Restore the domain boundary

**Evidence:** [domain/repository/MatchmakingRepository.kt:3-15](../app/src/main/java/com/example/gentehablando/domain/repository/MatchmakingRepository.kt#L3) imports `viewmodel.MatchmakingUiState`. [viewmodel/MatchmakingViewModel.kt:19-23](../app/src/main/java/com/example/gentehablando/viewmodel/MatchmakingViewModel.kt#L19) defines the type that both live and fake data adapters emit.

**Observed / risk:** domain and data depend on a presentation type. A UI-state change can force repository changes and complicate isolated domain/data testing.

**Smallest correction:** move the repository's matching outcome into the domain package and map it to display state in the ViewModel. Keep elapsed display time in presentation. Update the real adapter, fake adapter, and screen-facing ViewModel together; do not add a module or use-case wrapper for this move.

**Acceptance:** domain repository/model sources import neither UI nor ViewModel classes. Tests use the same domain contract for real-adapter seams and fakes, and existing role/room navigation preserves its behavior.

### A11. Bound data work before changing infrastructure

**Evidence:** [data/repository/FirebaseChatRepositoryImpl.kt:23-42](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseChatRepositoryImpl.kt#L23) observes all room messages and remaps/formats the complete snapshot on each emission. [data/repository/FirebaseMatchmakingRepositoryImpl.kt:61-96](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseMatchmakingRepositoryImpl.kt#L61) queries all opposite-role entries every two seconds and filters assignments in memory. [ui/screens/ChatScreen.kt:357-360](../app/src/main/java/com/example/gentehablando/ui/screens/ChatScreen.kt#L357) forwards every edit to [viewmodel/ChatViewModel.kt:146-151](../app/src/main/java/com/example/gentehablando/viewmodel/ChatViewModel.kt#L146), which writes typing status.

**Observed / risk:** message mapping and queue scans grow with retained data, and text editing can produce redundant writes. No read-cost, memory, latency, or frame-time measurement accompanies this observation.

**Smallest correction:** load a bounded recent-message page and retrieve older pages on demand, preserving chronological display. Query a bounded set of eligible queue candidates with deterministic ordering, then keep A03's transaction guard. Send typing state only on meaningful transitions, with a bounded expiry/debounce for stale indicators. Carry a sortable timestamp in the domain and format at the display boundary instead of creating a formatter per document.

**Acceptance:** a synthetic 1,000-message room initially loads no more than the agreed page size and older-page navigation introduces no duplicates. Queue responses obey the chosen query limit. Repeated nonblank keystrokes do not each write identical typing state; stopping/leaving clears it. Collect read counts and UI measurements in the authorized environment before claiming savings or introducing a server matcher.

### A12. Load eligibility before routing into a conversation

**Evidence:** [viewmodel/ProfileViewModel.kt:24](../app/src/main/java/com/example/gentehablando/viewmodel/ProfileViewModel.kt#L24) starts with a clean `UserProfile` rather than a loading state. [ui/screens/HomeScreen.kt:269-289](../app/src/main/java/com/example/gentehablando/ui/screens/HomeScreen.kt#L269) checks that profile for a ban. [ui/navigation/AppNavigation.kt:119-123](../app/src/main/java/com/example/gentehablando/ui/navigation/AppNavigation.kt#L119) routes successful login straight to guidelines. [domain/model/UserProfile.kt:23-44](../app/src/main/java/com/example/gentehablando/domain/model/UserProfile.kt#L23) reads wall-clock time directly and accepts future report timestamps into its active window.

**Observed / risk:** unknown profile state resembles an eligible new account, and the post-login route skips Home's eligibility check. Clock skew or future timestamps can distort the local report calculation. This does not establish whether deployed Firebase rules enforce eligibility.

**Smallest correction:** represent profile loading/error explicitly and use the same eligibility gate for Home and post-login navigation. Make the time argument explicit in the existing policy function and reject invalid/future report dates. Keep UI restrictions separate from authoritative backend enforcement; verify the latter before operational use.

**Acceptance:** delayed/failed profile loading never silently treats a user as cleared. A known suspended profile reaches the suspension UI rather than matchmaking from either entry path. Boundary tests cover exactly thirty days, seven-day forgiveness, and future timestamps. An authorized backend check establishes enforcement independently of UI gating.

### A13. Preserve quiz state and apply outcomes once

**Evidence:** [ui/screens/CourseDetailScreen.kt:36-83](../app/src/main/java/com/example/gentehablando/ui/screens/CourseDetailScreen.kt#L36) initializes mode from the initial progress snapshot and keeps mode/randomized questions in `remember`. [299-328](../app/src/main/java/com/example/gentehablando/ui/screens/CourseDetailScreen.kt#L299) saves completion or cooldown from effects inside result rendering.

**Observed / risk:** configuration recreation discards the current question order and selected answers. A later progress emission does not update an already-remembered initial mode. Re-entering result composition can repeat a persistence effect, although the current score comparison limits some duplicate writes.

**Smallest correction:** keep serializable question IDs/order, selections, and mode in saved presentation state. Apply completion/cooldown once from a quiz-submission transition in the existing CourseViewModel. Use an enum/sealed state instead of unchecked mode strings. Preserve the existing five-question, four-to-pass, ten-minute-cooldown behavior.

**Acceptance:** rotation/recreation preserves question order, answers, and cooldown deadline. Later progress loading selects the correct mode without overwriting an active quiz. A submitted result persists once; back/re-entry does not restart the penalty or submit progress again.

## Correction sequence and verification

Start with A01 and A02, then the pairing/feedback transactions and cleanup/error paths in A03-A06. Address eligibility before a multi-user demonstration. Fix course isolation and observation before treating progress as trustworthy. Domain-boundary and bounded-query work can follow without splitting the app.

The existing example tests check arithmetic in `app/src/test/java/com/example/gentehablando/ExampleUnitTest.kt` and the package name in `app/src/androidTest/java/com/example/gentehablando/ExampleInstrumentedTest.kt`. They do not cover the acceptance criteria above.

Use focused fake-backed ViewModel/domain checks in CI for failure, cancellation, clock boundaries, collector ownership, and state preservation. Use an explicitly approved isolated backend for transaction contention and authorization checks. Reserve Android runtime verification for the authorized physical device; this report does not authorize new test infrastructure, emulator execution, production data writes, deployments, or releases.

Before claiming operational readiness, obtain evidence for Firebase authentication/rules/indexes, room membership and report retention, Agora token provisioning and isolation, both participants' exit/feedback paths, and account data deletion. Account deletion currently calls Firebase Auth deletion in [data/repository/FirebaseAuthRepositoryImpl.kt:85-91](../app/src/main/java/com/example/gentehablando/data/repository/FirebaseAuthRepositoryImpl.kt#L85); the reviewed client does not itself establish erasure of Firestore records or account-scoped local progress.

## Evidence still needed

Genuine screenshots and exact build prerequisites remain with the coordinating review. Accessibility, multi-device conversation behavior, measured performance/cost, remote moderation/support operations, and complete erasure/retention guarantees remain unverified. No finding above claims those checks passed.
