# Ask AI — Project State

Updated: 20261125-083259
Package: com.akingstar.askai
Repository: akingstar/AskAI
Branch: main
Baseline commit: 1502be130d93a3ece79cf048144541a4027f67cd

## Product goal
Hindi-first voice-first Android phone controller.
Universal Voice Automation Engine using AccessibilityService,
Control Discovery, ActionPlan, PlanValidator, ActionExecutor,
Accessibility bridge and Hindi AI.

## Phase status
- Phase 1 — Existing code foundation: existing project retained.
- Phase 2 — Phone controls: existing implementation; verify coverage.
- Phase 3 — Universal screen reading: existing accessibility foundation;
  dynamic control inventory integration pending verification.
- Phase 4 — Voice action engine: existing ActionPlan/validator/executor;
  preserve current safety gates.
- Phase 5 — App automation + Hindi AI: IN PROGRESS.
- Phase 6 — Security + final testing: PENDING.

## Next module
Universal Control Discovery:
- Discover visible buttons, text fields, labels and scroll containers.
- Reuse the existing AccessibilityService.
- Avoid collecting password, OTP, PIN or payment data.
- Keep AI output constrained by existing ActionPlan and PlanValidator.
- Execute only through existing SafetyGate and ActionExecutor.
- Add unit tests and GitHub Actions verification.

## Baseline source inventory
See the Phase 5 preflight backup:
/data/data/com.termux/files/home/AskAI-phase5-preflight-20261125-083259/architecture-files.txt

## Rules
- No scratch rewrite.
- Backup before every source change.
- One complete module at a time.
- No placeholder or fake functionality.
- No APK installation before explicit user approval.
- Android protected screens and restricted system actions remain restricted.
