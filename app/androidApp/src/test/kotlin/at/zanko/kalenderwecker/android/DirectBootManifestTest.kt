package at.zanko.kalenderwecker.android

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * After an overnight reboot the phone stays locked until the user unlocks it.
 * Alarms must be re-planned and able to ring before that, so every component
 * on the alarm path has to run in direct-boot mode.
 */
class DirectBootManifestTest {
    private val ns = "http://schemas.android.com/apk/res/android"
    private val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private fun component(name: String) =
        listOf("activity", "service", "receiver").flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
        }.single { it.getAttributeNS(ns, "name") == name }

    @Test
    fun gIVEN_alarmPathComponents_WHEN_manifestParsed_THEN_directBootAware() {
        listOf(".android.BootReceiver", ".android.AlarmReceiver", ".android.AlarmService", ".android.AlarmActivity")
            .forEach { assertEquals("true", component(it).getAttributeNS(ns, "directBootAware"), it) }
    }

    @Test
    fun gIVEN_bootReceiver_WHEN_manifestParsed_THEN_listensForLockedBoot() {
        val actions = component(".android.BootReceiver").getElementsByTagName("action")
        val names = (0 until actions.length).map { (actions.item(it) as org.w3c.dom.Element).getAttributeNS(ns, "name") }
        assertTrue("android.intent.action.LOCKED_BOOT_COMPLETED" in names)
        assertTrue("android.intent.action.BOOT_COMPLETED" in names)
    }
}
