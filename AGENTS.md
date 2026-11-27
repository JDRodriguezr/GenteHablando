# Repository guidance

Read this file before changing the repository. Read [CONTRIBUTING.md](CONTRIBUTING.md)
for commands, CI checks and release preparation, and
[docs/architecture.md](docs/architecture.md) when working on application behavior.

## GitFlow and language

- Write commits, PR titles/descriptions, documentation and new project-facing prose
  in English. Preserve existing product localization and technical identifiers.
- Keep application-owned copy in Spanish default Android resources and matching
  English resources. Include courses, answers, accessibility names, errors and demo
  copy; preserve stable IDs and user-written content. Read the localization rules
  in `CONTRIBUTING.md` before adding text. Do not bypass the localization check by
  declaring product copy non-translatable or disguising it as a technical literal.
- Start ordinary work from `develop` on `feature/<short-kebab-case-name>`.
  Do not include a username or add another path segment.
- Open PRs using only these combinations:

  | Source | Target | Repository |
  | --- | --- | --- |
  | `feature/<short-kebab-case-name>` | `develop` | This repository or a fork |
  | `release/<major.minor.patch>` | `main` | This repository |
  | `main` | `develop` | This repository, to synchronize an authorized release |

  Feature names use lowercase ASCII letters, digits and single hyphens.
  Release numbers use three nonnegative integers without leading zeros.
  `release/0.1.0` identifies a preparation branch, not permission to publish.
- Create release branches from `develop`. After an authorized release merge into
  `main`, open a `main` -> `develop` PR to bring release changes back.
  Do not merge `develop` directly into `main`.
- Do not commit ordinary work to `main` or `develop`, force-push shared branches,
  bypass required checks, or treat a PR request as permission to merge, tag,
  publish, deploy or change repository access.
- Use short, natural English commit messages. Do not add coauthor trailers,
  automated signatures or tool attribution to commits or PRs.
- In an app-managed worktree, use its branch controls instead of raw branch
  renaming. If those controls produce a name outside this policy, preserve the
  unpublished work and report the blocker.

## Architecture and behavior

The application uses one Android module, Kotlin, Jetpack Compose, ViewModels,
Hilt, Firebase and Agora. Keep changes within the existing boundaries:

- Keep Compose screens and navigation in `ui`, state and UI coordination in
  `viewmodel`, models and repository contracts in `domain`, SDK implementations
  in `data/repository`, and bindings in `di`.
- Use domain repository interfaces and existing Hilt bindings. Keep Firebase
  and Agora calls out of screens. Prefer an existing helper over a new layer.
- Add a use case only when nontrivial shared business rules justify it. Do not
  add modules, generic repository frameworks, event buses or scaffolding for
  hypothetical future features.
- Preserve observable loading, success and error states, including `Resource`
  where the existing contract uses it. Keep cancellation meaningful and surface
  actionable failures instead of converting them to empty data or fake success.
- Preserve authentication, ownership checks, consent, permission handling,
  microphone privacy, listener cleanup and Agora engine/channel lifecycle.
  Do not weaken TLS or expose private credentials in logs, source or artifacts.
- Verify the selected DI implementation before describing behavior. Firebase
  implementations currently supply auth, chat and matchmaking; fake
  implementations also exist. Document intentional demo/test behavior, fixed
  test-room limitations and required backend configuration. Do not silently
  replace a failing real service with a fake implementation.
- Firebase client identifiers in `app/google-services.json` are not private
  signing keys or admin credentials. Keep the required client configuration;
  keep service-account keys, signing material and reusable private tokens out
  of commits.
- Keep prototype claims honest. APK generation does not establish production
  readiness, valid Agora tokens, working Firebase rules or device behavior.

## Verification and handoff

- Add focused tests for changed nontrivial behavior and relevant failure cases.
  Do not replace those tests with assertions about constants or source text.
- Use the GitFlow policy tests, Android debug unit tests, Android lint and debug
  APK build in Actions. Keep failures visible; do not add placeholder checks,
  disable lint rules wholesale or suppress unsupported toolchain warnings.
- Run the localization guard and its regression tests in Actions. Keep both
  languages complete and formatter arguments, array ordering and plural meanings
  consistent; Android lint alone does not catch hardcoded Compose/course text.
- Do not run local Gradle builds/tests or install local build tools for this
  workflow. Use CI for build validation and an authorized physical Android
  device for runtime checks; do not introduce an emulator requirement.
- Workflow changes must retain PR and feature/release push triggers so the first
  workflow PR can run before a release reaches the default branch. Do not use
  path filters that leave required checks missing.
- Treat PR metadata as untrusted data. Keep Actions permissions read-only,
  pin external actions to verified full commit SHAs, and do not expose secrets
  to PR code or run PR code under `pull_request_target`.
- Report the exact source SHA, CI run/check conclusion and artifact name. State
  which runtime, accessibility or integration checks did not run. A blocked or
  unpublished change has no verified CI result or distributable APK.
- GitFlow checks validate PRs; they cannot prevent direct pushes or protect
  their own workflow from an unreviewed edit. An administrator must configure
  branch protection and review requirements described in `CONTRIBUTING.md`.
