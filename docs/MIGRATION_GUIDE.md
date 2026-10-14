# Migrating from 1.x to 2.0

2.0 removes everything that 1.1.0 marked `forRemoval`. If your plugin compiles against 1.1.0 without removal warnings, it already compiles against 2.0. Otherwise, work through the steps below.

Your saved data does not change: 2.0 reads the same `<directory>/<id>.json` files that 1.x wrote, with either codec.

## 1. Replace the handler with a codec

```java
// 1.x
Jackson jackson = new Jackson();
GSON gson = new GSON();

// 2.0
Codec jackson = new JacksonCodec();
Codec gson = new GsonCodec();
```

Adapters move from `CRUDAdapters` into the codec's constructor callback:

```java
// 1.x
registry.getGlobalAdapters().addModule(new JavaTimeModule());
registry.getGlobalAdapters().addTypeAdapter(Location.class, new LocationAdapter());

// 2.0
Codec jackson = new JacksonCodec(mapper -> mapper.registerModule(new JavaTimeModule()));
Codec gson = new GsonCodec(builder -> builder
    .registerTypeAdapter(Location.class, new LocationAdapter())
    .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY));
```

Global adapters are now a shared codec instance: pass the same codec to every CRUD that needs the same settings. A CRUD that needed its own extra adapters in 1.x (`getCRUDAdapters()`) gets its own codec.

## 2. Replace CRUD subclasses with instances

```java
// 1.x
public class ProfileCRUD extends CRUD<Profile> {
    public ProfileCRUD(Handler handler, CRUDRegistry registry) { super(handler, registry); }
    @Override public Class<Profile> getClazz() { return Profile.class; }
    @Override public String getPath() { return dataFolder + "/profiles"; }
    @Override public CRUDAdapters getCRUDAdapters() { return new CRUDAdapters(); }
}

// 2.0
CRUD<Profile> profileCrud =
    new CRUD<>(Profile.class, getDataFolder().toPath().resolve("profiles"), jackson);
```

Method renames on `CRUD`:

| 1.x | 2.0 |
| --- | --- |
| `getData(id)` | `get(id)` |
| `saveData(entity)` (void) | `save(entity.getId(), entity)` (returns `boolean`) |
| `deleteData(entity)` | `delete(entity.getId())` |
| `getPath()` (`String`) | `directory()` (`Path`) |
| `getClazz()` | `type()` |

Entities keep implementing `CRUD.Identifiable`; no change there.

## 3. Replace CRUDManager subclasses

```java
// 1.x
public class ProfileManager extends CRUDManager<Profile> {
    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry) { super(crud, registry); }
    @Override public Profile getNewInstance(String id) { return new Profile(id); }
    @Override public void log(String message) { logger.info(message); }
}

// 2.0
CRUDManager<Profile> profiles = new CRUDManager<>(profileCrud, Profile::new, registry);
```

You can still subclass `CRUDManager` to add your own lookups; call `super(crud, factory, registry)` and use `crud()` for the underlying store. There is no `log()` to implement; the library logs through `java.util.logging` under the `AbstractCRUD` logger.

| 1.x | 2.0 |
| --- | --- |
| `getByID(id)` | `get(id)` |
| `getByID(entity)` | `get(entity.getId())` |
| `getByID(id, true)` (created but not cached) | `getOrCreate(id)` (cached and saved), or call your factory directly |
| `getByIDCreate(id)` | `getOrCreate(id)` |
| `getByIDCreate(id, false)` | call your factory directly |
| `add(entity)`, `update(entity)` | `save(entity)` |
| `add(entity, false)`, `update(entity, false)` | no direct replacement: cache and disk always change together. Use `save(entity)`. |
| `remove(entity)` | `remove(entity.getId())` |
| `remove(entity, false)` | no direct replacement; use `remove(id)` |
| `contains(entity)` | `contains(entity.getId())` |
| `getSet()` (mutable `HashSet`) | `all()` (read-only `Collection`) |
| `getSet(true)` | `load()` then `all()` |
| `loadByID(id)` | `reload(id)` |
| `getCRUD()` | `crud()` (protected, for subclasses) |

## 4. Replace SingleCRUD subclasses

```java
// 1.x
public class SettingsCRUD extends SingleCRUD<Settings> {
    public SettingsCRUD(Handler handler, CRUDRegistry registry) { super(handler, registry); }
    @Override public String getId() { return "settings"; }
    @Override public String getPath() { return dataFolder.getPath(); }
    @Override public Class<Settings> getClazz() { return Settings.class; }
    @Override public CRUDAdapters getCRUDAdapters() { return new CRUDAdapters(); }
    @Override public Settings construct() { return new Settings(); }
}

// 2.0
CRUD<Settings> settingsCrud = new CRUD<>(Settings.class, getDataFolder().toPath(), gson);
SingleCRUD<Settings> settings = new SingleCRUD<>(settingsCrud, "settings", Settings::new, registry);
```

`get()`, `save(value)`, `delete()` and `load()` work as before; `save` now returns `boolean`. `SingleCRUD` registers with the registry when you pass one, so `registry.initialize()` loads it along with the managers.

## 5. Remove leftovers

Delete any `@AutoRegister` annotations and `implements IRegistrable` clauses.
