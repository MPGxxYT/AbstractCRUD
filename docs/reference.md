# Reference

Every public class and method in AbstractCRUD 2.0, with the rules that apply to all of them. Packages are under `me.mortaldev.abstractcrud`.

## Classes

| Class | Package | Role |
| --- | --- | --- |
| `CRUD<T>` | `crudapi` | Reads and writes `<folder>/<id>.json` files for one class. |
| `CRUD.Identifiable` | `crudapi` | Interface for objects that know their own id. Required by `CRUDManager`. |
| `CRUDManager<T>` | `crudapi` | Keeps many objects in memory, one file each. |
| `SingleCRUD<T>` | `crudapi` | Keeps one object in memory, one file. |
| `Codec` | `crudapi.codec` | Turns objects into JSON text and back. |
| `JacksonCodec`, `GsonCodec` | `crudapi.codec` | The two `Codec` implementations. |
| `CRUDRegistry` | `crudapi.loading` | Loads every registered manager with one call. |
| `ILoadable` | `crudapi.loading` | Anything with a `load()` method; implemented by `CRUDManager` and `SingleCRUD`. |
| `OptionalTypeAdapter` | `typeadapters` | Gson support for `Optional` fields. |

## CRUD

`new CRUD<>(Class<T> type, Path folder, Codec codec)`

| Method | Returns | Behaviour |
| --- | --- | --- |
| `get(id)` | `Optional<T>` | Reads `<folder>/<id>.json`. Empty if the file is missing, unreadable or invalid. |
| `save(id, value)` | `boolean` | Writes the file, creating the folder if needed. `false` if the write failed. |
| `delete(id)` | `boolean` | Deletes the file. `true` only if a file existed and was deleted. |
| `loadAll()` | `Map<String, T>` | Reads every `.json` file in the folder, keyed by file name. Creates the folder if missing. Unreadable files are skipped. |
| `type()` | `Class<T>` | The class given to the constructor. |
| `directory()` | `Path` | The folder given to the constructor. |

## CRUDManager

`new CRUDManager<>(CRUD<T> crud, Function<String, T> factory, CRUDRegistry registry)`. `T` must implement `CRUD.Identifiable`. `registry` may be `null`.

| Method | Returns | Behaviour |
| --- | --- | --- |
| `get(id)` | `Optional<T>` | From memory only. |
| `getOrCreate(id)` | `T` | From memory, or created with `factory`, kept in memory and saved. Throws `IllegalStateException` if the factory's object has a different id. |
| `save(object)` | `boolean` | Adds or replaces in memory, then writes the file. `false` if the write failed or the object or its id is `null`. |
| `remove(id)` | `boolean` | Removes from memory and deletes the file. `true` if either existed. |
| `contains(id)` | `boolean` | Whether the id is in memory. |
| `all()` | `Collection<T>` | Read-only live view of everything in memory. |
| `load()` | | Replaces memory with every file in the folder. |
| `reload(id)` | `boolean` | Re-reads one file. `false`, and memory unchanged, if it can't be read. |
| `crud()` | `CRUD<T>` | Protected; for subclasses. |

## SingleCRUD

`new SingleCRUD<>(CRUD<T> crud, String id, Supplier<T> defaults, CRUDRegistry registry)`. `registry` may be `null`.

| Method | Returns | Behaviour |
| --- | --- | --- |
| `get()` | `T` | The object in memory. Loads it on the first call. |
| `save(value)` | `boolean` | Replaces the object in memory and writes the file. `false` if the write failed. |
| `load()` | | Reads the file again, or uses `defaults` if it can't be read. |
| `delete()` | `boolean` | Deletes the file. Memory is unchanged until the next `load()`. |
| `id()` | `String` | The file name without `.json`. |

## Codecs

| Constructor | Callback receives |
| --- | --- |
| `new JacksonCodec()` / `new JacksonCodec(Consumer<ObjectMapper>)` | The `ObjectMapper`, before first use |
| `new GsonCodec()` / `new GsonCodec(Consumer<GsonBuilder>)` | The `GsonBuilder`, with pretty printing already on |

| Method | Behaviour |
| --- | --- |
| `encode(object)` | Returns JSON text. Throws `IOException` on failure. |
| `decode(json, type)` | Returns the object. Throws `IOException` on invalid JSON. |

## CRUDRegistry

| Method | Behaviour |
| --- | --- |
| `new CRUDRegistry()` | An empty registry. |
| `register(loadable)` | Adds something to load. Managers and `SingleCRUD`s do this themselves when given the registry. |
| `initialize()` | Calls `load()` on everything registered. |
| `setLoggingEnabled(boolean)` | Logs each registration and the end of `initialize()`. |

## Files

- Each object is one file: `<folder>/<id>.json`, pretty-printed UTF-8 JSON.
- Writes go to `<id>.json.tmp` first, which is then moved over `<id>.json`. If the server stops mid-write, the old file is still intact.
- Only files ending in `.json` are read; other files and subfolders in the folder are ignored.
- Files written by AbstractCRUD 1.x load without changes.

## Ids

An id is used as a file name. It must not be `null` or empty, must not be `.` or `..`, and must not contain `/` or `\`. Calls with an invalid id log a warning and return empty or `false`.

## Errors

AbstractCRUD logs problems instead of throwing them, so one bad file doesn't stop your plugin from enabling. Everything is logged through `java.util.logging` under the logger name `AbstractCRUD`, which Paper prints to the console with the file path and the cause.

| Situation | Result |
| --- | --- |
| File missing | Read returns empty. Not logged. |
| File unreadable or invalid JSON | Read returns empty. Logged as SEVERE with the cause. |
| File contains only `null` | Read returns empty. Logged as a warning. |
| Write fails | `save` returns `false`. Logged as SEVERE. The temporary file is removed. |
| Invalid id | Returns empty or `false`. Logged as a warning. |
| `CRUDManager` factory returns the wrong id | `getOrCreate` throws `IllegalStateException`. |

The exceptions are constructors given `null` for a required argument, which throw `NullPointerException`.

## Threads

- `CRUDManager` reads (`get`, `contains`, `all`) never block and are safe from any thread.
- `CRUDManager` and `SingleCRUD` writes are `synchronized`, so concurrent saves are applied one at a time.
- Disk I/O runs on the thread that calls `save`, `remove`, `getOrCreate`, `load` or `reload`.
- Codecs are safe to share between threads.
