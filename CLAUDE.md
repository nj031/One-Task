# One Task — working agreement

Kotlin + Jetpack Compose, Material 3, MVVM (`ui` / `viewmodel` / `data` /
`navigation` packages), package `com.nj031.onetask`, minSdk 26,
target/compile SDK 34.

## Standard workflow for every feature/change

Unless told otherwise for a specific request, follow this for every
feature or change:

1. Implement the requested feature/change.
2. Open a PR for the change.
3. Subscribe to the PR's activity so CI completion is delivered automatically.
4. Wait for the `Android CI` GitHub Actions workflow to finish (don't poll).
5. If CI fails: diagnose the actual failure (pull the job logs, don't
   guess), push a fix, and wait for CI again. Repeat until green.
6. Once CI is green and the PR is mergeable, merge it into `main`
   automatically — do not wait for explicit "merge it" approval.
7. After merging, confirm the `latest-debug` GitHub Release on `main` was
   updated with the new `app-debug.apk` (the release job in
   `.github/workflows/android-ci.yml` does this on every push to `main`;
   verify it actually ran and attached the asset, don't just assume).
8. Reply with: a short summary of what was built, the direct APK download
   link, and any assumptions made.

## CI / release infrastructure

`.github/workflows/android-ci.yml` has two jobs:
- `build`: runs `./gradlew assembleDebug` on every push (all branches) and
  uploads `app-debug.apk` as a workflow artifact.
- `release`: runs only on pushes to `main`, after `build` succeeds. It
  deletes and recreates the `latest-debug` release/tag pointing at the new
  commit, with `app-debug.apk` attached — this is the stable, bookmarkable
  download URL:
  `https://github.com/nj031/One-Task/releases/download/latest-debug/app-debug.apk`

## Notes for whoever (human or Claude) picks this up next

- The dev/build sandbox this project has been developed in has no network
  access to Google's Maven repo or an installed Android SDK, so builds
  can't be verified locally there — GitHub Actions CI is the real build
  verification. Don't report a task done on local build success alone.
- When restarting a designated branch whose PR already merged, restart it
  from the latest `main` rather than stacking new commits on old
  (already-merged) history.
