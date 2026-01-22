# AbstractCRUD

A lightweight, flexible Java library for simple CRUD (Create, Read, Update, Delete) operations with JSON persistence. Designed for Minecraft plugins but suitable for any Java application requiring file-based data storage.

## Features

- **Dual JSON Support**: Choose between Jackson or GSON for serialization
- **Dependency Injection**: Modern DI pattern for testability and flexibility (no singletons required)
- **Thread-Safe**: Concurrent operations with synchronized mutations and O(1) ID lookups
- **Atomic Writes**: Prevents data corruption with temporary file + atomic move pattern
- **Custom Serialization**: Extensible adapter system for complex types (Jackson modules, GSON type adapters)
- **Simple Registration**: Managers auto-register via constructor injection
- **Two Manager Types**:
  - `CRUDManager<T>`: Manages collections of entities (e.g., profiles, maps)
  - `SingleCRUD<T>`: Manages single configuration objects (e.g., plugin settings)
- **Lazy Loading**: Optional on-demand data loading with in-memory caching
- **Type-Safe**: Strongly-typed entities with compile-time checking

## Requirements

- **Java 21+**
- **Maven** (for building)
- **Dependencies** (automatically managed):
  - Jackson Databind 2.17.2
  - Jackson JSR310 (Java 8 Date/Time) 2.17.2
  - GSON 2.11.0
  - Reflections 0.10.2

## Installation

### Maven

```xml
<dependency>
    <groupId>me.mortaldev</groupId>
    <artifactId>AbstractCRUD</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Local Build

```bash
git clone <repository-url>
cd AbstractCRUD
mvn clean install
```

## Quick Start

### 1. Define Your Entity

Entities must implement `CRUD.Identifiable` to provide a unique ID:

```java
public class Profile implements CRUD.Identifiable {
    private final String id;
    private String name;
    private int level;

    public Profile(String id) {
        this.id = id;
        this.name = "Unknown";
        this.level = 1;
    }

    @Override
    public String getId() {
        return id;
    }

    // Getters and setters...
}
```

### 2. Create a CRUD Implementation

```java
public class ProfileCRUD extends CRUD<Profile> {
    private final String path;

    public ProfileCRUD(String dataPath, Handler handler) {
        super(handler);
        this.path = dataPath;
    }

    @Override
    public Class<Profile> getClazz() {
        return Profile.class;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public CRUDAdapters getCRUDAdapters() {
        return new CRUDAdapters(); // Add custom adapters if needed
    }
}
```

### 3. Create a Manager

```java
public class ProfileManager extends CRUDManager<Profile> {
    private final Logger logger;

    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry, Logger logger) {
        super(crud, registry); // Auto-registers with registry
        this.logger = logger;
    }

    @Override
    public Profile getNewInstance(String id) {
        return new Profile(id);
    }

    @Override
    public void log(String message) {
        logger.info("[ProfileManager] " + message);
    }
}
```

### 4. Initialize in Your Plugin

```java
public class MyPlugin extends JavaPlugin {
    private CRUDRegistry registry;
    private ProfileManager profileManager;

    @Override
    public void onEnable() {
        // 1. Create registry and handler
        registry = new CRUDRegistry();
        registry.setLoggingEnabled(true); // Optional: verbose logging
        Jackson jackson = new Jackson();

        // 2. Configure global adapters (applies to all CRUD managers)
        registry.getGlobalAdapters()
            .addModule(new JavaTimeModule()); // Jackson: Java 8 Date/Time support

        // 3. Create CRUD instance
        String dataPath = getDataFolder() + "/profiles";
        ProfileCRUD crud = new ProfileCRUD(dataPath, jackson);
        crud.setRegistry(registry);

        // 4. Create manager (auto-registers via constructor)
        profileManager = new ProfileManager(crud, registry, getLogger());

        // 5. Initialize all registered managers
        registry.initialize();

        getLogger().info("CRUD system initialized!");
    }
}
```

### 5. Use the Manager

```java
// Access via your plugin instance or DI
ProfileManager manager = plugin.getProfileManager();

// Create and add a profile
Profile profile = new Profile("player123");
profile.setName("Steve");
profile.setLevel(10);
manager.add(profile); // Automatically saves to file

// Retrieve by ID
Optional<Profile> loaded = manager.getByID("player123");
loaded.ifPresent(p -> System.out.println("Name: " + p.getName()));

// Update
profile.setLevel(11);
manager.update(profile); // Saves changes

// Remove
manager.remove(profile); // Deletes from memory and file
```

## Core Components

### CRUD<T>

Abstract base class for data persistence operations.

**Key Methods:**
- `getData(String id)` - Load entity from file
- `saveData(T object)` - Save entity to file (atomic write)
- `deleteData(T object)` - Delete entity file

**Requires Override:**
- `getClazz()` - Return entity class
- `getPath()` - Return storage directory path
- `getCRUDAdapters()` - Return custom serialization adapters

### CRUDManager<T>

Manages collections of entities with in-memory caching and file persistence.

**Features:**
- Thread-safe operations (synchronized mutations)
- O(1) ID lookups via concurrent cache
- Lazy loading support
- Bulk operations (getSet, load, etc.)

**Key Methods:**
- `add(T data)` - Add and save entity
- `remove(T data)` - Remove and delete entity
- `update(T data)` - Update and save entity
- `getByID(String id)` - Retrieve by ID (cached)
- `getByIDCreate(String id)` - Get or create new
- `load()` - Load all entities from directory
- `loadByID(String id)` - Load/reload single entity

### SingleCRUD<T>

Manages a single configuration object (e.g., plugin settings).

**Features:**
- Simplified API for singleton data
- Lazy construction via `construct()` method
- Automatic file creation on first save

**Key Methods:**
- `get()` - Get the object (constructs if null)
- `save(T object)` - Replace and save object
- `load()` - Load from file or construct default
- `delete()` - Delete the file

**Example:**

```java
public class ConfigCRUD extends SingleCRUD<PluginConfig> {
    public ConfigCRUD(Handler handler) {
        super(handler);
    }

    @Override
    public PluginConfig construct() {
        return new PluginConfig(); // Default config
    }

    @Override
    public String getId() {
        return "config";
    }

    @Override
    public String getPath() {
        return "plugins/MyPlugin/config";
    }

    @Override
    public Class<PluginConfig> getClazz() {
        return PluginConfig.class;
    }

    @Override
    public CRUDAdapters getCRUDAdapters() {
        return new CRUDAdapters();
    }
}
```

## Dependency Injection Pattern (Recommended)

### Why Dependency Injection?

The modern DI approach offers:
- **Testability**: Mock dependencies in unit tests
- **Flexibility**: Swap implementations (Jackson ↔ GSON)
- **Explicit Dependencies**: No hidden singleton coupling
- **Thread Safety**: Avoid global state

### Setup Pattern

```java
public class MyPlugin extends JavaPlugin {
    private CRUDRegistry registry;
    private ProfileManager profileManager;
    private MapManager mapManager;

    @Override
    public void onEnable() {
        // 1. Create shared dependencies
        registry = new CRUDRegistry();
        Jackson jackson = new Jackson();
        String dataFolder = getDataFolder().getPath();

        // 2. Register global adapters (before creating managers)
        registry.getGlobalAdapters()
            .addModule(new JavaTimeModule());

        // 3. Create CRUD instances
        ProfileCRUD profileCRUD = new ProfileCRUD(dataFolder + "/profiles", jackson);
        profileCRUD.setRegistry(registry);

        MapCRUD mapCRUD = new MapCRUD(dataFolder + "/maps", jackson);
        mapCRUD.setRegistry(registry);

        // 4. Inject into managers (auto-registers)
        profileManager = new ProfileManager(profileCRUD, registry, getLogger());
        mapManager = new MapManager(mapCRUD, registry, getLogger());

        // 5. Initialize all managers
        registry.initialize();
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
```

## Custom Serialization

### Jackson Modules (Java 8 Date/Time, etc.)

```java
registry.getGlobalAdapters()
    .addModule(new JavaTimeModule()); // Instant, LocalDateTime, etc.
```

### Jackson Custom Serializers

```java
// Custom serializer for Bukkit Location
public class LocationSerializer extends JsonSerializer<Location> {
    @Override
    public void serialize(Location loc, JsonGenerator gen, SerializerProvider prov)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("world", loc.getWorld().getName());
        gen.writeNumberField("x", loc.getX());
        gen.writeNumberField("y", loc.getY());
        gen.writeNumberField("z", loc.getZ());
        gen.writeEndObject();
    }
}

// Register globally (applies to all managers)
registry.getGlobalAdapters()
    .addSerializer(Location.class, new LocationSerializer())
    .addDeserializer(Location.class, new LocationDeserializer());

// Or per-CRUD instance
@Override
public CRUDAdapters getCRUDAdapters() {
    return new CRUDAdapters()
        .addSerializer(Location.class, new LocationSerializer())
        .addDeserializer(Location.class, new LocationDeserializer());
}
```

### GSON Type Adapters

```java
// GSON adapter for UUID
public class UUIDTypeAdapter extends TypeAdapter<UUID> {
    @Override
    public void write(JsonWriter out, UUID value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public UUID read(JsonReader in) throws IOException {
        return UUID.fromString(in.nextString());
    }
}

// Register
registry.getGlobalAdapters()
    .addTypeAdapter(UUID.class, new UUIDTypeAdapter());
```

### Choosing Jackson vs GSON

```java
// Jackson (recommended for complex objects)
Jackson jackson = new Jackson();
ProfileCRUD crud = new ProfileCRUD(path, jackson);

// GSON (simpler API, good for basic objects)
GSON gson = new GSON();
ProfileCRUD crud = new ProfileCRUD(path, gson);
```

## Registration System

Managers automatically register with the `CRUDRegistry` when constructed using the DI constructor:

```java
public class ProfileManager extends CRUDManager<Profile> {
    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry, Logger logger) {
        super(crud, registry); // This line registers the manager
        this.logger = logger;
    }
}
```

When you call `new ProfileManager(crud, registry, logger)`, the manager is added to the registry's internal list. Then `registry.initialize()` loads all registered managers' data.

### Legacy @AutoRegister Annotation

The `@AutoRegister` annotation and `scanAndRegister()` method exist for backward compatibility but are **not recommended** for new code:

- **Complexity**: Requires reflection, classpath scanning, and getInstance() methods
- **Hidden Dependencies**: Makes initialization order unclear
- **Harder to Test**: Tight coupling to singleton pattern
- **Not Used**: Modern codebases (including reference implementations) use manual DI

**Recommended**: Use explicit instantiation with constructor injection as shown in the Quick Start guide.

## Advanced Features

### Thread-Safe Operations

All `CRUDManager` mutations are synchronized:

```java
// Safe for concurrent access
CompletableFuture.runAsync(() -> manager.add(profile1));
CompletableFuture.runAsync(() -> manager.add(profile2));
```

### Lazy Loading

```java
// Don't load until needed
HashSet<Profile> profiles = manager.getSet(true); // Loads if empty
```

### Partial Reloading

```java
// Reload single entity from disk
manager.loadByID("player123");
```

### Create-If-Not-Exists

```java
// Returns existing or creates new (without adding to set)
Profile profile = manager.getByIDCreate("player123", false);

// Creates and adds to set + saves to file
Profile profile = manager.getByIDCreate("player123", true);
```

### Atomic Writes

Both Jackson and GSON handlers use atomic write operations:

1. Write to `<filename>.tmp`
2. Flush and close
3. Atomically move to `<filename>.json`
4. Cleanup on error

This prevents corruption if the process crashes mid-write.

### Custom Logging

```java
@Override
public void log(String message) {
    myLogger.info("[ProfileManager] " + message);
}

// Or integrate with plugin logger
@Override
public void log(String message) {
    plugin.getLogger().info(message);
}
```

## Migration Guide

### From Singleton Pattern (Deprecated)

**Old Way:**

```java
public class ProfileManager extends CRUDManager<Profile> {
    private static ProfileManager instance;

    private ProfileManager() {
        CRUDRegistry.getInstance().register(this);
    }

    public static ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    @Override
    public CRUD<Profile> getCRUD() {
        return ProfileCRUD.getInstance(); // Hidden dependency!
    }
}
```

**New Way:**

```java
public class ProfileManager extends CRUDManager<Profile> {
    private static ProfileManager instance;

    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry) {
        super(crud, registry); // Explicit dependencies
    }

    public static ProfileManager getInstance() {
        return instance;
    }

    public static void setInstance(ProfileManager manager) {
        instance = manager;
    }

    // No need to override getCRUD()
}
```

**Benefits:**
- ✅ Testable (inject mock CRUD)
- ✅ Flexible (switch Jackson ↔ GSON)
- ✅ Explicit (no hidden dependencies)
- ✅ Thread-safe (no lazy initialization races)

## Best Practices

### 1. Use Dependency Injection

Inject `CRUD`, `CRUDRegistry`, and `Handler` instances instead of using singletons.

### 2. Register Global Adapters Once

Configure common serializers (JavaTimeModule, UUID, etc.) on `CRUDRegistry.getGlobalAdapters()` to apply to all managers.

### 3. Use Atomic Operations

Always use `manager.add()`, `update()`, `remove()` instead of manually calling `getCRUD().saveData()` to maintain cache consistency.

### 4. Handle Optional Returns

Many methods return `Optional<T>`:

```java
manager.getByID(id).ifPresent(profile -> {
    // Do something
});

// Or with default
Profile profile = manager.getByID(id).orElse(defaultProfile);
```

### 5. Validate IDs

Always check for null/empty IDs before operations:

```java
if (id == null || id.isEmpty()) {
    return; // Manager will log warning
}
```

### 6. Enable Logging During Development

```java
registry.setLoggingEnabled(true);
```

### 7. Separate Data Paths

Use different directories for different entity types:

```java
ProfileCRUD profileCRUD = new ProfileCRUD(dataFolder + "/profiles", jackson);
MapCRUD mapCRUD = new MapCRUD(dataFolder + "/maps", jackson);
```

### 8. Use SingleCRUD for Singletons

Don't use `CRUDManager` for configuration files or single objects:

```java
// ✅ Correct
public class ConfigCRUD extends SingleCRUD<PluginConfig> { }

// ❌ Wrong
public class ConfigManager extends CRUDManager<PluginConfig> { }
```

## File Format

Entities are stored as pretty-printed JSON files:

```
plugins/MyPlugin/
├── profiles/
│   ├── player123.json
│   ├── player456.json
│   └── player789.json
└── config/
    └── config.json
```

**Example `player123.json`:**

```json
{
  "id": "player123",
  "name": "Steve",
  "level": 10,
  "joinDate": "2025-01-15T10:30:00Z"
}
```

## Common Patterns

### Pattern 1: Service Locator

```java
public class DataServices {
    private final ProfileManager profileManager;
    private final MapManager mapManager;

    public DataServices(ProfileManager profileManager, MapManager mapManager) {
        this.profileManager = profileManager;
        this.mapManager = mapManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
```

### Pattern 2: Factory for CRUD Creation

```java
public class CRUDFactory {
    private final Handler handler;
    private final String baseDataPath;

    public CRUDFactory(Handler handler, String baseDataPath) {
        this.handler = handler;
        this.baseDataPath = baseDataPath;
    }

    public <T extends CRUD.Identifiable> CRUD<T> createCRUD(
            Class<T> clazz,
            String subdirectory,
            CRUDAdapters adapters) {
        String path = baseDataPath + "/" + subdirectory;
        return new CRUD<T>(handler) {
            @Override
            public Class<T> getClazz() { return clazz; }

            @Override
            public String getPath() { return path; }

            @Override
            public CRUDAdapters getCRUDAdapters() { return adapters; }
        };
    }
}
```

## Troubleshooting

### Manager not loading data

1. Check file exists: `<path>/<id>.json`
2. Ensure `registry.initialize()` was called
3. Enable logging: `registry.setLoggingEnabled(true)`
4. Verify JSON format is valid

### ClassCastException with adapters

Ensure global adapters are registered *before* creating managers:

```java
// Register adapters first
registry.getGlobalAdapters().addModule(new JavaTimeModule());

// Then create managers (they inherit global adapters)
profileManager = new ProfileManager(crud, registry, logger);
```

### Data not saving

1. Check directory exists and is writable
2. Ensure `manager.add()` or `update()` was called (not just `new Entity()`)
3. Verify no exceptions in logs

### Manager not registered

1. Ensure you're using the DI constructor: `super(crud, registry)`
2. Verify `registry.initialize()` is called after all managers are created
3. Check that the same `CRUDRegistry` instance is passed to all managers

## License

This library is provided as-is for use in your projects. Modify and distribute freely.

## Contributing

This is a personal library, but suggestions and improvements are welcome. Open an issue or submit a pull request.

## Version History

- **1.0-SNAPSHOT** - Initial release
  - Dual Jackson/GSON support
  - Dependency injection pattern
  - Thread-safe operations
  - Atomic write operations
  - AutoRegister system
  - CRUDAdapters for custom serialization