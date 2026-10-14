# Testing

Tests use JUnit 5 only. There is no Mockito: modules are tested through their own methods, with temp directories or an in-memory CRUD in place of the disk.

```bash
mvn test                                         # all tests
mvn test -Dtest=CRUDManagerTest                  # one class
mvn test -Dtest=CRUDManagerTest#saveInsertsThenReplaces   # one method
```

Tests that hit corrupt files log SEVERE stack traces on purpose; check the `Tests run` summary, not the log noise.

## What each suite covers

| Suite | Runs against | Covers |
| --- | --- | --- |
| `CRUDTest` | `@TempDir`, parameterized over both codecs | round trips, atomic replace (no `.tmp` left behind), corrupt files, `loadAll` filtering and id parsing, id validation |
| `codec/CodecTest` | strings only | reading files in the exact 1.x Jackson and Gson formats, Gson output identical to 1.x, configure callbacks (Jackson modules, `OptionalTypeAdapter.FACTORY`) |
| `CRUDManagerTest` | `InMemoryCRUD` | every manager method, registry loading, iterating `all()` while another thread writes |
| `SingleCRUDTest` | `InMemoryCRUD`, plus one `@TempDir` round trip | defaults, lazy load, save, delete, registry loading |

## Test helpers

- `TestEntity`: an `Identifiable` with public fields, readable by both Jackson and Gson.
- `InMemoryCRUD<T>`: a `CRUD` subclass that keeps values in a public `stored` map and counts `saves` and `deletes`. Use it wherever the test is about caching, not files.

## Manual checks on a Paper server

Automated tests do not cover these:
- A plugin built against 1.1.0 logs the deprecation warning once, naming its own package, even when it shades and relocates the library.
- Data written by a 1.x build of a plugin loads after upgrading that plugin to 2.0.
- Killing the server during heavy saving never leaves a truncated `.json` file.
