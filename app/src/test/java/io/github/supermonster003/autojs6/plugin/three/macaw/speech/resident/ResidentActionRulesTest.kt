package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wake words with their actions (roadmap P11.3, D186 - D188, D194, D220 - D226): ten rules, the checks and their
 * contract codes, the stored format and the 1.5.0 word list, the step of a hit and the path of a picked script.
 * zh-CN: 带动作的唤醒词 (路线图 P11.3, D186 - D188, D194, D220 - D226): 十条规则, 检查及其契约 code, 存储格式与 1.5.0 的词表, 命中的步骤与所选脚本的路径.
 */
class ResidentActionRulesTest {

    private val script = ResidentRule("Run it", ResidentAction.SCRIPT, "/storage/emulated/0/Scripts/run.js", "--quick 1")

    @Test
    fun `the actions and limits are those of the contract`() {
        assertEquals(
            listOf(SpeechContract.RESIDENT_ACTION_OPEN, SpeechContract.RESIDENT_ACTION_SCRIPT, SpeechContract.RESIDENT_ACTION_BROADCAST, SpeechContract.RESIDENT_ACTION_NOTIFY),
            ResidentAction.entries.map { it.id },
        )
        assertEquals(ResidentAction.SCRIPT, ResidentAction.of("script"))
        assertNull(ResidentAction.of("Script"))
        assertNull(ResidentAction.of(null))
        assertEquals("D188", 10, ResidentKeywords.MAX_KEYWORDS)
        assertEquals(SpeechLimits.MAX_RESIDENT_RULES, ResidentKeywords.MAX_KEYWORDS)
        assertEquals(SpeechLimits.MAX_RESIDENT_KEYWORD_CHARS, ResidentKeywords.MAX_CHARACTERS)
        assertEquals(SpeechLimits.MAX_RESIDENT_ARGUMENTS_CHARS, ResidentRules.MAX_ARGUMENTS_CHARS)
        assertEquals("old words open the sheet", ResidentKeywords.DEFAULT.map { ResidentRule(it) }, ResidentRules.DEFAULT)
    }

    @Test
    fun `ten rules pass, eleven exceed the limit`() {
        val ten = (1..10).map { ResidentRule("word $it", if (it % 2 == 0) ResidentAction.BROADCAST else ResidentAction.NOTIFY) }
        assertNull(ResidentRules.problem(ten))
        assertTrue(ResidentSettings(rules = ten).keywords.size == 10)
        val eleven = ResidentRules.problem(ten + ResidentRule("word 11"))
        assertNotNull(eleven)
        assertTrue("LIMIT_EXCEEDED", eleven!!.limit)
    }

    @Test
    fun `the checks name the first problem with its code`() {
        assertFalse("empty", ResidentRules.problem(emptyList())!!.limit)
        assertTrue("33 characters", ResidentRules.problem(listOf(ResidentRule("a".repeat(33))))!!.limit)
        assertFalse("blank", ResidentRules.problem(listOf(ResidentRule(" ")))!!.limit)
        assertFalse("repeat, case ignored", ResidentRules.problem(listOf(ResidentRule("Hey"), ResidentRule("hey")))!!.limit)
        assertNull(ResidentRules.problem(listOf(script)))
        assertNull("arguments are optional", ResidentRules.problem(listOf(script.copy(arguments = null))))
        assertFalse("script needs a path", ResidentRules.problem(listOf(script.copy(script = null)))!!.limit)
        assertFalse("relative path", ResidentRules.problem(listOf(script.copy(script = "Scripts/run.js")))!!.limit)
        assertFalse("two lines", ResidentRules.problem(listOf(script.copy(script = "/a.js" + Char(10) + "/b.js")))!!.limit)
        assertTrue("long path", ResidentRules.problem(listOf(script.copy(script = "/" + "a".repeat(1024))))!!.limit)
        assertTrue("long arguments", ResidentRules.problem(listOf(script.copy(arguments = "a".repeat(1025))))!!.limit)
        assertNull("1024 characters of arguments", ResidentRules.problem(listOf(script.copy(arguments = "a".repeat(1024)))))
        assertFalse("NUL", ResidentRules.problem(listOf(script.copy(arguments = "a" + Char(0))))!!.limit)
        assertFalse("a path for another action", ResidentRules.problem(listOf(ResidentRule("x", ResidentAction.OPEN, "/a.js")))!!.limit)
        assertFalse("arguments for another action", ResidentRules.problem(listOf(ResidentRule("x", ResidentAction.BROADCAST, arguments = "1")))!!.limit)
    }

    @Test
    fun `the stored rules round trip and reject what the checks reject`() {
        val rules = listOf(ResidentRule("Hey Macaw"), script, ResidentRule("Notify me", ResidentAction.NOTIFY), ResidentRule("Tell AutoJs6", ResidentAction.BROADCAST))
        assertEquals(rules, ResidentRules.decode(ResidentRules.encode(rules)))
        assertNull(ResidentRules.decode(null))
        assertNull(ResidentRules.decode("not json"))
        assertNull(ResidentRules.decode("[]"))
        assertNull(ResidentRules.decode("[1]"))
        assertNull("unknown action", ResidentRules.decode("""[{"keyword":"a","action":"launch"}]"""))
        assertNull("script as a number", ResidentRules.decode("""[{"keyword":"a","action":"script","script":5}]"""))
        assertEquals(listOf(ResidentRule("a")), ResidentRules.decode("""[{"keyword":"a","action":"open","script":null}]"""))
        assertNull("eleven", ResidentRules.decode(ResidentRules.encode((1..11).map { ResidentRule("w$it") })))
    }

    @Test
    fun `the 1_5_0 word list decides the words and keeps their actions`() {
        val stored = listOf(ResidentRule("Hey Macaw"), script)
        assertEquals("both agree", stored, ResidentRules.merge(listOf("Hey Macaw", "Run it"), stored))
        assertEquals("no word list", stored, ResidentRules.merge(null, stored))
        assertEquals("an invalid word list", stored, ResidentRules.merge(emptyList(), stored))
        assertEquals(
            "a 1.5.0 document: its words, the script kept for the same word (case ignored), a new word opens the sheet",
            listOf(script.copy(keyword = "run it"), ResidentRule("Hello")),
            ResidentRules.merge(listOf("run it", "Hello"), stored),
        )
        assertEquals("1.5.0 storage, upgraded", listOf(ResidentRule("a"), ResidentRule("b")), ResidentRules.merge(listOf("a", "b"), null))
        assertEquals("nothing stored", ResidentRules.DEFAULT, ResidentRules.merge(null, null))
        assertEquals(listOf(ResidentRule("x"), script), ResidentRules.withKeywords(stored, listOf("x", "Run it")))
    }

    @Test
    fun `a hit finds its rule by word`() {
        val rules = listOf(ResidentRule("Hey Macaw"), script)
        assertEquals(script, ResidentRules.ruleOf(rules, "Run it"))
        assertEquals(script, ResidentRules.ruleOf(rules, "run it"))
        assertNull(ResidentRules.ruleOf(rules, "other"))
        assertNull(ResidentRules.ruleOf(rules, null))
    }

    @Test
    fun `a hit takes the step of its action`() {
        ResidentAction.entries.forEach { action ->
            assertEquals("D79: nothing while the sheet shows", HitStep.IGNORE, HitStep.of(action, sheetVisible = true, canOpen = true, hostReceiver = true))
        }
        assertEquals(HitStep.OPEN_SHEET, HitStep.of(ResidentAction.OPEN, false, canOpen = true, hostReceiver = false))
        assertEquals("D71", HitStep.SPEAK_NOTICE, HitStep.of(ResidentAction.OPEN, false, canOpen = false, hostReceiver = false))
        assertEquals(HitStep.SPEAK_NOTICE, HitStep.of(ResidentAction.NOTIFY, false, canOpen = true, hostReceiver = true))
        assertEquals(HitStep.BROADCAST, HitStep.of(ResidentAction.BROADCAST, false, canOpen = false, hostReceiver = false))
        assertEquals("D220: 6.8.1 runs it in the background", HitStep.RUN_HOST, HitStep.of(ResidentAction.SCRIPT, false, canOpen = false, hostReceiver = true))
        assertEquals("D222: an older host", HitStep.RUN_ACTIVITY, HitStep.of(ResidentAction.SCRIPT, false, canOpen = true, hostReceiver = false))
        assertEquals("D222: tap to run", HitStep.RUN_NOTICE, HitStep.of(ResidentAction.SCRIPT, false, canOpen = false, hostReceiver = false))
    }

    @Test
    fun `a picked file has a path on the external storage only`() {
        val authority = ResidentScriptPaths.EXTERNAL_STORAGE_AUTHORITY
        val primary = "/storage/emulated/0"
        assertEquals("/storage/emulated/0/Scripts/run.js", ResidentScriptPaths.fromDocument(authority, "primary:Scripts/run.js", primary))
        assertEquals("/storage/emulated/0/a.js", ResidentScriptPaths.fromDocument(authority, "primary:a.js", "$primary/"))
        assertEquals("/storage/1234-ABCD/x/y.js", ResidentScriptPaths.fromDocument(authority, "1234-ABCD:x/y.js", primary))
        assertNull("a volume root", ResidentScriptPaths.fromDocument(authority, "primary:", primary))
        assertNull("no volume", ResidentScriptPaths.fromDocument(authority, "Scripts/run.js", primary))
        assertNull("another provider", ResidentScriptPaths.fromDocument("com.android.providers.downloads.documents", "raw:/storage/emulated/0/Download/a.js", primary))
        assertNull(ResidentScriptPaths.fromDocument(authority, null, primary))
    }
}
