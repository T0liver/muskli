# T-021 — Catalogue full sync

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-010 (format and version field from T-007; loader rules from D-021)
- **Refs:** D-005, D-021 · docs/ARCHITECTURE.md §2 · SPEC F-01 AC1

## Goal
When online, replace the stored published catalogue with a newer GitHub Pages bundle. Offline, the bundled library from T-010 stays usable.

## In scope
- GET the published catalogue URL from T-007 using the platform HTTP API (`HttpURLConnection`). No new HTTP library.
- Apply only when the remote version is newer. Same upsert rules as T-010: by stable id; never modify custom exercises; do not hard-delete a published exercise a workout references; archive unreferenced rows missing from the bundle.
- Sync failure or no network: keep the current library and do not block startup.
- Tests with a fake HTTP source: newer version applies, older/equal does not, failure leaves data unchanged, custom rows survive.

## Out of scope (do NOT do)
- Delta-sync. UI for sync status beyond a silent failure. Editing the catalogue format (that's T-007).

## Acceptance criteria
- [ ] A newer remote bundle upserts; an older or equal version does not.
- [ ] Network failure leaves the stored catalogue unchanged and the app still starts.
- [ ] Custom exercises are unchanged after sync.

## Verification
`./gradlew testDebugUnitTest`

## Handoff log
- 2026-10-28 DOCS: Created when D-021 split network sync out of T-010. Platform HTTP only.
