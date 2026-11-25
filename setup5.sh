#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
cd ~/AskAI

echo "========== ASKAI PHASE 5: ACCESSIBILITY INTEGRATION =========="

# Allow only this script itself to be untracked.
DIRTY="$(git status --porcelain | grep -vE '^\?\? setup5\.sh$' || true)"
if [ -n "$DIRTY" ]; then
  echo "ERROR: Other uncommitted changes detected:"
  echo "$DIRTY"
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$HOME/AskAI-phase5-backup-$STAMP"
mkdir -p "$BACKUP"

FILES=(
  app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt
  app/src/main/java/com/akingstar/askai/ActionExecutor.kt
  app/src/main/java/com/akingstar/askai/ActionPlan.kt
  app/src/main/java/com/akingstar/askai/AIClient.kt
  app/src/main/java/com/akingstar/askai/SafetyGate.kt
  app/src/main/java/com/akingstar/askai/OfflineParser.kt
  app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt
  app/src/test/java/com/akingstar/askai/SafetyGateTest.kt
)

for f in "${FILES[@]}"; do
  test -f "$f" || { echo "Missing file: $f"; exit 1; }
  mkdir -p "$BACKUP/$(dirname "$f")"
  cp "$f" "$BACKUP/$f"
done

echo "Backup created: $BACKUP"

python3 - <<'PY'
from pathlib import Path

changes = {}

def load(path):
    return changes.get(path, Path(path).read_text())

def replace_once(path, old, new):
    text = load(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"{path}: expected exactly one anchor, found {count}"
        )
    changes[path] = text.replace(old, new, 1)

# -------------------------------------------------------
# 1. Accessibility service lifecycle and main-thread bridge
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt"

replace_once(
    p,
    """class AskAIAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}""",
    """class AskAIAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun performCommand(
        action: String,
        value: String,
        target: String
    ): String? {
        return when (action) {
            "tap_text" ->
                if (target.isNotBlank() && tapText(target)) "टैप कर दिया।" else null

            "long_press_text" ->
                if (target.isNotBlank() && longPressText(target)) "लॉन्ग प्रेस किया।" else null

            "type_text" ->
                if (target.isNotBlank() && setText(value, target)) "टेक्स्ट लिख दिया।" else null

            "scroll_up" ->
                if (scroll("up")) "ऊपर स्क्रॉल किया।" else null

            "scroll_down" ->
                if (scroll("down")) "नीचे स्क्रॉल किया।" else null

            "home" ->
                if (global("home")) "होम स्क्रीन खोल दी।" else null

            "back" ->
                if (global("back")) "पीछे आ गया।" else null

            "recents" ->
                if (global("recents")) "हाल के ऐप खोल दिए।" else null

            "notifications" ->
                if (global("notifications")) "नोटिफिकेशन खोल दिए।" else null

            "quick_settings" ->
                if (global("quick_settings")) "क्विक सेटिंग खोल दी।" else null

            "read_screen" ->
                readScreen().takeIf { it.isNotBlank() }

            else -> null
        }
    }

    companion object {
        @Volatile
        private var activeService: AskAIAccessibilityService? = null

        fun perform(
            action: String,
            value: String = "",
            target: String = ""
        ): String? {
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                return null
            }
            val service = activeService ?: return null
            return runCatching {
                service.performCommand(action, value, target)
            }.getOrNull()
        }
    }"""
)

replace_once(
    p,
    """    fun tapText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        val clickable = clickableParent(node) ?: node
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }""",
    """    fun tapText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        val clickable = clickableParent(node) ?: return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }"""
)

replace_once(
    p,
    """    fun longPressText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }""",
    """    fun longPressText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }"""
)

replace_once(
    p,
    """    fun setText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findEditable(root) ?: return false
        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        )
    }""",
    """    fun setText(text: String, fieldHint: String): Boolean {
        if (text.isBlank() || text.length > 500 ||
            fieldHint.isBlank() || fieldHint.length > 100
        ) return false

        val root = rootInActiveWindow ?: return false
        val matches = findEditables(root).filter { node ->
            listOfNotNull(
                node.text?.toString(),
                node.hintText?.toString(),
                node.contentDescription?.toString()
            ).any { it.contains(fieldHint, ignoreCase = true) }
        }

        // Never guess when the field hint matches zero or multiple fields.
        val node = matches.singleOrNull() ?: return false

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            android.os.Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
        )
    }"""
)

replace_once(
    p,
    """    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = when (direction) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return root.performAction(action)
    }""",
    """    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findScrollable(root) ?: return false
        val action = when (direction) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }
        return node.performAction(action)
    }"""
)

replace_once(
    p,
    """    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findEditable(it) }
            if (found != null) return found
        }
        return null
    }""",
    """    private fun findEditables(
        node: AccessibilityNodeInfo
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        fun collectEditable(current: AccessibilityNodeInfo) {
            if (current.isEditable) result.add(current)
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let(::collectEditable)
            }
        }

        collectEditable(node)
        return result
    }

    private fun findScrollable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let(::findScrollable)
            if (found != null) return found
        }
        return null
    }"""
)

# -------------------------------------------------------
# 2. Safety policy
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/SafetyGate.kt"

replace_once(
    p,
    """    fun needsConfirmation(action: String, target: String): Boolean =
        action.lowercase(Locale.ROOT) in setOf(
            "call", "sms", "delete", "uninstall",
            "security_settings"
        ) || target.matches(Regex("\\\\+?[0-9]{7,15}"))""",
    """    private val riskyUiLabels = listOf(
        "send", "submit", "delete", "remove", "pay",
        "payment", "transfer", "purchase", "buy", "confirm",
        "भेजें", "भेजो", "डिलीट", "हटाएं", "हटाओ",
        "भुगतान", "खरीद", "पुष्टि"
    )

    private val sensitiveFieldLabels = listOf(
        "password", "passcode", "pin", "otp",
        "verification code", "security code", "cvv",
        "पासवर्ड", "पिन", "ओटीपी"
    )

    fun isRiskyTapTarget(target: String): Boolean {
        val normalized = normalize(target)
        return riskyUiLabels.any { normalized.contains(normalize(it)) }
    }

    fun isSensitiveInputTarget(target: String): Boolean {
        val normalized = normalize(target)
        return sensitiveFieldLabels.any { normalized.contains(normalize(it)) }
    }

    fun needsConfirmation(action: String, target: String): Boolean =
        action.lowercase(Locale.ROOT) in setOf(
            "call", "sms", "delete", "uninstall",
            "security_settings", "tap_text", "long_press_text",
            "type_text"
        ) || target.matches(Regex("\\\\+?[0-9]{7,15}"))"""
)

# -------------------------------------------------------
# 3. Action plan schema and argument validation
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/ActionPlan.kt"

replace_once(
    p,
    """    private val allowed = setOf(
        "open_app", "alarm", "timer", "camera", "selfie", "gallery",""",
    """    private val allowed = setOf(
        "tap_text", "long_press_text", "type_text",
        "scroll_up", "scroll_down", "home", "back",
        "recents", "notifications", "quick_settings",
        "read_screen", "accessibility_settings",
        "open_app", "alarm", "timer", "camera", "selfie", "gallery","""
)

replace_once(
    p,
    """                if (action !in allowed ||
                    target.length > 160 ||
                    value.length > 500 ||
                    (action == "open_app" && target.isBlank() && value.isBlank())
                ) return null""",
    """                if (action !in allowed ||
                    target.length > 160 ||
                    value.length > 500 ||
                    (action == "open_app" && target.isBlank() && value.isBlank())
                ) return null

                if (action in setOf("tap_text", "long_press_text") &&
                    (target.isBlank() || value.isNotBlank())
                ) return null

                if (action == "type_text" &&
                    (target.isBlank() || value.isBlank())
                ) return null

                if (action in setOf(
                    "scroll_up", "scroll_down", "home", "back",
                    "recents", "notifications", "quick_settings",
                    "read_screen", "accessibility_settings"
                ) && (target.isNotBlank() || value.isNotBlank())
                ) return null"""
)

# -------------------------------------------------------
# 4. Action executor integration
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/ActionExecutor.kt"

replace_once(
    p,
    """        if (SafetyGate.blocked(sensitiveText) ||
            SafetyGate.isNeverAllowedAction(step.action)
        ) {""",
    """        val riskyUiTap =
            step.action == "tap_text" &&
                SafetyGate.isRiskyTapTarget(step.target)

        val sensitiveField =
            step.action == "type_text" &&
                SafetyGate.isSensitiveInputTarget(step.target)

        if (SafetyGate.blocked(sensitiveText) ||
            SafetyGate.isNeverAllowedAction(step.action) ||
            riskyUiTap ||
            sensitiveField
        ) {"""
)

replace_once(
    p,
    """        return when (step.action) {
            "open_app", "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar" -> {""",
    """        return when (step.action) {
            "tap_text" ->
                accessibilityAction("tap_text", target = step.target)

            "long_press_text" ->
                accessibilityAction("long_press_text", target = step.target)

            "type_text" ->
                accessibilityAction(
                    "type_text",
                    value = step.value,
                    target = step.target
                )

            "scroll_up" -> accessibilityAction("scroll_up")
            "scroll_down" -> accessibilityAction("scroll_down")
            "home" -> accessibilityAction("home")
            "back" -> accessibilityAction("back")
            "recents" -> accessibilityAction("recents")
            "notifications" -> accessibilityAction("notifications")
            "quick_settings" -> accessibilityAction("quick_settings")

            "read_screen" -> {
                val screen = AskAIAccessibilityService.perform("read_screen")
                if (screen.isNullOrBlank()) {
                    speak("स्क्रीन पढ़ने के लिए Accessibility अनुमति चालू करें।")
                    false
                } else {
                    speak(screen.take(700))
                    true
                }
            }

            "accessibility_settings" ->
                openIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

            "open_app", "phonepe", "paytm", "gpay", "irctc", "jio", "airtel", "hotstar" -> {"""
)

replace_once(
    p,
    """    private fun openPackage(pkg: String): Boolean {""",
    """    private fun accessibilityAction(
        action: String,
        value: String = "",
        target: String = ""
    ): Boolean {
        val result = AskAIAccessibilityService.perform(action, value, target)

        if (result == null) {
            speak(
                "यह काम नहीं हो सका। Ask AI की Accessibility अनुमति चालू करें " +
                    "और सही स्क्रीन खुली रखें।"
            )
            return false
        }

        if (result.isNotBlank()) speak(result)
        return true
    }

    private fun openPackage(pkg: String): Boolean {"""
)

# -------------------------------------------------------
# 5. AI action contract
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/AIClient.kt"

replace_once(
    p,
    """Never invent phone numbers. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Use only supported safe actions.""",
    """Never invent phone numbers. Never execute calls, SMS, contact access, payments, money transfers, OTP, PIN, password, factory reset, deletion or uninstall actions. Accessibility actions: tap_text and long_press_text use target as the exact visible label and require empty value; type_text uses target as the exact field hint and value as the exact text; scroll_up, scroll_down, home, back, recents, notifications, quick_settings and read_screen require empty target and value. accessibility_settings opens Android Accessibility settings. Never guess labels or field hints. Never type passwords, OTPs, PINs, CVV or payment credentials. Never tap Send, Submit, Delete, Remove, Pay, Transfer, Purchase or similar risky controls. Use only supported safe actions."""
)

# -------------------------------------------------------
# 6. Offline navigation commands
# -------------------------------------------------------
p = "app/src/main/java/com/akingstar/askai/OfflineParser.kt"

replace_once(
    p,
    """    private val commands = listOf(
        listOf("torch", "flashlight", "torch on", "flashlight on", "टॉर्च", "टॉर्च जलाओ", "torch jalao"),""",
    """    private val commands = listOf(
        listOf("home", "go home", "home screen", "होम जाओ", "होम स्क्रीन", "होम खोलो"),
        listOf("back", "go back", "पीछे जाओ", "वापस जाओ", "बैक करो"),
        listOf("recents", "recent apps", "हाल के ऐप", "हाल के ऐप दिखाओ"),
        listOf("notifications", "show notifications", "नोटिफिकेशन दिखाओ", "सूचनाएं दिखाओ"),
        listOf("quick_settings", "quick settings", "क्विक सेटिंग", "क्विक सेटिंग खोलो"),
        listOf("scroll_up", "scroll up", "ऊपर स्क्रॉल"),
        listOf("scroll_down", "scroll down", "नीचे स्क्रॉल"),
        listOf("read_screen", "read screen", "स्क्रीन पढ़ो", "यहाँ क्या लिखा है"),
        listOf("accessibility_settings", "accessibility settings", "एक्सेसिबिलिटी सेटिंग"),
        listOf("torch", "flashlight", "torch on", "flashlight on", "टॉर्च", "टॉर्च जलाओ", "torch jalao"),"""
)

# -------------------------------------------------------
# 7. Unit tests
# -------------------------------------------------------
p = "app/src/test/java/com/akingstar/askai/SafetyGateTest.kt"

replace_once(
    p,
    """    @Test
    fun rejectsUnsupportedSensitiveActions() {""",
    """    @Test
    fun accessibilityTextActionsRequireConfirmation() {
        assertTrue(SafetyGate.needsConfirmation("tap_text", "Settings"))
        assertTrue(SafetyGate.needsConfirmation("long_press_text", "Photo"))
        assertTrue(SafetyGate.needsConfirmation("type_text", "Search"))
    }

    @Test
    fun blocksRiskyUiLabelsAndSensitiveFields() {
        assertTrue(SafetyGate.isRiskyTapTarget("Send"))
        assertTrue(SafetyGate.isRiskyTapTarget("भुगतान करें"))
        assertFalse(SafetyGate.isRiskyTapTarget("Settings"))
        assertTrue(SafetyGate.isSensitiveInputTarget("Password"))
        assertTrue(SafetyGate.isSensitiveInputTarget("OTP"))
        assertFalse(SafetyGate.isSensitiveInputTarget("Search"))
    }

    @Test
    fun rejectsUnsupportedSensitiveActions() {"""
)

p = "app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt"

replace_once(
    p,
    """    @Test fun acceptsExactSchema() {""",
    """    @Test fun acceptsAccessibilityNavigationActions() {
        val actions = listOf(
            "scroll_up", "scroll_down", "home", "back",
            "recents", "notifications", "quick_settings",
            "read_screen", "accessibility_settings"
        )
        actions.forEach { action ->
            val json = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"$action","target":"","value":""}]}""" + '"""' + """
            assertNotNull("$action should be accepted", PlanValidator.parse(json))
        }
    }

    @Test fun acceptsTextActionsWithRequiredArguments() {
        val tap = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"tap_text","target":"Settings","value":""}]}""" + '"""' + """
        val press = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"long_press_text","target":"Photo","value":""}]}""" + '"""' + """
        val type = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":"weather"}]}""" + '"""' + """
        assertNotNull(PlanValidator.parse(tap))
        assertNotNull(PlanValidator.parse(press))
        assertNotNull(PlanValidator.parse(type))
    }

    @Test fun rejectsAmbiguousOrMalformedTextActions() {
        val noTarget = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"tap_text","target":"","value":""}]}""" + '"""' + """
        val typeNoHint = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"type_text","target":"","value":"hello"}]}""" + '"""' + """
        val typeNoValue = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"type_text","target":"Search","value":""}]}""" + '"""' + """
        val navWithTarget = """ + '"""' + """{"language":"hi-IN","steps":[{"action":"home","target":"YouTube","value":""}]}""" + '"""' + """
        assertNull(PlanValidator.parse(noTarget))
        assertNull(PlanValidator.parse(typeNoHint))
        assertNull(PlanValidator.parse(typeNoValue))
        assertNull(PlanValidator.parse(navWithTarget))
    }

    @Test fun acceptsExactSchema() {"""
)

new_test = Path("app/src/test/java/com/akingstar/askai/AccessibilityOfflineParserTest.kt")
if new_test.exists():
    raise RuntimeError(f"Refusing to overwrite {new_test}")

changes[str(new_test)] = """package com.akingstar.askai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AccessibilityOfflineParserTest {
    @Test
    fun parsesHindiNavigationCommands() {
        assertEquals("home", OfflineParser.parse("होम जाओ")?.steps?.first()?.action)
        assertEquals("back", OfflineParser.parse("वापस जाओ")?.steps?.first()?.action)
        assertEquals("recents", OfflineParser.parse("हाल के ऐप")?.steps?.first()?.action)
    }

    @Test
    fun parsesHindiScrollingAndScreenReading() {
        assertEquals("scroll_up", OfflineParser.parse("ऊपर स्क्रॉल")?.steps?.first()?.action)
        assertEquals("scroll_down", OfflineParser.parse("नीचे स्क्रॉल")?.steps?.first()?.action)
        assertEquals("read_screen", OfflineParser.parse("स्क्रीन पढ़ो")?.steps?.first()?.action)
    }

    @Test
    fun parsesAccessibilitySettings() {
        val plan = OfflineParser.parse("एक्सेसिबिलिटी सेटिंग")
        assertNotNull(plan)
        assertEquals("accessibility_settings", plan?.steps?.first()?.action)
    }
}
"""

# Validate every patch before writing any source file.
for path, content in changes.items():
    if not content:
        raise RuntimeError(f"Empty patch result: {path}")

for path, content in changes.items():
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(content)

print("Patched files:")
for path in changes:
    print("  " + path)
PY

echo
echo "========== DIFF VALIDATION =========="
git diff --check

echo
echo "========== REQUIRED IMPLEMENTATION CHECKS =========="
python3 - <<'PY'
from pathlib import Path

checks = {
    "app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt": [
        "onServiceConnected", "onUnbind", "onDestroy",
        "activeService", "performCommand", "findEditables",
        "findScrollable", "Looper.getMainLooper"
    ],
    "app/src/main/java/com/akingstar/askai/ActionExecutor.kt": [
        '"tap_text"', '"long_press_text"', '"type_text"',
        '"quick_settings"', '"read_screen"', "accessibilityAction"
    ],
    "app/src/main/java/com/akingstar/askai/ActionPlan.kt": [
        '"tap_text"', '"type_text"', '"quick_settings"',
        "fieldHint" if False else '"accessibility_settings"'
    ],
    "app/src/main/java/com/akingstar/askai/SafetyGate.kt": [
        "isRiskyTapTarget", "isSensitiveInputTarget",
        '"tap_text"', '"type_text"'
    ],
    "app/src/main/java/com/akingstar/askai/AIClient.kt": [
        "Accessibility actions:", "Never guess labels"
    ],
    "app/src/main/java/com/akingstar/askai/OfflineParser.kt": [
        '"home"', '"read_screen"', '"quick_settings"'
    ]
}

for filename, required in checks.items():
    content = Path(filename).read_text()
    for item in required:
        if item not in content:
            raise SystemExit(f"Missing {item!r} in {filename}")

print("Required source checks passed.")
PY

echo
echo "========== REVIEW SUMMARY =========="
git diff --stat
git status --short

echo
echo "========== COMMIT AND PUSH =========="
git add \
  app/src/main/java/com/akingstar/askai/AskAIAccessibilityService.kt \
  app/src/main/java/com/akingstar/askai/ActionExecutor.kt \
  app/src/main/java/com/akingstar/askai/ActionPlan.kt \
  app/src/main/java/com/akingstar/askai/AIClient.kt \
  app/src/main/java/com/akingstar/askai/SafetyGate.kt \
  app/src/main/java/com/akingstar/askai/OfflineParser.kt \
  app/src/test/java/com/akingstar/askai/PlanValidatorTest.kt \
  app/src/test/java/com/akingstar/askai/SafetyGateTest.kt \
  app/src/test/java/com/akingstar/askai/AccessibilityOfflineParserTest.kt \
  setup5.sh

git commit -m "Integrate safe accessibility voice actions"
git push origin main

echo
echo "========== PHASE 5 PUSHED =========="
echo "Backup: $BACKUP"
echo "Commit: $(git rev-parse HEAD)"
echo "Next: gh run list --repo akingstar/AskAI --limit 1"
