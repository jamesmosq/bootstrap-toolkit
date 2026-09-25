package com.jamesmosquera.bootstraptoolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

/**
 * Checks the templates against the real files they load from the CDN. Copies of those files live in
 * src/test/resources/vendor/<npm package>@<version>/..., at the same path as on jsDelivr, so this runs offline.
 */
class BootstrapAssetsTest {

    private val templates = GeneratedTemplates.html
    private val cdnLink = Regex("""(?:href|src)="https://cdn\.jsdelivr\.net/npm/([^"]+)"[^>]*?integrity="([^"]+)"""")

    private fun vendored(path: String): ByteArray? = javaClass.getResourceAsStream("/vendor/$path")?.use { it.readBytes() }

    /** Every class selector defined in the vendored stylesheets (Bootstrap + Bootstrap Icons). */
    private val knownClasses: Set<String> by lazy {
        val stylesheets = templates.flatMap { t -> cdnLink.findAll(t.value).map { it.groupValues[1] } }.filter { it.endsWith(".css") }.toSet()
        assertTrue("no stylesheet linked from the templates", stylesheets.isNotEmpty())
        stylesheets.flatMap { path ->
            val css = requireNotNull(vendored(path)) { "missing vendored copy of $path" }.decodeToString()
            Regex("""\.(-?[_a-zA-Z][_a-zA-Z0-9-]*)""").findAll(css).map { it.groupValues[1] }.toList()
        }.toSet()
    }

    /**
     * Classes with no CSS rule that Bootstrap's JavaScript reads, e.g. `carousel slide` enables the animation
     * (`classList.contains("slide")`). Each one must really be referenced by the vendored JS bundle.
     */
    private val jsOnlyClasses = setOf("slide")

    /** Hook classes of the Bootstrap docs example scripts (vendored), e.g. `needs-validation` for form validation. */
    private val docsScriptClasses = mapOf("needs-validation" to "bootstrap-docs@5.3.8/validate-forms.js")

    @Test
    fun `js-only classes are really used by the bootstrap bundle`() {
        val bundle = templates.flatMap { t -> cdnLink.findAll(t.value).map { it.groupValues[1] } }.single { it.endsWith("bootstrap.bundle.min.js") }
        val js = requireNotNull(vendored(bundle)).decodeToString()
        jsOnlyClasses.forEach { assertTrue("'$it' is not referenced by $bundle", js.contains("classList.contains(\"$it\")")) }
    }

    @Test
    fun `docs script hook classes are really used by the docs scripts`() =
        docsScriptClasses.forEach { (cls, script) ->
            val js = requireNotNull(vendored(script)) { "missing vendored $script" }.decodeToString()
            assertTrue("'$cls' is not used by $script", js.contains("'.$cls'"))
        }

    @Test
    fun `integrity hashes match the files served by the cdn`() {
        val links = templates.flatMap { t -> cdnLink.findAll(t.value).map { t.name to it } }
        assertTrue(links.isNotEmpty())
        links.forEach { (name, m) ->
            val (path, integrity) = m.destructured
            val bytes = vendored(path)
            assertNotNull("$name: add a copy of https://cdn.jsdelivr.net/npm/$path to src/test/resources/vendor/$path", bytes)
            val hash = "sha384-" + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-384").digest(bytes))
            assertEquals("$name: integrity of $path", hash, integrity)
        }
    }

    @Test
    fun `every class used by every template and option exists in bootstrap or bootstrap icons`() {
        val classAttribute = Regex("""\sclass="([^"]*)"""")
        val unknown = templates.flatMap { t ->
            t.expansions().flatMap { markup -> classAttribute.findAll(markup).flatMap { it.groupValues[1].split(Regex("\\s+")) } }
                .filter { it.isNotEmpty() && it !in knownClasses && it !in jsOnlyClasses && it !in docsScriptClasses }
                .map { "${t.name}: .$it" }
        }.distinct()
        assertTrue("classes not defined by Bootstrap 5.3.8 / Bootstrap Icons 1.13.1:\n${unknown.joinToString("\n")}", unknown.isEmpty())
    }

    @Test
    fun `the class check catches a typo`() {
        assertTrue("btn-primary" in knownClasses && "bi-house" in knownClasses)
        assertTrue("btn-primry" !in knownClasses && "bi-hosue" !in knownClasses)
    }
}
