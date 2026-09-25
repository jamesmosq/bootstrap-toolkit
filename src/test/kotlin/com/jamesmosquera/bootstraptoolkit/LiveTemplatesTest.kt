package com.jamesmosquera.bootstraptoolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the rules in CLAUDE.md on the XML generated from src/templates (see buildSrc).
 * Pure XML checks: they need no IDE and no network.
 */
class LiveTemplatesTest {

    private class Tpl(
        val name: String,
        val value: String,
        val description: String,
        val contexts: Set<String>,
        val variables: List<String>,
    )

    private fun load(resource: String): List<Tpl> {
        val stream = javaClass.getResourceAsStream(resource) ?: error("missing resource $resource")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        val templates = doc.getElementsByTagName("template")
        return (0 until templates.length).map { i ->
            val e = templates.item(i) as Element
            val options = e.getElementsByTagName("option")
            Tpl(
                name = e.getAttribute("name"),
                value = e.getAttribute("value"),
                description = e.getAttribute("description"),
                contexts = (0 until options.length).map { j -> options.item(j) as Element }
                    .filter { it.getAttribute("value") == "true" }
                    .map { it.getAttribute("name") }
                    .toSet(),
                variables = e.getElementsByTagName("variable").let { vars ->
                    (0 until vars.length).map { j -> (vars.item(j) as Element).let { v -> "${v.getAttribute("name")}=${v.getAttribute("defaultValue")}" } }
                },
            )
        }
    }

    private val html = load("/liveTemplates/BootstrapToolkitHtml.xml")
    private val jsx = load("/liveTemplates/BootstrapToolkitJsx.xml")

    /** Whole documents (`<!doctype html>`): HTML only, no JSX counterpart. */
    private val pages = html.filter { it.value.trimStart().startsWith("<!doctype", ignoreCase = true) }
    private val components = html - pages.toSet()

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
    fun `cdn links pin one bootstrap version and carry an integrity hash`() {
        val cdn = Regex("""(?:href|src)="(https://cdn\.jsdelivr\.net/npm/bootstrap@([^/]+)/[^"]+)"([^>]*)>""")
        val links = html.flatMap { t -> cdn.findAll(t.value).map { t.name to it } }
        assertTrue("no Bootstrap CDN link found", links.isNotEmpty())
        links.forEach { (name, m) ->
            assertEquals("$name must use Bootstrap 5.3.8", "5.3.8", m.groupValues[2])
            assertTrue("$name: ${m.groupValues[1]} needs integrity + crossorigin",
                Regex("""integrity="sha384-[A-Za-z0-9+/=]+"""").containsMatchIn(m.groupValues[3]) && m.groupValues[3].contains("crossorigin="))
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
