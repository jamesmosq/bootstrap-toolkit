# Third-party notices

Bootstrap Toolkit is licensed under the MIT License (see `LICENSE`). It builds on the following
third-party works.

## Bootstrap documentation (template markup)

The markup of the live templates in `src/templates` is adapted from the examples in the Bootstrap 5.3
documentation (https://getbootstrap.com/docs/5.3/). Changes: examples were shortened, placeholder text
and images replaced (images now come from picsum.photos), ids turned into template variables, and the
JSX/TSX versions are generated automatically.

- Copyright 2011-2025 The Bootstrap Authors
- Documentation licensed under Creative Commons Attribution 3.0 (CC BY 3.0),
  https://creativecommons.org/licenses/by/3.0/

## Bootstrap and Bootstrap Icons (loaded from a CDN, not bundled)

The generated markup links to Bootstrap 5.3.8 and Bootstrap Icons 1.13.1 on jsDelivr. The plugin itself
does not ship them. Byte-exact copies are kept in `src/test/resources/vendor` for tests only.

- Bootstrap — Copyright (c) 2011-2025 The Bootstrap Authors, MIT License,
  https://github.com/twbs/bootstrap/blob/v5.3.8/LICENSE
- Bootstrap Icons — Copyright (c) 2019-2024 The Bootstrap Authors, MIT License,
  https://github.com/twbs/icons/blob/v1.13.1/LICENSE

Bootstrap is a project of the Bootstrap Authors. This plugin is unofficial and not affiliated with or
endorsed by the Bootstrap team.
