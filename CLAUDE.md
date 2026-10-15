# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

AbstractCRUD is a Java 21 library (Maven, published via JitPack as `com.github.MPGxxYT:AbstractCRUD`) for file-based JSON persistence, mainly used by Paper/Bukkit plugins. It has no Bukkit dependency itself. Each value is stored as one `<id>.json` file in a directory. Domain terms are defined in `GLOSSARY.md`; use them.

## Commands

```bash
mvn clean install                          # build, test, install to ~/.m2
mvn test -Dtest=CRUDManagerTest            # one test class
mvn test -Dtest=CRUDManagerTest#saveInsertsThenReplaces   # one test method
```

Tests that exercise corrupt files log SEVERE stack traces on purpose. See `TESTING.md` for what each suite covers.

## Branches and releases

- `feature` holds 1.1.0: the 1.x API with every removed member marked `@Deprecated(forRemoval = true)` and a one-time runtime warning (`handlers/DeprecationNotice`). It gets no new features.
- `v2` holds 2.0.0, which removes the 1.x API. `docs/MIGRATION_GUIDE.md` maps every removed 1.x call to its 2.0 replacement; update it when changing public methods.
- JitPack publishes from git tags; tagging and pushing are done by the maintainer.

## Architecture (2.0)

Packages live under `src/main/java/me/mortaldev/`.

- `crudapi.CRUD<T>` is the store for one type in one directory: `get(id)`, `save(id, value)`, `delete(id)`, `loadAll()`. All file handling is here: the `<id>.json` layout, id validation, atomic write via `<id>.json.tmp` and a move, and logging failures instead of throwing (reads return `Optional.empty()`, writes return `false`). Nothing else in the library touches `java.io`/`java.nio.file`.
- `crudapi.codec.Codec` is the seam for JSON conversion, implemented by `JacksonCodec` and `GsonCodec`. Each one builds its mapper once from a constructor callback. Serialization settings belong to the codec; "global" settings mean sharing one codec instance.
- `CRUDManager<T extends CRUD.Identifiable>` is a cache in front of a CRUD, backed by a single `ConcurrentHashMap`. Reads don't lock; writes are `synchronized` and always update both the cache and the CRUD. `load()` swaps contents in without emptying the cache first.
- `SingleCRUD<T>` holds one value under a fixed id in a CRUD, with a default supplier for when no file exists.
- `loading.CRUDRegistry` collects `ILoadable`s; managers and SingleCRUDs register themselves when their constructor receives a non-null registry, and `initialize()` loads them all.

Tests replace the disk with `src/test/.../InMemoryCRUD` (a `CRUD` subclass), so `CRUD`'s public methods must stay overridable.

2.0 must keep reading files written by 1.x. `codec/CodecTest` contains literal 1.x output for both codecs; keep those tests passing.
