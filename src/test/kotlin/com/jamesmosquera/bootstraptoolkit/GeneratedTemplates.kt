package com.jamesmosquera.bootstraptoolkit

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** A live template variable as generated: `expression` is `enum("a","b")`, `date(...)` or empty. */
class TplVariable(val name: String, val default: String, val expression: String) {
    /** The values of an `enum(...)` expression, empty otherwise. */
    val options: List<String> =
        if (expression.startsWith("enum(")) Regex(""""([^"]*)"""").findAll(expression).map { it.groupValues[1] }.toList() else emptyList()
}

/** A live template as generated from src/templates (see buildSrc). */
class Tpl(
    val name: String,
    val value: String,
    val description: String,
    val contexts: Set<String>,
    val vars: List<TplVariable>,
) {
    val variables: List<String> get() = vars.map { "${it.name}=\"${it.default}\"" }

    /** Whole documents (`<!doctype html>`): HTML only, no JSX counterpart. */
    val isPage: Boolean get() = value.trimStart().startsWith("<!doctype", ignoreCase = true)

    /**
     * The markup as it can be inserted: every variable at its default, plus one variant per option of each
     * `enum` variable (the others at their default). `$END$` is removed.
     */
    fun expansions(): List<String> {
        fun fill(overrides: Map<String, String>) =
            vars.fold(value.replace("\$END\$", "")) { text, v -> text.replace("\$${v.name}\$", overrides[v.name] ?: v.default) }
        return listOf(fill(emptyMap())) + vars.flatMap { v -> v.options.map { fill(mapOf(v.name to it)) } }
    }
}

object GeneratedTemplates {
    val html: List<Tpl> by lazy { load("/liveTemplates/BootstrapToolkitHtml.xml") }
    val jsx: List<Tpl> by lazy { load("/liveTemplates/BootstrapToolkitJsx.xml") }

    private fun load(resource: String): List<Tpl> {
        val stream = javaClass.getResourceAsStream(resource) ?: error("missing resource $resource")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        val templates = doc.getElementsByTagName("template")
        return (0 until templates.length).map { i ->
            val e = templates.item(i) as Element
            val options = e.getElementsByTagName("option")
            val variables = e.getElementsByTagName("variable")
            Tpl(
                name = e.getAttribute("name"),
                value = e.getAttribute("value"),
                description = e.getAttribute("description"),
                contexts = (0 until options.length).map { j -> options.item(j) as Element }
                    .filter { it.getAttribute("value") == "true" }
                    .map { it.getAttribute("name") }
                    .toSet(),
                vars = (0 until variables.length).map { j -> variables.item(j) as Element }.map {
                    TplVariable(it.getAttribute("name"), it.getAttribute("defaultValue").removeSurrounding("\""), it.getAttribute("expression"))
                },
            )
        }
    }
}
