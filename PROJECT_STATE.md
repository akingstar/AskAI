# Ask AI — Project State

Updated: 20261126
Package: com.akingstar.askai
Repository: akingstar/AskAI
Branch: ai-reliable-fallback-20261126
Audit baseline: 9ab01a646102393f81557bb5ae0d822cd1498224

## Phase status
- Phase 1: Existing project foundation retained.
- Phase 2: Phone controls — coverage audit pending.
- Phase 3: Accessibility screen reading foundation exists.
- Phase 4: ActionPlan, PlanValidator and ActionExecutor exist.
- Phase 5: COMPLETED — Universal Control Discovery and voice integration verified by CI.
- Phase 6: Security and final testing pending.

## Current module
Universal Control Discovery:
- Identify the active Manifest-registered AccessibilityService.
- Identify the ActionExecutor actually used by MainActivity.
- Reuse the existing accessibility bridge.
- Discover visible controls without collecting sensitive values.
- Keep PlanValidator, SafetyGate and ActionExecutor synchronized.
- Add tests before integration.

## Important discovery
Duplicate service and executor implementations exist.
Do not modify either until the active execution path is verified.

## Audit report
PHASE5_DISCOVERY_AUDIT.md

## Rules
Backup-first; one complete module at a time; no scratch rewrite;
no placeholders; no APK installation before explicit approval.

## Universal Control Discovery
- Status: implemented; GitHub Actions verification passed (run 45710467784).
- Active service: AskAIAccessibilityService.
- Inventory: clickable controls, editable fields, password-field type, scroll areas, labels, resource IDs and enabled state.
- Privacy: editable text values are excluded; password labels are generic; labels pass through SensitiveDataFilter.
- Limits: 300 visited nodes, 100 reported controls, depth 20; accessibility events throttled to 350 ms.
- Voice/action entry: discover_controls.
- Tests: ControlDiscoveryTest.


## Control Discovery Voice Integration
- PlanValidator accepts discover_controls only with empty target/value.
- OfflineParser recognizes Hindi and English control-discovery commands.
- AIClient schema instructions updated.
- ActionExecutor routes to active Accessibility Service; read-only. CI verification passed (run 45710467784).
- Validator and offline parser tests added; CI verification passed (run 45710467784).


## Safe Dynamic Control Matching
- Status: implemented; GitHub Actions verification passed (run 45710467784).
- Tap and long-press require one exact visible label match.
- Duplicate/missing labels fail closed; no first-substring selection.
- Tap and long-press require an enabled, visible clickable target.
- Text entry matches exact hint/content-description only, never existing field text.
- Password and SafetyGate-sensitive fields/values are rejected.
- Accessibility traversal is bounded to 300 nodes and depth 20.
- Risky labels are blocked for both tap and long-press.
- Unit tests cover exact matching, normalization and ambiguity.

## Hindi Screen Narration
- Status: implemented; GitHub Actions verification passed (run 45710467784).
- discover_controls now speaks a concise Hindi description instead of raw JSON.
- Describes buttons, text fields, choices, scroll areas and disabled controls.
- Password fields are described generically; editable contents are never narrated.
- Duplicate descriptions are collapsed; spoken inventory is limited to 8 items.
- read_screen continues to read visible text as-is.
- Unit tests cover narration, duplicates, disabled controls and malformed inventory.

## Dynamic Hindi / Hinglish Voice Commands
- Status: feature branch CI passed.
- CI run: 45543393917
- Unicode-preserving Hindi normalization implemented.
- Hindi tap, typing and long-press regression tests added.
- Sensitive input and risky-target checks retained.
- PR #1 merged; feature and main CI passed (runs 45543593917 and 45543742580). APK not installed.
- Updated: 20261125-092745


## Reliable AI + Offline Fallback
- Status: implemented; GitHub Actions verification passed.
- Feature branch: ai-reliable-fallback-20261126.
- Commit: 7ada8b6 — Add reliable offline AI fallback.
- CI run: 45710467784 — successful.
- Unit tests passed; debug APK built; artifact uploaded.
- Blank endpoint uses validated OfflineParser output.
- Eligible online failures can fall back to a validated offline plan:
  network errors, invalid responses, HTTP 429 and HTTP 5xx.
- Invalid/insecure endpoints and non-retryable client errors do not
  silently switch to offline mode.
- Offline plans are round-tripped through PlanValidator before use.
- Hindi, Hinglish and English web-search queries retain their search text.
- Main branch has not been changed by this feature branch.
- APK installation remains deferred until explicit user approval.

## AI Provider Integration + Usage & Cost Guard

- Gemini → Groq → HTTPS Worker → validated offline parser priority.
- Gemini and Groq API keys are stored using Android encrypted preferences.
- Provider responses pass through PlanValidator before becoming executable plans.
- API keys are sent in request headers, not URL query parameters.
- Daily online-attempt limit: 20 per local calendar day.
- Each outbound provider HTTP attempt is counted; Worker retries reserve separately.
- Duplicate normalized online commands are blocked for 5 seconds using a SHA-256 fingerprint; raw command text is not stored by the duplicate guard.
- Daily rollover resets the usage counter and duplicate fingerprint state.
- When the online limit is reached, the offline parser remains available.
- Settings displays today's online attempts and remaining attempts.
- This is an app-side request limiter, not a provider billing or spending cap. Provider dashboard quota/billing controls remain necessary.
- Usage policy tests cover the daily limit, day rollover, duplicate window, different commands and clock rollback.
- Feature branch: ai-reliable-fallback-20261126.
- Verification pending: GitHub Actions CI after this feature commit is pushed.
