package com.jamesmosquera.bootstraptoolkit.generator

/** A live template variable. Declaration order is the Tab order in the editor. */
data class Variable(val name: String, val default: String)

/**
 * One source template: an HTML file under src/templates whose name is the abbreviation
 * (bs5-btn.html -> bs5-btn) and which starts with a header comment:
 *
 * ```
 * <!--
 * description: Bootstrap 5 button
 * var VARIANT: primary
 * var TEXT: Button
 * -->
 * <button type="button" class="btn btn-$VARIANT$">$TEXT$</button>
 * ```
 *
 * The body is plain Bootstrap 5.3 HTML; the JSX version is derived from it by [JsxConverter].
 */
data class TemplateSource(
    val name: String,
    val description: String,
    val variables: List<Variable>,
    val body: String,
) {
    companion object {
        private val NAME = Regex("""bs5-[a-z0-9]+(-[a-z0-9]+)*""")
        private val VARIABLE_NAME = Regex("""[A-Z][A-Z0-9_]*""")
        private val VARIABLE_USE = Regex("""\$([A-Za-z_][A-Za-z0-9_]*)\$""")
        private val HEADER = Regex("""\A\s*<!--(.*?)-->""", RegexOption.DOT_MATCHES_ALL)
        private val PREDEFINED = setOf("END", "SELECTION")

        fun parse(name: String, text: String): TemplateSource {
            require(NAME.matches(name)) { "file name must be the abbreviation, like bs5-card-image (got '$name')" }
            val source = text.replace("\r\n", "\n")
            val header = HEADER.find(source) ?: error("must start with a <!-- ... --> header")

            var description = ""
            val variables = mutableListOf<Variable>()
            header.groupValues[1].lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                require(':' in line) { "header line must be 'key: value' (got '$line')" }
                val key = line.substringBefore(':').trim()
                val value = line.substringAfter(':').trim()
                when {
                    key == "description" -> description = value
                    key.startsWith("var ") -> variables += Variable(key.removePrefix("var ").trim(), value)
                    else -> error("unknown header key '$key' (expected 'description' or 'var NAME')")
                }
            }

            val body = source.substring(header.range.last + 1)
                .lines().dropWhile { it.isBlank() }.joinToString("\n").trimEnd()

            require(description.isNotBlank()) { "header needs a 'description'" }
            require(body.isNotBlank()) { "template body is empty" }
            variables.forEach {
                require(VARIABLE_NAME.matches(it.name)) { "variable '${it.name}' must be UPPER_SNAKE_CASE" }
                require(it.name !in PREDEFINED) { "\$${it.name}\$ is predefined, do not declare it" }
                require('"' !in it.default && '\\' !in it.default) { "default of ${it.name} must not contain \" or \\" }
            }
            val declared = variables.map { it.name }
            require(declared.size == declared.toSet().size) { "duplicate variable declaration" }
            val used = VARIABLE_USE.findAll(body).map { it.groupValues[1] }.toSet() - PREDEFINED
            (used - declared.toSet()).let { require(it.isEmpty()) { "used but not declared: $it" } }
            (declared.toSet() - used).let { require(it.isEmpty()) { "declared but not used: $it" } }

            return TemplateSource(name, description, variables, body)
        }
    }
}
