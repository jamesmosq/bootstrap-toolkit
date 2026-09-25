package com.jamesmosquera.bootstraptoolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the rules in CLAUDE.md. Pure XML checks: they need no IDE and no network.
 */
class LiveTemplatesTest {

    private class Tpl(val name: String, val value: String, val description: String, val contexts: Set<String>)

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
            )
        }
    }

    private val html = load("/liveTemplates/BootstrapToolkitHtml.xml")
    private val jsx = load("/liveTemplates/BootstrapToolkitJsx.xml")

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
    fun `every html template has a jsx counterpart and vice versa`() =
        assertEquals(html.map { it.name }.toSet(), jsx.map { it.name }.toSet())

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
