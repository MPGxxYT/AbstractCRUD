# Storing many objects: CRUDManager

`CRUDManager<T>` keeps a collection of objects of one class, each with its own id and its own file, and holds all of them in memory. Use it for player profiles, arenas, shops, or anything you look up by an id.

## The stored class

The class implements `CRUD.Identifiable`. Its id becomes the file name, so it must not be empty, `.` or `..`, and must not contain `/` or `\`. UUID strings and simple names work well.

```java
public class Arena implements CRUD.Identifiable {
    private String id;
    private int maxPlayers = 8;

    public Arena() {}                          // used by the JSON library
    public Arena(String id) { this.id = id; }  // used by getOrCreate

    @Override public String getId() { return id; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
}
```

## Create it

```java
CRUD<Arena> files = new CRUD<>(Arena.class, getDataFolder().toPath().resolve("arenas"), codec);
CRUDManager<Arena> arenas = new CRUDManager<>(files, Arena::new, registry);
registry.initialize();   // reads arenas/*.json into memory
```

| Argument | Meaning |
| --- | --- |
| `files` | The `CRUD` for this class. Give each manager its **own folder**: `load()` reads every `.json` file in it. |
| `Arena::new` | Creates a new object for an id. It receives the id, so it points at the `Arena(String id)` constructor. The new object's `getId()` must return that same id. |
| `registry` | Optional. Pass a `CRUDRegistry` so `registry.initialize()` loads this manager; pass `null` to skip. |

## Use it

```java
Arena arena = arenas.getOrCreate("spleef");     // existing one, or a new saved one
arena.setMaxPlayers(12);
arenas.save(arena);                             // writes arenas/spleef.json

Optional<Arena> maybe = arenas.get("spleef");   // memory only, never reads a file
for (Arena a : arenas.all()) { ... }            // every arena in memory
arenas.remove("spleef");                        // removes it and deletes the file
```

| Method | What it does |
| --- | --- |
| `get(id)` | The object with this id, from memory. Empty if there is none. Never reads from disk. |
| `getOrCreate(id)` | The object with this id. If there is none, creates one with the factory, keeps it in memory and saves its file. |
| `save(object)` | Adds or replaces the object in memory and writes its file. Returns `false` if the file write failed; memory is updated either way. |
| `remove(id)` | Removes the object from memory and deletes its file. Returns `true` if either existed. |
| `contains(id)` | Whether an object with this id is in memory. |
| `all()` | Every object in memory, as a read-only collection. It updates as objects are added and removed. |
| `load()` | Re-reads every file in the folder, replacing what is in memory. `registry.initialize()` calls this for you. |
| `reload(id)` | Re-reads one file. If the file is missing or unreadable, memory is left unchanged and it returns `false`. |

## Things to know

- **Edits need a save.** Changing an object you got from `get` changes it in memory only. Call `save` to write the file.
- **Everything is in memory.** All files in the folder are read at startup and stay in memory. If the folder grows too large for that, use a `CRUD` on its own and read files when you need them.
- **Threads.** Reading (`get`, `contains`, `all`) never waits and is safe from any thread, including async tasks. Writing methods take a lock, so two threads saving at once don't corrupt the data.
- **Disk work happens on the calling thread.** `save`, `remove` and `getOrCreate` write files right away. For frequent saves on the main thread, consider calling them from an async task.

## Adding your own methods

Extend `CRUDManager` to add lookups for your plugin. `crud()` gives the subclass the underlying `CRUD`.

```java
public class ArenaManager extends CRUDManager<Arena> {
    public ArenaManager(CRUD<Arena> files, CRUDRegistry registry) {
        super(files, Arena::new, registry);
    }

    public List<Arena> withRoomFor(int players) {
        return all().stream().filter(a -> a.getMaxPlayers() >= players).toList();
    }
}
```

## Without a manager

When you don't need everything in memory, use the `CRUD` directly. It takes the id as an argument, and the class doesn't have to implement `Identifiable`:

```java
CRUD<Match> matches = new CRUD<>(Match.class, data.resolve("matches"), codec);
matches.save("2026-11-17-001", match);
Optional<Match> m = matches.get("2026-11-17-001");   // reads the file every time
Map<String, Match> all = matches.loadAll();          // keyed by file name
matches.delete("2026-11-17-001");
```
