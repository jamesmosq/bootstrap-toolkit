// Type-checks every generated JSX/TSX live template with TypeScript and React's types.
// Each template is expanded with its default values (and once per enum option) and compiled
// inside a component, so a template that would not compile in a user's .tsx file fails here.
//
// Usage (after ./gradlew check or build, which generates the XML):
//   npm ci --prefix tools/tsx-check && npm --prefix tools/tsx-check run check
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

const here = dirname(fileURLToPath(import.meta.url));
const xmlPath = join(here, '../../build/generated/liveTemplates/liveTemplates/BootstrapToolkitJsx.xml');

const decode = (s) => s
  .replace(/&#10;/g, '\n').replace(/&#9;/g, '\t').replace(/&quot;/g, '"')
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
const attr = (tag, name) => {
  const m = tag.match(new RegExp(`\\s${name}="([^"]*)"`));
  return m ? decode(m[1]) : '';
};

// The generator writes one <template ...> element per template and one <variable .../> per line.
const xml = readFileSync(xmlPath, 'utf8');
const templates = [...xml.matchAll(/<template\s[\s\S]*?<\/template>/g)].map(([block]) => {
  const head = block.match(/<template\s[^>]*>/)[0];
  const vars = [...block.matchAll(/<variable\s[^>]*\/>/g)].map(([tag]) => {
    const expression = attr(tag, 'expression');
    return {
      name: attr(tag, 'name'),
      default: attr(tag, 'defaultValue').replace(/^"|"$/g, ''),
      options: expression.startsWith('enum(') ? [...expression.matchAll(/"([^"]*)"/g)].map((m) => m[1]) : [],
    };
  });
  return { name: attr(head, 'name'), value: attr(head, 'value'), vars };
});
if (templates.length === 0) throw new Error(`no templates in ${xmlPath}`);

const fill = (t, overrides) => t.vars.reduce(
  (text, v) => text.replaceAll(`$${v.name}$`, overrides[v.name] ?? v.default),
  t.value.replaceAll('$END$', ''),
);

// One component per expansion; remember which lines belong to which template to report errors.
let source = '';
const ranges = [];
templates.forEach((t) => {
  const expansions = [fill(t, {}), ...t.vars.flatMap((v) => v.options.map((o) => fill(t, { [v.name]: o })))];
  expansions.forEach((markup, i) => {
    const start = source.split('\n').length;
    source += `export function ${t.name.replace(/-/g, '_')}_${i}() {\n  return (\n    <div>\n${markup}\n    </div>\n  );\n}\n\n`;
    ranges.push({ name: t.name, start, end: source.split('\n').length });
  });
});

const outDir = join(here, 'build');
mkdirSync(outDir, { recursive: true });
const file = join(outDir, 'templates.tsx');
writeFileSync(file, source);

const program = ts.createProgram([file], {
  strict: true,
  noEmit: true,
  jsx: ts.JsxEmit.ReactJSX,
  types: ['react'],
  typeRoots: [join(here, 'node_modules/@types')],
  skipLibCheck: true,
  target: ts.ScriptTarget.ES2022,
  moduleResolution: ts.ModuleResolutionKind.Bundler,
  module: ts.ModuleKind.ESNext,
});
const diagnostics = ts.getPreEmitDiagnostics(program);
for (const d of diagnostics) {
  const message = ts.flattenDiagnosticMessageText(d.messageText, '\n');
  if (d.file) {
    const { line } = d.file.getLineAndCharacterOfPosition(d.start);
    const owner = ranges.find((r) => line + 1 >= r.start && line + 1 < r.end);
    console.error(`${owner ? owner.name : '?'} (templates.tsx:${line + 1}): ${message}`);
  } else {
    console.error(message);
  }
}
if (diagnostics.length > 0) process.exit(1);
console.log(`TSX check passed: ${templates.length} templates, ${ranges.length} expansions.`);
