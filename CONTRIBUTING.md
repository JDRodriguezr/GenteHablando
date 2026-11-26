# Contributing

GenteHablando is an Android prototype. Use English for commits, PRs and
documentation, and preserve the existing product localization.
Read [AGENTS.md](AGENTS.md) and [docs/architecture.md](docs/architecture.md)
before changing application behavior.

## Branches and pull requests

Keep `main` as the default branch and use `develop` for integration.
Use a short feature name without a username:

```powershell
git fetch origin
git switch --no-track -c feature/short-name origin/develop
# Make and review the scoped change.
git add <changed-files>
git commit -m "Describe the change"
git push -u origin feature/short-name
gh pr create --base develop --head feature/short-name --title "Describe the change" --body "Explain the change and its verification."
```

In app-managed worktrees, use the app's branch controls. Do not publish an
app-generated branch that does not match this policy.
Verify the intended authenticated GitHub account before any GitHub write.

The policy accepts exactly these combinations:

| Source | Target | Purpose |
| --- | --- | --- |
| `feature/<short-kebab-case-name>` | `develop` | Ordinary work, including fork PRs |
| `release/<major.minor.patch>` | `main` | Same-repository release preparation |
| `main` | `develop` | Same-repository release synchronization |

Feature slugs contain lowercase ASCII letters, digits and single hyphens.
Release versions contain three nonnegative integers without leading zeros.
For example, `feature/chat-errors` and `release/0.1.0` are valid;
`future/chat-errors`, `feature/user/chat-errors` and `release/01.0.0` are not.
This workflow does not introduce separate `hotfix/*` or `fix/*` routes.
Use a feature PR for an ordinary correction.

Do not work directly on long-lived branches, force-push shared branches, or
merge `develop` directly into `main`. Open PRs; obtain separate authorization
before merging, tagging, publishing, deploying or changing access.

## Actions and build prerequisites

The workflows run on PR creation, reopening, source updates and PR edits.
They also run on pushes to `feature/**`, `release/**`, `develop` and `main`,
so a first workflow change can run before it reaches the default branch.
They have no path filters, production secrets, emulator jobs, automatic merges,
tags or releases. Fork PRs run with a read-only token; an owner may need to approve
a first-time contributor's run in GitHub.

| Check | Work |
| --- | --- |
| `GitFlow policy` | Python standard-library policy tests and PR metadata validation |
| `Android CI` | `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` |

The Python tests live in `.github/scripts/test_gitflow_policy.py`.
The Android unit-test task selects the debug variant; the existing unit suite
currently contains only the template arithmetic test. A green run does not
imply coverage of authentication, matchmaking or voice calls.

The Android runner requests Temurin JDK 17, Android platform 35 and Build Tools
35.0.0. It uses the checked-in Gradle wrapper and version catalog.
[Google's AGP 8.8 compatibility table](https://developer.android.com/build/releases/agp-8-8-0-release-notes)
lists Gradle 8.10.2, JDK 17 and a maximum supported API level of 35.

**Unverified inherited build configuration:** the current wrapper requests
Gradle 9.6.0, daemon criteria request JDK 25, and the catalog uses Kotlin 2.1.20
and Compose BOM 2026.02.01. Daemon criteria can override `JAVA_HOME`.
Do not call this combination compatible or claim a successful build without
an actual Actions result. Diagnose the first failing CI run and change only
the necessary versions, preferring the supported AGP 8.8 toolchain over an
AGP-major migration. Verify any replacement distribution checksum against
Gradle's official checksum before committing it.

Do not run local Gradle builds/tests or install local build tools for this
workflow. Actions runs the following command on its Linux runner:

```sh
bash ./gradlew --no-daemon --stacktrace :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

After a successful build/check step, Actions uploads
`app/build/outputs/apk/debug/app-debug.apk` as
`gentehablando-debug-<source-sha>`, with seven-day retention.
The diagnostic artifact is `android-reports-<source-sha>`.
For PRs, the artifact name contains the PR head SHA; checkout/build uses GitHub's
test merge commit. Record both when reproducing a PR build.

Use these read-only commands to inspect and download a run after publication:

```powershell
gh pr view <pr-number> --json url,headRefOid,baseRefName,headRefName,statusCheckRollup
gh run list --branch feature/short-name --limit 10 --json databaseId,headSha,status,conclusion,url
gh run view <run-id> --json headSha,event,status,conclusion,url,jobs
gh run view <run-id> --log-failed
gh run download <run-id> --name gentehablando-debug-<source-sha> --dir .\ci-artifacts
```

Confirm the run/check corresponds to the current PR head, not an older green
commit. Use an authorized physical device for installation and smoke checks.
APK build success does not verify Firebase access, Agora token validity,
dynamic voice rooms, microphone consent or device behavior.
Keep required Firebase client configuration; do not add private signing keys,
service-account credentials or production tokens to make CI pass.

## Administrator setup

Only a repository administrator can apply the following settings.
Push access alone does not grant that ability. A 403 from
`GET /repos/{owner}/{repo}/actions/permissions` does not prove Actions is disabled;
inspect workflow runs or ask an administrator to inspect **Settings > Actions**.

After the workflows have produced real check runs, configure rulesets or branch
protection for both `main` and `develop`:

1. Require pull requests, at least one approval, and resolution of review
   conversations. Dismiss stale approvals after new commits.
2. Require `GitFlow policy` and `Android CI` from GitHub Actions, and require
   branches to be up to date before merging.
3. Block force pushes and branch deletion. Restrict direct pushes and bypasses,
   including administrator bypasses where the repository plan supports it.
4. Require owner/code-owner review for `.github/**`, `AGENTS.md` and build
   configuration changes. Assign actual maintainers before enabling code-owner
   review. PR-controlled Actions files can change their own checks; status-check
   requirements alone do not prevent that.
5. Keep the workflow token's default permission read-only. Review allowed actions
   and first-time fork contributor approvals without giving PR code secrets or
   write tokens.

These are owner instructions, not a claim that protection already exists.
Do not change administrative settings through another account or bypass a
denied permission.

## Manual release preparation

Release preparation does not authorize publication. Choose a version with the
owner; keep prototype/pre-release labeling honest.

```powershell
git fetch origin
git switch --no-track -c release/0.1.0 origin/develop
# Update approved version metadata and release documentation.
# Keep versionCode increasing; choose an honest versionName.
git add <changed-files>
git commit -m "Prepare release 0.1.0"
git push -u origin release/0.1.0
gh pr create --base main --head release/0.1.0 --title "Prepare release 0.1.0" --body "Describe the approved version and verification."
```

Inspect the exact PR SHA and both required checks. Obtain authorized device
smoke results and separate approval before merging or distributing a production
build. Signing, tags and GitHub releases need their own authorized manual steps;
the workflows do not perform them.

After an authorized release merge into `main`, synchronize it through a PR:

```powershell
git fetch origin
gh pr create --base develop --head main --title "Sync release changes to develop" --body "Bring the authorized release changes back to develop."
```

Resolve conflicts without rewriting shared history. Obtain separate approval
for the synchronization merge, then delete the release branch only after
confirming its changes reached both long-lived branches.
