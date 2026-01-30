package me.mortaldev.crudapi.loading;

import me.mortaldev.crudapi.CRUDAdapters;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CRUDRegistry {

  private static final Logger CRUD_LOGGER = Logger.getLogger("CRUDRegistry");
  private boolean loggingEnabled = false;
  private final CRUDAdapters globalCRUDAdapters = new CRUDAdapters();

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

  /** Initializes all registered managers. */
  public void initialize() {
    CRUD_LOGGER.info("Initializing " + registeredManagers.size() + " registered managers...");

    registeredManagers.forEach(ILoadable::load);

    logVerbose("All managers initialized successfully.");
  }
}
