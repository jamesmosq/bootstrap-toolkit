package com.jamesmosquera.bootstraptoolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rules in CLAUDE.md on the XML generated from src/templates (see buildSrc).
 * Pure XML checks: they need no IDE and no network.
 */
class LiveTemplatesTest {

    private val html = GeneratedTemplates.html
    private val jsx = GeneratedTemplates.jsx
    private val pages = html.filter { it.isPage }
    private val components = html.filterNot { it.isPage }

    @Test
    fun `plugin description lists every template`() {
        val pluginXml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use { it.readBytes().decodeToString() }
        val description = pluginXml.substringAfter("<description>").substringBefore("</description>")
        val missing = html.map { it.name }.filter { "<code>${it}</code>" !in description }
        assertTrue("add to the plugin.xml description: $missing", missing.isEmpty())
    }

    @Test
    fun `groups are not empty`() {
        assertTrue(html.isNotEmpty())
        assertTrue(jsx.isNotEmpty())
    }

    @Test
    fun `every template has bs5 prefix, description and a context`() {
        (html + jsx).forEach {
            assertTrue("${it.name} must start with bs5-", it.name.startsWith("bs5-"))
            assertTrue("${it.name} needs a description", it.description.isNotBlank())
            assertTrue("${it.name} needs a context", it.contexts.isNotEmpty())
        }
    }

    @Test
    fun `jsx templates use className and never class or for`() {
        jsx.forEach {
            assertFalse("${it.name} (JSX) must not use class=", Regex("""\sclass\s*=""").containsMatchIn(it.value))
            assertFalse("${it.name} (JSX) must not use for=", Regex("""\sfor\s*=""").containsMatchIn(it.value))
            assertTrue("${it.name} (JSX) must use className=", it.value.contains("className="))
        }
    }

    @Test
    fun `html templates never use className`() =
        html.forEach { assertFalse("${it.name} (HTML) must not use className", it.value.contains("className")) }

    @Test
    fun `jsx group only targets jsx and tsx contexts, html group never does`() {
        jsx.forEach { assertEquals(setOf("JSX_HTML", "TSX_HTML"), it.contexts) }
        html.forEach { assertTrue(it.contexts.none { c -> c.startsWith("JSX") || c.startsWith("TSX") }) }
    }

    @Test
    fun `pages exist and only target plain html`() {
        assertTrue(pages.isNotEmpty())
        pages.forEach { assertEquals("${it.name} is a page", setOf("HTML"), it.contexts) }
    }

    @Test
    fun `cdn links pin one version per library and carry an integrity hash`() {
        // Hashes were recomputed from the real files; bump version and hash together (see CLAUDE.md).
        val pinned = mapOf("bootstrap" to "5.3.8", "bootstrap-icons" to "1.13.1")
        val cdn = Regex("""(?:href|src)="(https://cdn\.jsdelivr\.net/npm/([a-z0-9-]+)@([^/]+)/[^"]+)"([^>]*)>""")
        val links = html.flatMap { t -> cdn.findAll(t.value).map { t.name to it } }
        assertEquals("every pinned library is used", pinned.keys, links.map { it.second.groupValues[2] }.toSet())
        links.forEach { (name, m) ->
            val (url, library, version, rest) = m.destructured
            assertEquals("$name: $url must use $library ${pinned[library]}", pinned[library], version)
            assertTrue("$name: $url needs integrity + crossorigin",
                Regex("""integrity="sha384-[A-Za-z0-9+/=]+"""").containsMatchIn(rest) && rest.contains("crossorigin="))
        }
    }

    @Test
    fun `every html component has a jsx counterpart and vice versa`() =
        assertEquals(components.map { it.name }.toSet(), jsx.map { it.name }.toSet())

    @Test
    fun `jsx markup has no html-only syntax`() {
        val unclosedVoid = Regex("""<(area|base|br|col|embed|hr|img|input|link|meta|source|track|wbr)\b[^<>]*(?<!/)>""")
        jsx.forEach {
            assertFalse("${it.name} (JSX) has an HTML comment", it.value.contains("<!--"))
            assertFalse("${it.name} (JSX) has a string style", Regex("""\sstyle\s*=\s*["']""").containsMatchIn(it.value))
            assertFalse("${it.name} (JSX) has an unclosed void element", unclosedVoid.containsMatchIn(it.value))
        }
    }

    @Test
    fun `html and jsx versions share the same variables and description`() {
        val jsxByName = jsx.associateBy { it.name }
        components.forEach {
            val other = jsxByName.getValue(it.name)
            assertEquals("${it.name} variables differ", it.variables, other.variables)
            assertEquals("${it.name} descriptions differ", "${it.description} (JSX/TSX)", other.description)
        }
    }

    @Test
    fun `no bootstrap 4 syntax`() {
        val bs4 = Regex("""data-toggle|data-target|\bml-\d|\bmr-\d|\bpl-\d|\bpr-\d|text-left|text-right|float-left|float-right|badge-\w+|sr-only|form-group|btn-block""")
        (html + jsx).forEach { assertFalse("${it.name} uses Bootstrap 4 syntax", bs4.containsMatchIn(it.value)) }
    }

    @Test
    fun `no dead or script-dependent image placeholders`() =
        (html + jsx).forEach {
            assertFalse("${it.name} uses a dead or script-dependent placeholder", Regex("""holder\.js|via\.placeholder""").containsMatchIn(it.value))
        }
}
