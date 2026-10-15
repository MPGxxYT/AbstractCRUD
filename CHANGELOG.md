# AbstractCRUD Changelog

## [2.0.0]

A breaking release. The 1.x API was deprecated in 1.1.0 and is removed here. See [docs/MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md) for step-by-step upgrade instructions. Files saved by 1.x load unchanged.

### Added
- `Codec` interface with `JacksonCodec` and `GsonCodec`. Each is configured once through a constructor callback (`mapper -> ...` or `builder -> ...`) and builds its mapper a single time.
- `CRUD` is a concrete class: `new CRUD<>(Type.class, path, codec)` with `get(id)`, `save(id, value)`, `delete(id)` and `loadAll()`. It works for any type, not only `Identifiable` ones.
- `CRUDManager` is concrete: `new CRUDManager<>(crud, factory, registry)` with `get`, `getOrCreate`, `save`, `remove`, `contains`, `all`, `load` and `reload`.
- `SingleCRUD` is concrete: `new SingleCRUD<>(crud, id, defaults, registry)`, and registers itself with the registry.
- JUnit 5 test suite.

### Changed
- `CRUD.save` returns `boolean`; a failed write no longer passes silently.
- `CRUD.loadAll()` logs and skips corrupt files instead of stopping.
- `CRUDManager` keeps one `ConcurrentHashMap`. Reads never block, iterating `all()` while other threads write no longer throws, and `load()` never leaves the cache empty partway through.
- `CRUDManager.all()` returns a read-only view; 1.x `getSet()` returned the internal set.
- Directories are `java.nio.file.Path` instead of `String`.
- Ids are validated: empty ids, `.`/`..`, and ids containing `/` or `\` are rejected.
- An id containing `.json` (for example `a.json.b`) now loads under the right key.
- Jackson (`jackson-databind`, `jackson-datatype-jsr310`) upgraded from 2.17.2 to 2.18.11, fixing the open Dependabot security alerts against `jackson-databind`.
- Atomic writes fall back to a plain replace on file systems that do not support atomic moves.

### Removed
- `Handler`, `Jackson`, `GSON`, `Get`, `Save`, `Delete` and the `operations` package. Use a `Codec`.
- `CRUDAdapters` and `CRUDRegistry.getGlobalAdapters()`. Configure the codec, and share one codec across CRUDs for global settings.
- Abstract `CRUD` methods `getClazz()`, `getPath()` and `getCRUDAdapters()`, and `getData`/`saveData`/`deleteData`.
- `CRUDManager.log()`, `getNewInstance()`, `getCRUD()`, `getByID`, `getByIDCreate`, `add`, `update`, `getSet`, `loadByID` and the `Boolean` save/delete flags.
- `SingleCRUD.construct()`, `getPath()`, `getClazz()`, `getCRUDAdapters()`. `SingleCRUD` no longer implements `Identifiable`.
- `IRegistrable`, `AutoRegister` and the unused `org.reflections` dependency.

## [1.1.0]

A deprecation release with no new features.
- Every type and member that 2.0.0 changes or removes is marked `@Deprecated(since = "1.1.0", forRemoval = true)`.
- The `Jackson` and `GSON` constructors log a one-time console warning naming the calling plugin's package, so server owners can report it to the plugin developer.

## [1.0.2]
- Fixed `SingleCRUD.get()` not caching results (now auto-loads).
- Fixed `CRUDManager.getByIDCreate()` ignoring the `add` parameter.
- Fixed `OptionalTypeAdapter` type erasure and field order issues.
- `GsonGet`/`GsonSave` reuse handler instances.
- Added a null check for the path in `NormalDelete`.
- Removed the unused `globalInstance` field from the GSON handler.

## [1.0.1]
- Minor updates.

## [1.0.0]
- Initial stable release: Jackson and GSON support, constructor injection, thread-safe managers, atomic writes, `CRUDAdapters`.
