# Getting started

This guide adds AbstractCRUD to a Paper plugin and stores two things: a spawn point for the whole server, and a profile for each player. Each step explains what the code does, so you can adapt it to your own data.

You need Java 21 and a Maven plugin project. The examples use a plugin whose main class is `MyPlugin`.

## 1. Add the dependency

Add the JitPack repository and the dependency to `pom.xml`, as shown in the [README](../README.md#install). If you use the Maven Shade plugin, add the relocation from [Shading](shading.md) now so you don't forget it.

## 2. Write the classes you want to save

AbstractCRUD saves plain Java objects. Each saved object becomes one JSON file.

A class with one instance for the whole plugin needs nothing special:

```java
public class ServerData {
    private Location spawn;          // null until an admin sets it

    public ServerData() {}           // needed: used for the default and by the JSON library

    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
}
```

A class with many instances, one per player here, implements `CRUD.Identifiable`. Its id becomes the file name.

```java
public class Profile implements CRUD.Identifiable {
    private String id;
    private int kills;

    public Profile() {}                          // used by the JSON library when reading
    public Profile(String id) { this.id = id; }  // used to create new profiles

    @Override public String getId() { return id; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
}
```

Three rules apply to every class you store:

1. **Give the JSON library a way to build it.** The simplest is a no-argument constructor: the library creates an empty object with it, then fills in the fields from the file. It can be `private` if you don't want your own code to call it. Records and annotated constructors work too; see [Serialization](serialization.md#constructors).
2. **Match the codec's rules for fields.** With Jackson, every field needs a getter and setter (or must be public). With Gson, fields are saved directly. See [Serialization](serialization.md#jackson-or-gson).
3. **Only use types the JSON library understands.** Strings, numbers, booleans, lists, maps and your own simple classes work. Bukkit types like `Location` need a serializer; `ServerData` above needs one for `spawn`. See [Serialization](serialization.md#bukkit-location).

## 3. Create a codec

The codec turns your objects into JSON text and back. Create one and share it between everything you store:

```java
Codec codec = new JacksonCodec();   // or new GsonCodec()
```

Custom serializers, like one for `Location`, are added here. The [serialization guide](serialization.md) shows how.

## 4. Create the storage objects in `onEnable`

```java
public final class MyPlugin extends JavaPlugin {
    private SingleCRUD<ServerData> serverData;
    private CRUDManager<Profile> profiles;

    @Override
    public void onEnable() {
        Codec codec = new JacksonCodec(mapper -> mapper.registerModule(BukkitJson.module()));
        CRUDRegistry registry = new CRUDRegistry();
        Path data = getDataFolder().toPath();

        CRUD<ServerData> serverDataFiles = new CRUD<>(ServerData.class, data, codec);
        serverData = new SingleCRUD<>(serverDataFiles, "server-data", ServerData::new, registry);

        CRUD<Profile> profileFiles = new CRUD<>(Profile.class, data.resolve("profiles"), codec);
        profiles = new CRUDManager<>(profileFiles, Profile::new, registry);

        registry.initialize();
    }

    public SingleCRUD<ServerData> serverData() { return serverData; }
    public CRUDManager<Profile> profiles() { return profiles; }
}
```

What each line does:

| Line | Purpose |
| --- | --- |
| `new CRUD<>(ServerData.class, data, codec)` | Files for `ServerData` go in `plugins/MyPlugin/`. The class tells the codec what to build when reading. |
| `new SingleCRUD<>(..., "server-data", ServerData::new, registry)` | One object stored as `server-data.json`. `ServerData::new` creates it when the file doesn't exist yet. |
| `data.resolve("profiles")` | Profiles get their own folder, `plugins/MyPlugin/profiles/`. Each kind of object needs its own folder, because `loadAll()` reads every `.json` file in it. |
| `new CRUDManager<>(..., Profile::new, registry)` | Keeps every profile in memory. `Profile::new` here is the `Profile(String id)` constructor, used by `getOrCreate`. |
| `registry` argument | Each storage object adds itself to the registry. |
| `registry.initialize()` | Reads every registered file into memory. Call it once, after creating everything. |

Keep the storage objects in **fields**. A local variable in `onEnable` is gone when the method returns, and the rest of your plugin can't reach the data. Commands and listeners get them through the getters, or through their constructors.

## 5. Use the data

```java
// A command that sets the spawn
ServerData data = plugin.serverData().get();
data.setSpawn(player.getLocation());
plugin.serverData().save(data);          // writes server-data.json

// A listener that counts kills
Profile profile = plugin.profiles().getOrCreate(killer.getUniqueId().toString());
profile.setKills(profile.getKills() + 1);
plugin.profiles().save(profile);         // writes profiles/<uuid>.json
```

Changing an object does not write anything. Call `save` after each change you want kept on disk.

## 6. Check the files

Start the server, run the code once, and open `plugins/MyPlugin/`. You should see `server-data.json` and a `profiles` folder. The files are plain JSON, so you can read and edit them while the server is stopped.

If a file is missing or wrong, look in the server console. AbstractCRUD logs every failed read or write under the `AbstractCRUD` logger, with the file path and the cause.

## Next steps

- [Storing one object](single-crud.md) and [storing many objects](crud-manager.md) list everything those classes can do.
- [Serialization](serialization.md) covers `Location`, `ItemStack`, `UUID`, dates and your own serializers.
- [Reference](reference.md) has the full method list and error behaviour.
