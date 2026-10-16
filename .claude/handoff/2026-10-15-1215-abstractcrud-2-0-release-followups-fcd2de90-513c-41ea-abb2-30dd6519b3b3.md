# Handoff: fcd2de90-513c-41ea-abb2-30dd6519b3b3
2026-10-15 12:15 CDT · AbstractCRUD / master

## Unfinished tasks
1. [pending] Publish v2.0.0: the user creates tag `v2.0.0` on master (merge commit `26e2530`) and a GitHub release using the 2.0.0 notes written in the previous session. JitPack and the notes' compare link need the tag. Maintainer does tagging/publishing; confirm before doing it yourself.
2. [pending] Fix stale "Branches and releases" section in CLAUDE.md: it still says `feature` holds 1.1.0 and `v2` holds 2.0.0. Both are merged into master (PRs #3 and #4). Rewrite to: master is 2.x; 1.1.0 is tag `v1.1.0`. Also drop the mention of `handlers/DeprecationNotice` (only exists in 1.1.0).
3. [pending] Optional cleanup: delete merged branches `feature` and `v2` locally and on origin, only if the user asks.
4. [pending] Manual check on a Paper server (not covered by tests): a plugin's 1.x data loads after upgrading it to 2.0.0. Listed as unchecked in PR #4's test plan.

## Context

**Goal.** Architecture review of AbstractCRUD, then a two-step release: 1.1.0 (deprecations only) and 2.0.0 (deepened API with the 1.x API removed).

**Done.**
- 1.1.0: merged via PR MPGxxYT/AbstractCRUD#3 and tagged `v1.1.0`. Adds `@Deprecated(since="1.1.0", forRemoval=true)` everywhere, plus a one-time console warning from the `Jackson`/`GSON` constructors naming the caller's package (StackWalker, works when the library is relocated).
- 2.0.0: merged via PR MPGxxYT/AbstractCRUD#4 (merge `26e2530`), not yet tagged. Commits: Codec seam + concrete `CRUD` store; CRUDManager loads via CRUD; CRUDManager on one ConcurrentHashMap with get/getOrCreate/save/remove/contains/all/load/reload; SingleCRUD built on CRUD; docs; Jackson 2.17.2 -> 2.18.11 (fixes all 11 Dependabot alerts on jackson-databind); README Gson/Jackson dropdowns.
- Docs: README (rewritten with /create-readme), CHANGELOG.md, docs/MIGRATION_GUIDE.md, TESTING.md, GLOSSARY.md, CLAUDE.md.
- Release notes for 1.1.0 and 2.0.0 were given in chat (not saved as files).
- Local master is synced with origin/master at `26e2530`.

**In progress / next.** Nothing is mid-edit. Start with task 2 (CLAUDE.md fix, a small commit). master is protected: direct pushes are rejected, so changes need a branch + PR.

**Key files & paths.** `src/main/java/me/mortaldev/crudapi/{CRUD,CRUDManager,SingleCRUD}.java`, `crudapi/codec/{Codec,JacksonCodec,GsonCodec}.java`, `crudapi/loading/CRUDRegistry.java`, `src/test/java/me/mortaldev/crudapi/{CRUDTest,CRUDManagerTest,SingleCRUDTest,InMemoryCRUD,TestEntity}.java`, `codec/CodecTest.java`, `CLAUDE.md`, `docs/MIGRATION_GUIDE.md`.

**Decisions & gotchas.**
- `CRUD<T>` takes the id explicitly (`save(id, value)`, `loadAll()` returns `Map<id, T>`) because SingleCRUD values do not implement `Identifiable`. CRUDManager takes whole entities.
- Failures are logged, not thrown: reads return `Optional.empty()`, writes return `false`.
- 2.0 must keep reading 1.x files; `CodecTest` holds literal 1.x output.
- `InMemoryCRUD` subclasses `CRUD`, so CRUD's public methods must stay non-final.
- With JacksonCodec, private fields without getters/setters are silently not saved (README warns about this).
- The `origin` master branch has a repository rule blocking direct pushes; use PRs (as done for #3, #4).
- `.idea/workspace.xml` shows as modified locally; it is IDE state, leave it alone.
- User writing rules: `~/.claude/claudisms.md` applies (no em dashes, no emojis, no hype words) to docs and release notes.

**How to verify.** `mvn test` (47 tests; SEVERE stack traces from corrupt-file tests are expected). `mvn clean install` for a full build.
