# Ask AI — Project State

Updated: 20261125-083356
Package: com.akingstar.askai
Repository: akingstar/AskAI
Branch: main
Audit baseline: 9ab01a646102393f81557bb5ae0d822cd1498224

## Phase status
- Phase 1: Existing project foundation retained.
- Phase 2: Phone controls — coverage audit pending.
- Phase 3: Accessibility screen reading foundation exists.
- Phase 4: ActionPlan, PlanValidator and ActionExecutor exist.
- Phase 5: IN PROGRESS — Universal Control Discovery.
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
- Status: implemented; GitHub Actions verification pending.
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
- ActionExecutor routes to active Accessibility Service; read-only.
- Validator and offline parser tests added; CI verification pending.


## Safe Dynamic Control Matching
- Status: implemented; GitHub Actions verification required.
- Tap and long-press require one exact visible label match.
- Duplicate/missing labels fail closed; no first-substring selection.
- Tap and long-press require an enabled, visible clickable target.
- Text entry matches exact hint/content-description only, never existing field text.
- Password and SafetyGate-sensitive fields/values are rejected.
- Accessibility traversal is bounded to 300 nodes and depth 20.
- Risky labels are blocked for both tap and long-press.
- Unit tests cover exact matching, normalization and ambiguity.

## Hindi Screen Narration
- Status: implemented; GitHub Actions verification pending.
- discover_controls now speaks a concise Hindi description instead of raw JSON.
- Describes buttons, text fields, choices, scroll areas and disabled controls.
- Password fields are described generically; editable contents are never narrated.
- Duplicate descriptions are collapsed; spoken inventory is limited to 8 items.
- read_screen continues to read visible text as-is.
- Unit tests cover narration, duplicates, disabled controls and malformed inventory.
