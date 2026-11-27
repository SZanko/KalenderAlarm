package at.zanko.kalenderwecker.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class ExecResolverTest {
    @Test
    fun gIVEN_envVar_WHEN_resolve_THEN_envWins() {
        assertEquals(
            listOf("/usr/bin/kalenderwecker", "--verbose"),
            resolveExec(mapOf("KALENDERWECKER_EXEC" to " /usr/bin/kalenderwecker  --verbose "), "/usr/bin/java", listOf("-cp", "x")),
        )
    }

    @Test
    fun gIVEN_processCommand_WHEN_resolve_THEN_modeArgsStripped() {
        assertEquals(
            listOf("/usr/bin/java", "-cp", "a.jar", "at.zanko.kalenderwecker.MainKt"),
            resolveExec(
                emptyMap(),
                "/usr/bin/java",
                listOf("-cp", "a.jar", "at.zanko.kalenderwecker.MainKt", "--ring", "2026-11-30", "--force"),
            ),
        )
        assertEquals(
            listOf("/opt/kw/bin/kw"),
            resolveExec(emptyMap(), "/opt/kw/bin/kw", listOf("--refresh")),
        )
    }

    @Test
    fun gIVEN_nothingKnown_WHEN_resolve_THEN_empty() {
        assertEquals(emptyList(), resolveExec(emptyMap(), null, emptyList()))
    }
}
