package me.mortaldev.crudapi.loading;

import me.mortaldev.crudapi.CRUDAdapters;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CRUDRegistry {

  private static final Logger CRUD_LOGGER = Logger.getLogger("CRUDRegistry");
  private boolean loggingEnabled = false;
  private final CRUDAdapters globalCRUDAdapters = new CRUDAdapters();

  private static CRUDRegistry globalInstance;

  /**
   * Creates a new CRUDRegistry instance.
   *
   * <p>This constructor allows for dependency injection patterns where you can create
   * and manage your own registry instance. This is the recommended approach for new code.
   *
   * <p><b>Example usage:</b>
   * <pre>{@code
   * // In your plugin's onEnable():
   * CRUDRegistry registry = new CRUDRegistry();
   * CRUDRegistry.setGlobalInstance(registry); // For backward compatibility
   *
   * // Pass to managers via constructor injection
   * ProfileManager profiles = new ProfileManager(profileCRUD, registry, logger);
   * }</pre>
   */
  public CRUDRegistry() {}

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
    if (globalInstance == null) {
      globalInstance = new CRUDRegistry();
    }
    return globalInstance;
  }

  /**
   * Sets the global singleton instance of CRUDRegistry.
   *
   * <p>This method is provided to maintain backward compatibility during migration from singleton
   * to dependency injection. Call this in your plugin's initialization if you have code that still
   * uses {@link #getInstance()}.
   *
   * <p><b>Example:</b>
   * <pre>{@code
   * CRUDRegistry registry = new CRUDRegistry();
   * CRUDRegistry.setGlobalInstance(registry); // For old code using getInstance()
   * }</pre>
   *
   * @param instance The registry instance to set as global
   * @deprecated This is a temporary bridge during migration. Once all code uses DI, this method is not needed.
   */
  @Deprecated(since = "2.0", forRemoval = true)
  public static void setGlobalInstance(CRUDRegistry instance) {
    globalInstance = instance;
  }

  /**
   * Returns the globally configured CRUDAdapters instance. You can use this to register adapters
   * and modules that should apply to all CRUD managers.
   *
   * @return The global CRUDAdapters instance.
   */
  public CRUDAdapters getGlobalAdapters() {
    return globalCRUDAdapters;
  }

  /**
   * Enables or disables verbose informational logging for the CRUDRegistry. The manager
   * initialization count and critical errors will always be logged.
   *
   * @param enabled true to enable verbose logging, false to disable.
   */
  public void setLoggingEnabled(boolean enabled) {
    this.loggingEnabled = enabled;
    if (enabled) {
      logVerbose("CRUDRegistry verbose logging has been enabled.");
    }
  }

  private void logVerbose(String message) {
    if (loggingEnabled) {
      CRUD_LOGGER.info(message);
    }
  }

  private final Set<ILoadable> registeredManagers = ConcurrentHashMap.newKeySet();

  public void register(ILoadable manager) {
    logVerbose("Successfully registered manager: " + manager.getClass().getSimpleName());
    registeredManagers.add(manager);
  }

  @Deprecated
  public void scanAndRegister(ClassLoader loader, String basePackage) {
    logVerbose("Scanning for managers in package: " + basePackage);
    ConfigurationBuilder config =
        new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forClassLoader(loader))
            .forPackages(basePackage)
            .addClassLoaders(loader);
    Reflections reflections = new Reflections(config);

    Set<Class<? extends IRegistrable>> registrableClasses =
        reflections.getSubTypesOf(IRegistrable.class);
    for (Class<?> clazz : registrableClasses) {
      if (!clazz.isAnnotationPresent(AutoRegister.class)) {
        continue;
      }
      try {
        Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
        if (!Modifier.isPublic(getInstanceMethod.getModifiers())
            || !Modifier.isStatic(getInstanceMethod.getModifiers())) {
          CRUD_LOGGER.log(
              Level.SEVERE,
              "Failed to process "
                  + clazz.getSimpleName()
                  + ": The getInstance() method must be public and static.");
          continue;
        }

        logVerbose("Discovered manager, invoking getInstance(): " + clazz.getSimpleName());
        getInstanceMethod.invoke(null);

      } catch (NoSuchMethodException e) {
        CRUD_LOGGER.log(
            Level.SEVERE,
            "FATAL: Class '"
                + clazz.getSimpleName()
                + "' is marked with @AutoRegister but does not have a public static getInstance() method. It cannot be loaded.");
      } catch (Exception e) {
        CRUD_LOGGER.log(Level.SEVERE, "Failed to auto-process manager: " + clazz.getName(), e);
      }
    }
  }

  /** Initializes all registered managers. */
  public void initialize() {
    CRUD_LOGGER.info("Initializing " + registeredManagers.size() + " registered managers...");

    registeredManagers.forEach(ILoadable::load);

    logVerbose("All managers initialized successfully.");
  }
}
