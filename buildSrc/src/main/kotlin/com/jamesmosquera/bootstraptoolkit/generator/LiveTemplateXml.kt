package com.jamesmosquera.bootstraptoolkit.generator

/**
 * A live template group in the platform's XML format.
 * https://plugins.jetbrains.com/docs/intellij/providing-live-templates.html
 */
class LiveTemplateGroup(
    val fileName: String,
    val group: String,
    val descriptionSuffix: String,
    val kinds: Set<Kind>,
    val contexts: (Kind) -> List<String>,
    val markup: (TemplateSource) -> String,
)

object LiveTemplateXml {

    /**
     * The two dialects. The same abbreviation lives in both, so they must be separate groups.
     * Pages are whole documents: plain HTML files only (not a Vue <template>, not JSX).
     */
    val GROUPS = listOf(
        LiveTemplateGroup(
            "BootstrapToolkitHtml.xml", "Bootstrap Toolkit (HTML)", "", setOf(Kind.COMPONENT, Kind.PAGE),
            contexts = { if (it == Kind.PAGE) listOf("HTML") else listOf("HTML", "VUE_TEMPLATE") },
        ) { it.body },
        LiveTemplateGroup(
            "BootstrapToolkitJsx.xml", "Bootstrap Toolkit (JSX)", " (JSX/TSX)", setOf(Kind.COMPONENT),
            contexts = { listOf("JSX_HTML", "TSX_HTML") },
        ) { JsxConverter.convert(it.body) },
    )

    fun render(group: LiveTemplateGroup, templates: List<TemplateSource>): String = buildString {
        append("<!-- Generated from src/templates by the generateLiveTemplates task. Do not edit. -->\n")
        append("<templateSet group=\"${escape(group.group)}\">\n")
        templates.filter { it.kind in group.kinds }.forEach { template ->
            val markup = group.markup(template).let { if ("\$END\$" in it) it else it + "\$END\$" }
            append("    <template name=\"${escape(template.name)}\" value=\"${escape(markup)}\"\n")
            append("              description=\"${escape(template.description + group.descriptionSuffix)}\" toReformat=\"false\" toShortenFQNames=\"false\">\n")
            template.variables.forEach {
                append("        <variable name=\"${it.name}\" expression=\"\" defaultValue=\"${escape("\"${it.default}\"")}\" alwaysStopAt=\"true\"/>\n")
            }
            append("        <context>\n")
            group.contexts(template.kind).forEach { append("            <option name=\"$it\" value=\"true\"/>\n") }
            append("        </context>\n")
            append("    </template>\n")
        }
        append("</templateSet>\n")
    }

    /** Attribute escaping; newlines and tabs must be character references or XML parsers turn them into spaces. */
    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        .replace("\n", "&#10;").replace("\t", "&#9;")
}
