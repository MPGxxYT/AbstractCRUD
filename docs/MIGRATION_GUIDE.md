# AbstractCRUD v2.0 - Migration Guide to Dependency Injection

## Overview

AbstractCRUD v2.0 has been refactored to support **Dependency Injection (DI)** while maintaining **full backward compatibility** with existing code. This guide explains what changed, why, and how to migrate.

---

## What Changed

### Before (v1.x - Singleton Pattern)

```java
// Singletons everywhere
CRUDRegistry.getInstance().register(manager);
Jackson.getInstance();
ProfileCRUD.getInstance();
ProfileManager.getInstance();
```

**Problems**:
- Hidden dependencies
- Impossible to test
- Tight coupling
- Manual initialization order

### After (v2.0 - Dependency Injection)

```java
// Explicit dependencies
CRUDRegistry registry = new CRUDRegistry();
Jackson jackson = new Jackson();
ProfileCRUD crud = new ProfileCRUD(path, jackson);
ProfileManager manager = new ProfileManager(crud, registry);
```

**Benefits**:
- ✅ Explicit dependencies
- ✅ Easy to test with mocks
- ✅ Loose coupling
- ✅ Clear initialization order

---

## Migration Strategy

You have **three options**:

### Option 1: Keep Using Singletons (No Changes Required)

All old code continues to work. The deprecated methods remain functional.

```java
// This still works (but shows deprecation warnings)
CRUDRegistry.getInstance().register(manager);
ProfileCRUD.getInstance();
```

### Option 2: Gradual Migration (Recommended)

Migrate one manager at a time while keeping existing code working.

### Option 3: Full Migration

Refactor everything to use DI in one go.

---

## Step-by-Step Migration

### Step 1: Update Your Plugin's Main Class

Create instances instead of using singletons.

#### Before:
```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Singletons initialized automatically
        CRUDRegistry.getInstance().scanAndRegister(
            getClassLoader(),
            "com.example.plugin"
        );
        CRUDRegistry.getInstance().initialize();
    }
}
```

#### After:
```java
public class MyPlugin extends JavaPlugin {
    private CRUDRegistry registry;
    private Jackson jackson;

    @Override
    public void onEnable() {
        // Create instances
        registry = new CRUDRegistry();
        jackson = new Jackson();

        // Set global instances for backward compatibility
        CRUDRegistry.setGlobalInstance(registry);
        Jackson.setGlobalInstance(jackson);

        // Now your old code using getInstance() will still work
        registry.scanAndRegister(getClassLoader(), "com.example.plugin");
        registry.initialize();
    }
}
```

---

### Step 2: Update CRUD Implementations

#### Before (Singleton):
```java
public class ProfileCRUD extends CRUD<Profile> {
    private static final String PATH = Main.getInstance().getDataFolder() + "/profiles/";

    private static class Singleton {
        private static final ProfileCRUD INSTANCE = new ProfileCRUD();
    }

    public static ProfileCRUD getInstance() {
        return Singleton.INSTANCE;
    }

    private ProfileCRUD() {
        super(Jackson.getInstance());
    }

    @Override
    public String getPath() {
        return PATH;
    }

    // ... other methods
}
```

#### After (Dependency Injection):
```java
public class ProfileCRUD extends CRUD<Profile> {
    private final String storagePath;

    /**
     * Constructor with dependency injection.
     *
     * @param storagePath Path to storage directory
     * @param jackson JSON handler
     */
    public ProfileCRUD(String storagePath, Jackson jackson) {
        super(jackson);
        this.storagePath = storagePath;
    }

    @Override
    public String getPath() {
        return storagePath;
    }

    // ... other methods (no changes needed)
}
```

---

### Step 3: Update Manager Implementations

#### Before (Singleton):
```java
@AutoRegister
public class ProfileManager extends CRUDManager<Profile> {
    private static class Singleton {
        private static final ProfileManager INSTANCE = new ProfileManager();
    }

    public static ProfileManager getInstance() {
        return Singleton.INSTANCE;
    }

    private ProfileManager() {
        CRUDRegistry.getInstance().register(this);
    }

    @Override
    public CRUD<Profile> getCRUD() {
        return ProfileCRUD.getInstance();
    }

    @Override
    public void log(String message) {
        Main.log(message);
    }

    @Override
    public Profile getNewInstance(String id) {
        return Profile.create(id);
    }
}
```

#### After (Dependency Injection):
```java
public class ProfileManager extends CRUDManager<Profile> {
    private final Logger logger;

    /**
     * Constructor with dependency injection.
     *
     * @param crud The CRUD implementation for persistence
     * @param registry The registry to register with
     * @param logger Logger for this manager
     */
    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry, Logger logger) {
        super(crud, registry);  // Calls parent constructor
        this.logger = logger;
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public Profile getNewInstance(String id) {
        return Profile.create(id);
    }
}
```

**What changed**:
- ✅ Removed `@AutoRegister` (manual registration now)
- ✅ Removed singleton pattern
- ✅ Added constructor with dependencies
- ✅ Removed `getCRUD()` override (parent handles it)
- ✅ Injected logger instead of calling `Main.log()`

---

### Step 4: Wire Dependencies in Main

Create and connect all dependencies in your plugin's `onEnable()`.

```java
public class MyPlugin extends JavaPlugin {
    // Store as instance fields
    private CRUDRegistry registry;
    private ProfileManager profileManager;
    private MapManager mapManager;

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();

        // 1. Create registry and handler
        registry = new CRUDRegistry();
        Jackson jackson = new Jackson();

        // 2. Set global instances for backward compatibility
        CRUDRegistry.setGlobalInstance(registry);
        Jackson.setGlobalInstance(jackson);

        // 3. Register global type adapters
        registry.getGlobalAdapters().registerTypeAdapter(
            ItemStack.class,
            new ItemStackAdapter()
        );

        // 4. Create CRUD implementations
        String profilePath = getDataFolder() + "/profiles/";
        ProfileCRUD profileCRUD = new ProfileCRUD(profilePath, jackson);
        profileCRUD.setRegistry(registry);  // For global adapters

        String mapPath = getDataFolder() + "/maps/";
        MapCRUD mapCRUD = new MapCRUD(mapPath, jackson);
        mapCRUD.setRegistry(registry);

        // 5. Create managers with dependencies
        profileManager = new ProfileManager(profileCRUD, registry, logger);
        mapManager = new MapManager(mapCRUD, registry, logger);

        // 6. Load data
        registry.initialize();  // Calls load() on all registered managers

        // 7. Register commands with dependencies
        registerCommands();

        // 8. Register listeners with dependencies
        registerListeners();

        logger.info("Plugin enabled with " + registry.getAllManagers().size() + " managers");
    }

    private void registerCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);

        // Inject dependencies into commands
        commandManager.registerCommand(new ProfileCommand(profileManager));
        commandManager.registerCommand(new MapCommand(mapManager));
    }

    private void registerListeners() {
        // Inject dependencies into listeners
        getServer().getPluginManager().registerEvents(
            new ProfileListener(profileManager),
            this
        );
    }

    @Override
    public void onDisable() {
        // Save all data
        if (profileManager != null) {
            profileManager.getSet().forEach(profileManager::update);
        }
        if (mapManager != null) {
            mapManager.getSet().forEach(mapManager::update);
        }
    }

    // Getters for other parts of the plugin
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
```

---

### Step 5: Update Commands and Listeners

#### Before (Hidden Dependencies):
```java
public class ProfileCommand extends BaseCommand {
    @CommandAlias("profile")
    public void onProfile(Player player) {
        // Hidden dependency
        Profile profile = ProfileManager.getInstance()
            .getByIDCreate(player.getUniqueId().toString());

        player.sendMessage("Level: " + profile.getLevel());
    }
}
```

#### After (Explicit Dependencies):
```java
public class ProfileCommand extends BaseCommand {
    private final ProfileManager profileManager;

    /**
     * Constructor with dependency injection.
     * Dependencies are now explicit and testable!
     */
    public ProfileCommand(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @CommandAlias("profile")
    public void onProfile(Player player) {
        // Uses injected dependency
        Profile profile = profileManager
            .getByIDCreate(player.getUniqueId().toString());

        player.sendMessage("Level: " + profile.getLevel());
    }
}
```

---

## Backward Compatibility

### How It Works

The refactored library maintains backward compatibility through:

1. **Deprecated but functional singleton methods**
   ```java
   @Deprecated(since = "2.0", forRemoval = true)
   public static CRUDRegistry getInstance() {
       // Still works!
   }
   ```

2. **Bridge methods for global instances**
   ```java
   CRUDRegistry.setGlobalInstance(registry);
   // Now getInstance() returns your instance
   ```

3. **Dual constructor support in CRUDManager**
   ```java
   // Old way still works
   protected CRUDManager() { }

   // New way available
   protected CRUDManager(CRUD<T> crud, CRUDRegistry registry) { }
   ```

### Deprecation Warnings

You'll see warnings like:
```
ProfileCRUD.getInstance() is deprecated and will be removed in a future version.
Use 'new ProfileCRUD(path, jackson)' instead.
```

These are **informational only** - your code still works!

---

## Testing With Dependency Injection

One of the biggest benefits of DI is testability.

### Before (Impossible to Test):
```java
@Test
void testProfileManager() {
    // Can't test! ProfileManager.getInstance() uses real file system
    // Can't mock ProfileCRUD.getInstance()
    // Can't control dependencies
}
```

### After (Easy to Test):
```java
@Test
void testProfileManager() {
    // Create mock dependencies
    CRUD<Profile> mockCRUD = mock(CRUD.class);
    CRUDRegistry mockRegistry = mock(CRUDRegistry.class);
    Logger mockLogger = mock(Logger.class);

    // Inject mocks
    ProfileManager manager = new ProfileManager(mockCRUD, mockRegistry, mockLogger);

    // Control behavior
    when(mockCRUD.getData("test-id")).thenReturn(Optional.of(testProfile));

    // Test in isolation
    Optional<Profile> result = manager.getByID("test-id");

    // Verify
    assertTrue(result.isPresent());
    verify(mockCRUD).getData("test-id");
}
```

---

## Common Patterns

### Pattern 1: Manager with Multiple Dependencies

```java
public class LobbyManager extends CRUDManager<Lobby> {
    private final ProfileManager profileManager;
    private final MapManager mapManager;
    private final Logger logger;

    public LobbyManager(
        CRUD<Lobby> crud,
        CRUDRegistry registry,
        ProfileManager profileManager,
        MapManager mapManager,
        Logger logger
    ) {
        super(crud, registry);
        this.profileManager = profileManager;
        this.mapManager = mapManager;
        this.logger = logger;
    }

    public Lobby createLobby(Player player, String mapId) {
        Profile profile = profileManager.getByIDCreate(player.getUniqueId().toString());
        Map map = mapManager.getByID(mapId).orElseThrow();

        Lobby lobby = Lobby.create(profile, map);
        add(lobby);
        return lobby;
    }

    @Override
    public Lobby getNewInstance(String id) {
        return Lobby.create(id);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }
}
```

### Pattern 2: Interface-Based DI

```java
// Create interfaces for managers
public interface IProfileManager {
    Profile getByIDCreate(String id);
    Optional<Profile> getByID(String id);
    void update(Profile profile);
}

// Implement interface
public class ProfileManager extends CRUDManager<Profile> implements IProfileManager {
    // Implementation
}

// Inject interface, not implementation
public class LobbyManager {
    private final IProfileManager profileManager;  // Interface!

    public LobbyManager(IProfileManager profileManager) {
        this.profileManager = profileManager;
    }
}

// Now you can swap implementations or use mocks
ProfileManager realManager = new ProfileManager(...);
IProfileManager mockManager = mock(IProfileManager.class);
```

---

## Removal Timeline

- **v2.0 (Current)**: DI support added, singletons deprecated
- **v2.1 (Future)**: Deprecation warnings upgraded to errors
- **v3.0 (Future)**: Singleton methods removed entirely

---

## FAQ

### Q: Do I have to migrate immediately?
**A**: No! Your existing code continues to work. Migrate at your own pace.

### Q: What if I have a large codebase?
**A**: Use the hybrid approach:
1. Create DI instances in Main
2. Set global instances for backward compatibility
3. Migrate one manager at a time
4. Old and new code coexist peacefully

### Q: Will this break my existing plugins?
**A**: No! The library is 100% backward compatible.

### Q: Can I mix singleton and DI patterns?
**A**: Yes, during migration. Eventually, aim for full DI.

### Q: Why should I migrate if singletons work?
**A**: Benefits of DI:
- Testable code
- Clear dependencies
- Better architecture
- Easier to maintain
- More flexible

### Q: How do I test with @AutoRegister?
**A**: Stop using `@AutoRegister`. Manually create and register managers in your tests.

---

## Migration Checklist

- [ ] Update Main.java to create instances instead of using singletons
- [ ] Set global instances for backward compatibility
- [ ] Create interfaces for managers (optional but recommended)
- [ ] Update CRUD implementations to use constructor injection
- [ ] Update Manager implementations to use constructor injection
- [ ] Update commands to use constructor injection
- [ ] Update listeners to use constructor injection
- [ ] Write unit tests for managers
- [ ] Remove @AutoRegister annotations
- [ ] Remove getInstance() methods from your code
- [ ] Remove static fields and singleton patterns
- [ ] Update documentation

---

## Support

If you encounter issues during migration:
1. Check this guide's examples
2. Review the JavaDoc in the refactored classes
3. Look at deprecation messages for migration hints
4. Open an issue on GitHub with your specific scenario

---

## Summary

**What You Need to Do**:
1. Create instances in Main.java
2. Pass dependencies via constructors
3. Remove singletons from your code gradually

**What You Get**:
- Testable code
- Clear dependencies
- Better architecture
- Easier maintenance

**Backward Compatibility**:
- All old code still works
- Migrate at your own pace
- No breaking changes

The migration is **optional but recommended**. Start with new code using DI, migrate old code over time.