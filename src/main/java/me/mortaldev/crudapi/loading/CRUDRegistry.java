package me.mortaldev.crudapi.loading;

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

  private static class Singleton {
    private static final CRUDRegistry INSTANCE = new CRUDRegistry();
  }

  public static CRUDRegistry getInstance() {
    return Singleton.INSTANCE;
  }

  private CRUDRegistry() {}

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
