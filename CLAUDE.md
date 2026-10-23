# CLAUDE.md

Guidance for AI assistants working in this repo.

## What this is

A Java library + app for tracking personal wealth ("patrimoine"): specify possessions
(Trésorerie/Immobilisation/Obligation), project their evolution over time, reconcile
projections against real events ("Recouper"), and alert on cash-flow impossibilities
("Alerter"). See `doc/FEATURES.md` for the four-pillar model (Spécifier/Projeter/
Recouper/Alerter) and `README.md` for the philosophy.

## Build is heavy — don't run it reflexively

`mvn clean package`/`mvn test` run the Vaadin frontend build (npm/vite) and Spring
Boot repackaging on top of the normal compile+test cycle. This routinely takes
10+ minutes and downloads a large dependency tree (Vaadin BOM alone is substantial).

- CI (`.github/workflows/ci.yml`) runs `mvn clean verify` on `push`/`pull_request` to
  `main` across ubuntu+windows, plus a separate `format` job. Push and check
  `gh pr checks` / `gh run view` rather than reproducing the full build locally.
- For iterating on a single class without the Vaadin/Spring-Boot overhead: generate
  ANTLR sources once (`mvn -o generate-sources`), then compile/run with plain
  `javac`/`java` against a classpath built from `~/.m2/repository` — skips the
  frontend build and packaging entirely. See `wrapper/build.sh` for the one command
  that does need the full `mvn clean package` (building the real exec jar).
- The CI-uploaded `patrimoine-exec-jar` artifact has very short retention — don't
  assume `gh run download` will find one from more than a day or so ago.

## Code style

- Java 21, Lombok throughout (`@Getter`, `@RequiredArgsConstructor`, `@Builder`,
  `@SneakyThrows`, `@Slf4j`) — prefer it over hand-written boilerplate.
- Formatting is `google-java-format`, enforced in CI: the `format` job runs
  `./format.sh && git diff --exit-code`. Run `java -jar
  google-java-format-1.23.0-all-deps.jar --replace <files>` (the jar lives at repo
  root) before pushing, especially after any multi-line edit — CI will reject
  mismatches.
- Domain/model code (`modele`, `cas`, the PatriLang grammar and DSL content) uses
  French names and French user-facing messages/exceptions (`"La variable ... n'existe
  pas"`, `"Objectif final"`, etc.) — match that convention there. Infra/plumbing code
  (`cli`, `compiler`, build scripts) uses English.
- `Dette` enforces `valeurComptable <= 0` and `Creance` enforces `>= 0` at
  construction — a debt can't go positive (overpaid) or a credit negative; model
  overpayments/surplus as a separate flow, not a value that violates the invariant.

## Architecture

- `modele` — core domain: `Patrimoine`, `Personne`, `Argent`, `Devise`,
  `modele/possession` (Compte/Trésorerie/Dette/Creance/Materiel/...),
  `modele/evolution` (EvolutionPatrimoine, FluxJournalier — the day-by-day
  projection and ZFJA/ZFI machinery), `modele/recouppement`, `modele/objectif`.
- `cas` — a `Cas` is one person/scenario's full specification (`Patrimoine.of(...)`
  built lazily); `CasSet`/`ToutCas` aggregate several `Cas` into one combined view.
- `patrilang` — the PatriLang DSL (`.cas.md`/`.tout.md`/`.pj.md` files) letting a
  patrimoine be specified without writing Java. Grammar lives in
  `patrilang/grammar/*.g4`; antlr4-maven-plugin generates the parser into
  `target/generated-sources/antlr` — **never hand-edit generated parser code**,
  edit the `.g4` and regenerate. `PatriLangTranspiler`/`PatriLangParser` turn files
  into `Cas`/`CasSet`; `PatriLangValidator` (lint) forces lazy evaluation to surface
  errors without rendering anything.
- `compiler` — compiles legacy `.java`-based Cas files at runtime (older mechanism,
  superseded by PatriLang for new specs).
- `visualisation/{swing,web,xchart}` — three separate UIs: a Swing desktop viewer
  (`MainIHM`, `VisualiseurCas`, `PatriLangViewer`), a Vaadin/Spring web app
  (`Application`), and `GrapheurEvolutionPatrimoine` (XChart, headless-safe, no
  Swing/AWT display needed — renders straight to PNG).
- `cli` — `PatrimoineCli`: a headless entry point mirroring the UI's operations
  (`lint`/`projeter`/`recouper`/`alerter`/`valeur`) for scripting/CI use, reusing
  the same model/visitor/chart classes as the UIs rather than reimplementing them.
- `google` — Drive/Docs integration for syncing `.cas.md` files and comments.
- `wrapper/` — shell scripts (`init.sh`/`build.sh`/`run.sh`) for running the built
  exec jar against a personal data directory (`PATRIMOINE_HOME`, default
  `~/.patrimoine`) kept outside this repo.

## Testing

- JUnit 5 + Mockito. Unit tests are `*Test.java`, colocated by package under
  `src/test/java`. Integration tests are `*IT.java` (rare — currently just
  `PatriLangTranspilerIT`, which diffs a transpiled `.tout.md` fixture against an
  equivalent hand-built `CasSet`).
- Jacoco enforces 90% line coverage (`mvn verify`), but only on `modele`, `cas`,
  `compiler`, and `cli` — `visualisation/swing`, `visualisation/web`, `google`, and
  `patrilang` are excluded (UI and generated/DSL-plumbing code). New logic in the
  covered packages needs tests to keep CI green.
- A new CLI command or non-trivial branch should leave a runnable check: a small
  `*Test.java` exercising the happy path against real fixture data (see
  `PatrimoineCliTest`) is enough — no need for exhaustive coverage of every flag.
- Beware calling code that may call `System.exit()` directly from a test in the same
  JVM (no subprocess): it silently kills the Surefire fork instead of failing one
  test, and shows up as "VM crash or System.exit called" with no useful stack trace.
  Prefer invoking only success paths that way, and catch/convert exit-worthy errors
  with a clear message rather than letting an unexpected bug reach `System.exit`.

## Git conventions

- Conventional-commit-style subjects: `feat(scope): ...`, `fix(scope): ...`,
  `chore(scope): ...`, `ci: ...`. Scope is often the touched area (`cli`, `PJ`,
  `wrapper`, `ui`).
- Both patterns are in active use: substantive/reviewable changes go through a
  short-lived `feat/x` or `fix/x` branch + PR into `main`; small continuations or
  solo-authored follow-ups are pushed directly to `main`. Match whichever fits the
  size of the change.
- This is a fork (of `MiiJack/patrimoine-STD22079-FP-A`) with GitHub Actions
  disabled by default — if CI never registers a run on a push/PR, check
  `gh api repos/<owner>/<repo>/actions/workflows`; a `total_count: 0` means the
  one-time "enable workflows on this fork" step (web UI only, no API) hasn't been
  done yet, not that the workflow files are broken.
