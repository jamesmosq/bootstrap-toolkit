# Vendored CDN files (tests only)

Byte-for-byte copies of the files the templates load from jsDelivr, stored at the same path as on
`https://cdn.jsdelivr.net/npm/<path>`. `BootstrapAssetsTest` uses them offline to verify the `integrity`
hashes and that every class used by a template exists. They are not shipped in the plugin.

When bumping a version: download the new files to the new path, update the templates, run the tests,
then delete the old folder.

- `bootstrap@5.3.8` — Bootstrap, (c) 2011-2025 The Bootstrap Authors, MIT License,
  https://github.com/twbs/bootstrap/blob/v5.3.8/LICENSE
- `bootstrap-icons@1.13.1` — Bootstrap Icons, (c) 2019-2024 The Bootstrap Authors, MIT License,
  https://github.com/twbs/icons/blob/v1.13.1/LICENSE
- `bootstrap-docs@5.3.8/validate-forms.js` — the form validation example script of the Bootstrap docs
  (`site/static/docs/[version]/assets/js/validate-forms.js` at tag v5.3.8), (c) The Bootstrap Authors,
  docs licensed CC BY 3.0. Used to verify that `needs-validation` is the hook class the docs script expects.
