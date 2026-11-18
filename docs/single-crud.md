# Storing one object: SingleCRUD

`SingleCRUD<T>` keeps one object for the whole plugin, such as settings or a spawn point, in one JSON file. The class you store does not need an id and does not implement any interface.

## Create it

```java
CRUD<Settings> files = new CRUD<>(Settings.class, getDataFolder().toPath(), codec);
SingleCRUD<Settings> settings = new SingleCRUD<>(files, "settings", Settings::new, registry);
```

| Argument | Meaning |
| --- | --- |
| `files` | The `CRUD` that reads and writes the file. Its folder is where the file goes. |
| `"settings"` | The file name without `.json`. Here: `plugins/MyPlugin/settings.json`. |
| `Settings::new` | Creates the default object when there is no file yet. `Settings::new` needs a no-argument constructor; a lambda such as `() -> new Settings("Welcome")` can call any constructor. |
| `registry` | Optional. Pass a `CRUDRegistry` so `registry.initialize()` loads this object; pass `null` to skip. |

Several `SingleCRUD`s can share one folder and one `CRUD` type as long as their ids differ. Don't put them in a folder that a `CRUDManager` also uses, because the manager reads every `.json` file in its folder.

## Use it

```java
Settings s = settings.get();   // the object in memory
s.motd = "Welcome back";
settings.save(s);              // writes settings.json, returns false if the write failed
```

| Method | What it does |
| --- | --- |
| `get()` | Returns the object. The first call reads the file, or creates the default if there is no file. Later calls return the same object without reading again. |
| `save(value)` | Replaces the object in memory and writes it to the file. Returns `true` if the file was written. |
| `load()` | Reads the file again, replacing the object in memory. Use it after editing the file by hand, for example in a reload command. |
| `delete()` | Deletes the file. The object in memory stays until the next `load()`, which then returns the default. |
| `id()` | The file name without `.json`. |

## When is the default saved?

`get()` creates the default in memory only. The file is written the first time you call `save`. To create the file at startup, save once after loading:

```java
registry.initialize();
settings.save(settings.get());   // writes settings.json if it didn't exist
```

> [!WARNING]
> If the file exists but can't be read (bad JSON, or a `Location` in a world that isn't loaded), `get()` logs the error and returns the **default**. Calling `save` after that overwrites the file with the default. Check the console for `Failed to read` errors before saving in that situation.

## Example: a spawn point

```java
public class ServerData {
    private Location spawn;

    public ServerData() {}

    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
}
```

```java
// onEnable
Codec codec = new JacksonCodec(mapper -> mapper.registerModule(BukkitJson.module()));
serverData = new SingleCRUD<>(
    new CRUD<>(ServerData.class, getDataFolder().toPath(), codec),
    "server-data", ServerData::new, registry);

// /setspawn
ServerData data = serverData.get();
data.setSpawn(player.getLocation());
serverData.save(data);

// on join
Location spawn = serverData.get().getSpawn();
if (spawn != null) {
    player.teleport(spawn);
}
```

`Location` needs a serializer; `BukkitJson.module()` is defined in [Serialization](serialization.md#bukkit-location).
