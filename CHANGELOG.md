# AbstractCRUD Changelog

## [2.0.0] - Dependency Injection Support

### ✨ New Features

#### 1. Dependency Injection Support
- **CRUDRegistry** can now be instantiated: `new CRUDRegistry()`
- **CRUD** implementations can inject Handler via constructor
- **CRUDManager** supports constructor injection: `new CRUDManager(crud, registry)`
- **Jackson** and **GSON** handlers can be instantiated: `new Jackson()`
- All classes now support DI patterns for better testability

#### 2. Registry Management
- Added `setRegistry(CRUDRegistry)` method to CRUD class
- CRUD implementations can now use injected registry for global adapters
- Supports multiple registry instances (useful for testing)

#### 3. Bridge Methods for Migration
- Added `CRUDRegistry.setGlobalInstance(registry)` for backward compatibility
- Added `Jackson.setGlobalInstance(jackson)` for backward compatibility
- Added `GSON.setGlobalInstance(gson)` for backward compatibility

### 🔄 Changed

#### CRUDRegistry.java
```diff
+ public CRUDRegistry()  // New: Public constructor for DI
- private CRUDRegistry() // Old: Private singleton

+ public static void setGlobalInstance(CRUDRegistry instance)
  // New: Set global instance for backward compat

  @Deprecated(since = "2.0", forRemoval = true)
  public static CRUDRegistry getInstance()
  // Still works but deprecated
```

#### CRUDManager.java
```diff
+ protected CRUDManager(CRUD<T> crud, CRUDRegistry registry)
  // New: DI constructor (recommended)

  @Deprecated(since = "2.0", forRemoval = true)
  protected CRUDManager()
  // Old: Default constructor (still works)

  @Deprecated(since = "2.0", forRemoval = false)
  public abstract CRUD<T> getCRUD()
  // Deprecated when using DI constructor
```

#### CRUD.java
```diff
+ private CRUDRegistry registry
+ public void setRegistry(CRUDRegistry registry)
  // New: Injectable registry

  protected CRUDAdapters getCombinedAdapters()
  // Now uses injected registry if available, falls back to getInstance()
```

#### Jackson.java & GSON.java
```diff
+ public Jackson()  // New: Public constructor for DI
- private Jackson() // Old: Private singleton

+ public static void setGlobalInstance(Jackson instance)
  // New: Set global instance for backward compat

  @Deprecated(since = "2.0", forRemoval = true)
  public static Jackson getInstance()
  // Still works but deprecated
```

### 📝 Documentation

#### Added Comprehensive JavaDoc
- All deprecated methods now include detailed migration examples
- Constructor JavaDoc explains DI patterns with code examples
- Class-level documentation updated with usage patterns

#### Example from CRUDRegistry:
```java
/**
 * Returns the global singleton instance of CRUDRegistry.
 *
 * @return The global CRUDRegistry instance
 * @deprecated Use dependency injection instead. Create your own instance with {@code new CRUDRegistry()}
 *             and pass it to your managers via constructor injection. This method will be removed in a future version.
 *             <p><b>Migration:</b>
 *             <pre>{@code
 * // Old way (deprecated):
 * CRUDRegistry.getInstance().register(manager);
 *
 * // New way (recommended):
 * CRUDRegistry registry = new CRUDRegistry();
 * ProfileManager manager = new ProfileManager(crud, registry, logger);
 *             }</pre>
 */
@Deprecated(since = "2.0", forRemoval = true)
public static CRUDRegistry getInstance() {
    // ...
}
```

### 🔧 Migration Support

#### Created MIGRATION_GUIDE.md
- Step-by-step migration instructions
- Before/after code examples
- Testing patterns with DI
- Common patterns and FAQ
- Migration checklist
- Timeline for deprecation removal

#### Backward Compatibility Strategy
1. **Phase 1 (v2.0)**: DI support added, singletons deprecated
2. **Phase 2 (v2.1)**: Deprecation warnings upgraded
3. **Phase 3 (v3.0)**: Singleton methods removed

### ✅ Backward Compatibility

**100% backward compatible!** All existing code continues to work:

```java
// Old code still works (shows deprecation warnings)
CRUDRegistry.getInstance().register(manager);
Jackson.getInstance();
GSON.getInstance();
ProfileCRUD.getInstance();
ProfileManager.getInstance();

// New code uses DI
CRUDRegistry registry = new CRUDRegistry();
Jackson jackson = new Jackson();
ProfileCRUD crud = new ProfileCRUD(path, jackson);
ProfileManager manager = new ProfileManager(crud, registry, logger);
```

### 🧪 Testing Improvements

DI makes testing possible:

```java
// Before: Impossible to test
@Test
void test() {
    // ProfileManager.getInstance() uses real file system
    // Can't mock dependencies
}

// After: Easy to test
@Test
void test() {
    CRUD<Profile> mockCRUD = mock(CRUD.class);
    CRUDRegistry mockRegistry = mock(CRUDRegistry.class);
    ProfileManager manager = new ProfileManager(mockCRUD, mockRegistry, logger);

    // Test with mocks!
    when(mockCRUD.getData("test")).thenReturn(Optional.of(profile));
    Optional<Profile> result = manager.getByID("test");
    verify(mockCRUD).getData("test");
}
```

### 📦 Dependencies

No new dependencies added. All changes are internal refactoring.

### ⚠️ Breaking Changes

**None!** This is a **non-breaking release**.

All deprecated methods will be removed in v3.0 (future release).

### 🎯 Recommendations

#### For New Code
```java
// ✅ DO: Use DI constructors
ProfileManager manager = new ProfileManager(crud, registry, logger);

// ❌ DON'T: Use singletons
ProfileManager manager = ProfileManager.getInstance();
```

#### For Existing Code
```java
// Option 1: Keep as-is (works but deprecated)
CRUDRegistry.getInstance().register(manager);

// Option 2: Hybrid approach during migration
CRUDRegistry registry = new CRUDRegistry();
CRUDRegistry.setGlobalInstance(registry);  // Old code still works
ProfileManager newManager = new ProfileManager(crud, registry, logger);  // New DI

// Option 3: Full migration (recommended long-term)
// Remove all getInstance() calls
// Use constructor injection everywhere
```

### 📊 Statistics

- **Files Modified**: 5 core files
  - `CRUDRegistry.java`
  - `CRUDManager.java`
  - `CRUD.java`
  - `Jackson.java`
  - `GSON.java`

- **Lines of Documentation Added**: 200+
  - Detailed JavaDoc for all changes
  - Migration examples in every deprecated method
  - Comprehensive migration guide

- **Backward Compatibility**: 100%
  - All existing code works without changes
  - Deprecation warnings guide migration
  - Hybrid approaches supported

### 🔮 Future Plans

#### v2.1 (Next Release)
- Add helper methods for common DI patterns
- Consider fluent builder API for complex setups
- Add more testing utilities

#### v3.0 (Future)
- Remove all deprecated singleton methods
- Pure DI implementation
- Simplified API surface

### 📚 Resources

- **MIGRATION_GUIDE.md**: Complete migration instructions
- **JavaDoc**: Inline documentation with examples
- **Examples**: Before/after patterns in this changelog

### 🙏 Acknowledgments

This refactoring enables:
- ✅ Better testing practices
- ✅ Clearer code architecture
- ✅ Easier maintenance
- ✅ More flexible designs
- ✅ Industry best practices

while maintaining **full backward compatibility** with existing code.

---

## [1.x.x] - Previous Versions

### Singleton Pattern Era
- Used singleton pattern throughout
- `@AutoRegister` annotation for automatic registration
- `CRUDRegistry.getInstance()` for registry access
- Hidden dependencies
- Difficult to test

---

## Migration Example

### Before (v1.x):
```java
@AutoRegister
public class ProfileManager extends CRUDManager<Profile> {
    private static final ProfileManager INSTANCE = new ProfileManager();

    public static ProfileManager getInstance() {
        return INSTANCE;
    }

    private ProfileManager() {
        CRUDRegistry.getInstance().register(this);
    }

    @Override
    public CRUD<Profile> getCRUD() {
        return ProfileCRUD.getInstance();
    }

    @Override
    public void log(String msg) {
        Main.log(msg);
    }
}
```

### After (v2.0):
```java
public class ProfileManager extends CRUDManager<Profile> {
    private final Logger logger;

    public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry, Logger logger) {
        super(crud, registry);
        this.logger = logger;
    }

    @Override
    public void log(String msg) {
        logger.info(msg);
    }

    @Override
    public Profile getNewInstance(String id) {
        return Profile.create(id);
    }
}

// In Main.java:
CRUDRegistry registry = new CRUDRegistry();
ProfileCRUD crud = new ProfileCRUD(path, new Jackson());
ProfileManager manager = new ProfileManager(crud, registry, logger);
```

---

## Version History

- **2.0.0** - Dependency Injection Support (Current)
- **1.x.x** - Singleton Pattern Era