# Quick start

The shortest path to saving data in a Paper plugin: four files to copy, then a test. For an explanation of each line, read [Getting started](getting-started.md) afterwards.

The example stores one `Settings` object for the plugin and one `Profile` per player, using Gson.

## 1. Add the dependency

In `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.MPGxxYT</groupId>
        <artifactId>AbstractCRUD</artifactId>
        <version>v2.0.1</version>
    </dependency>
</dependencies>
```

If you build a shaded jar, add the relocation from [Shading](shading.md).

## 2. Create the classes to save

`Settings.java`: one object for the whole plugin.

```java
public class Settings {
    public String motd = "Welcome!";
    public int maxHomes = 3;

    public Settings() {}
}
```

`Profile.java`: one object per player. The id becomes the file name.

```java
import me.mortaldev.abstractcrud.crudapi.CRUD;

public class Profile implements CRUD.Identifiable {
    private String id;
    private int joins;

    public Profile() {}
    public Profile(String id) { this.id = id; }

    @Override public String getId() { return id; }
    public int getJoins() { return joins; }
    public void addJoin() { joins++; }
}
```

## 3. Set up storage in your main class

```java
import java.nio.file.Path;
import me.mortaldev.abstractcrud.crudapi.CRUD;
import me.mortaldev.abstractcrud.crudapi.CRUDManager;
import me.mortaldev.abstractcrud.crudapi.SingleCRUD;
import me.mortaldev.abstractcrud.crudapi.codec.Codec;
import me.mortaldev.abstractcrud.crudapi.codec.GsonCodec;
import me.mortaldev.abstractcrud.crudapi.loading.CRUDRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    private SingleCRUD<Settings> settings;
    private CRUDManager<Profile> profiles;

    @Override
    public void onEnable() {
        Codec codec = new GsonCodec();
        CRUDRegistry registry = new CRUDRegistry();
        Path data = getDataFolder().toPath();

        settings = new SingleCRUD<>(
            new CRUD<>(Settings.class, data, codec), "settings", Settings::new, registry);
        profiles = new CRUDManager<>(
            new CRUD<>(Profile.class, data.resolve("profiles"), codec), Profile::new, registry);

        registry.initialize();
        settings.save(settings.get());   // writes settings.json with defaults on first run

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
    }

    public SingleCRUD<Settings> settings() { return settings; }
    public CRUDManager<Profile> profiles() { return profiles; }
}
```

## 4. Use it

`JoinListener.java`:

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final MyPlugin plugin;

    public JoinListener(MyPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Profile profile = plugin.profiles().getOrCreate(event.getPlayer().getUniqueId().toString());
        profile.addJoin();
        plugin.profiles().save(profile);

        event.getPlayer().sendMessage(plugin.settings().get().motd + " Visit #" + profile.getJoins());
    }
}
```

## 5. Test it

Build the plugin, start the server and join. You should get the message, and these files should exist:

```
plugins/MyPlugin/
├── settings.json
└── profiles/
    └── <your-uuid>.json
```

Stop the server, change `motd` in `settings.json`, start it again and rejoin: the new message shows, and the visit count keeps going up.

## Where to go next

| You want to | Read |
| --- | --- |
| Understand what each line does | [Getting started](getting-started.md) |
| Store a `Location`, `ItemStack` or other Bukkit type | [Serialization](serialization.md) |
| Use Jackson instead of Gson | [Serialization](serialization.md#jackson-or-gson) |
| See every method | [Reference](reference.md) |
