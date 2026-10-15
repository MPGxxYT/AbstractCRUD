<div align="center">

# AbstractCRUD

[![JitPack](https://jitpack.io/v/MPGxxYT/AbstractCRUD.svg)](https://jitpack.io/#MPGxxYT/AbstractCRUD)
![Java 21](https://img.shields.io/badge/Java-21-blue)

Store Java objects as JSON files, one file per object, with an in-memory cache in front.

[Installation](#installation) | [Usage](#usage) | [Configuring JSON](#configuring-json) | [Migrating from 1.x](docs/MIGRATION_GUIDE.md)

</div>

AbstractCRUD is a small persistence library for Paper and Bukkit plugins, and for any Java 21 application that wants file-based storage without a database. You give it a class, a directory and a JSON codec (Jackson or Gson), and it reads and writes `<directory>/<id>.json`.

- **One file per object.** Files are plain pretty-printed JSON that you can open and edit by hand.
- **Atomic writes.** Each save goes to a temporary file first and is then moved into place, so a crash mid-save never leaves a half-written file.
- **Jackson or Gson.** Pick a codec and configure it once; the rest of the code is the same either way.
- **Thread-safe cache.** `CRUDManager` keeps entities in a `ConcurrentHashMap`; reads never block and iterating while other threads write never throws.
- **Failures are logged.** A missing or corrupt file reads as empty and a failed write returns `false`, so one bad file does not stop a plugin from enabling.

> [!IMPORTANT]
> 2.0.0 removes the 1.x API. If your plugin uses `Jackson`, `GSON`, `CRUDAdapters` or subclasses `CRUD`, follow the [migration guide](docs/MIGRATION_GUIDE.md). Files saved by 1.x load in 2.0 unchanged.

## Installation

Requires Java 21. The library is published through [JitPack](https://jitpack.io/#MPGxxYT/AbstractCRUD).

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.MPGxxYT</groupId>
    <artifactId>AbstractCRUD</artifactId>
    <version>v2.0.0</version>
</dependency>
```

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.MPGxxYT:AbstractCRUD:v2.0.0'
}
```

> [!TIP]
> If more than one plugin on a server shades AbstractCRUD, relocate it (for example with the Maven Shade plugin's `<relocation>`) so the plugins don't load each other's copy.

## Usage

### Store objects with a manager

Entities implement `CRUD.Identifiable`. The id becomes the file name. Create a codec, a `CRUD` for the directory, and a `CRUDManager` on top. Passing a `CRUDRegistry` lets you load every manager with one call.

The two codecs read your class differently: Gson reads and writes fields directly, while Jackson only sees public fields and getter/setter pairs. Pick the example for the codec you use.

<details open>
<summary><b>Gson</b></summary>

```java
public class Profile implements CRUD.Identifiable {
    private String id;
    private String name = "Unknown";
    private int level = 1;

    public Profile() {}                      // used by Gson when reading
    public Profile(String id) { this.id = id; }

    @Override
    public String getId() { return id; }
}
```

```java
@Override
public void onEnable() {
    Codec codec = new GsonCodec();
    CRUDRegistry registry = new CRUDRegistry();

    CRUD<Profile> profileCrud =
        new CRUD<>(Profile.class, getDataFolder().toPath().resolve("profiles"), codec);
    profiles = new CRUDManager<>(profileCrud, Profile::new, registry);

    registry.initialize(); // reads plugins/YourPlugin/profiles/*.json into the cache
}
```

</details>

<details>
<summary><b>Jackson</b></summary>

```java
public class Profile implements CRUD.Identifiable {
    private String id;
    private String name = "Unknown";
    private int level = 1;

    public Profile() {}                      // used by Jackson when reading
    public Profile(String id) { this.id = id; }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
```

```java
@Override
public void onEnable() {
    Codec codec = new JacksonCodec();
    CRUDRegistry registry = new CRUDRegistry();

    CRUD<Profile> profileCrud =
        new CRUD<>(Profile.class, getDataFolder().toPath().resolve("profiles"), codec);
    profiles = new CRUDManager<>(profileCrud, Profile::new, registry);

    registry.initialize(); // reads plugins/YourPlugin/profiles/*.json into the cache
}
```

> [!WARNING]
> Without the getters and setters, Jackson saves only `id` (through `getId()`) and silently drops the private fields. Annotating the fields with `@JsonProperty` also works.

</details>

Then work with the cache. Every write updates the cache and the file together.

```java
Profile profile = profiles.getOrCreate(player.getUniqueId().toString()); // creates and saves if new
profiles.get(id);          // Optional<Profile>, from the cache only
profiles.save(profile);    // insert or replace; returns false if the file write failed
profiles.remove(id);       // removes from the cache and deletes the file
profiles.contains(id);
profiles.all();            // read-only view of every cached profile
profiles.reload(id);       // re-read one file into the cache
```

To add lookups of your own, extend `CRUDManager` and use `crud()` to reach the store.

### Store a single object

`SingleCRUD` holds one value, such as plugin settings, under a fixed id. The value type does not need to implement `Identifiable`.

```java
CRUD<Settings> settingsCrud = new CRUD<>(Settings.class, getDataFolder().toPath(), codec);
SingleCRUD<Settings> settings =
    new SingleCRUD<>(settingsCrud, "settings", Settings::new, registry);

Settings current = settings.get();   // loads settings.json, or Settings::new if there is none
current.motd = "Welcome back";
settings.save(current);
```

### Use the store directly

`CRUD` can be used without a cache. It works for any type and takes the id explicitly.

```java
CRUD<Arena> arenas = new CRUD<>(Arena.class, dataDir.resolve("arenas"), codec);

arenas.save("spleef", arena);              // writes arenas/spleef.json
Optional<Arena> loaded = arenas.get("spleef");
Map<String, Arena> all = arenas.loadAll(); // keyed by file name; corrupt files are skipped
arenas.delete("spleef");
```

> [!NOTE]
> Ids must be non-empty and must not be `.` or `..` or contain `/` or `\`. Invalid ids are logged and the call reads as empty or returns `false`.

## Configuring JSON

Each codec builds its mapper once. Register modules and adapters in the constructor callback:

```java
Codec jackson = new JacksonCodec(mapper -> mapper.registerModule(new JavaTimeModule()));

Codec gson = new GsonCodec(builder -> builder
    .registerTypeAdapter(Location.class, new LocationAdapter())
    .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
```

Pass the same codec instance to every `CRUD` that needs the same settings. A `CRUD` that needs extra adapters gets its own codec.

`OptionalTypeAdapter.FACTORY` (in `me.mortaldev.typeadapters`) lets Gson read and write `Optional` fields: an empty `Optional` is written as `null`, a present one as its value. Jackson's `jackson-datatype-jsr310` module is included as a dependency for `java.time` types.

## Logging

The library logs through `java.util.logging` under the `AbstractCRUD` and `CRUDRegistry` loggers, which Paper prints to the server console. Call `registry.setLoggingEnabled(true)` for verbose registration messages.

## Building from source

```bash
git clone https://github.com/MPGxxYT/AbstractCRUD.git
cd AbstractCRUD
mvn clean install
```

This runs the JUnit 5 test suite and installs `me.mortaldev:AbstractCRUD:2.0.0` to your local Maven repository. See [TESTING.md](TESTING.md) for running single tests and for what each suite covers.
