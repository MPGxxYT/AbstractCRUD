package me.mortaldev.abstractcrud.crudapi.loading;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Collects {@link ILoadable}s so they can all be loaded in one call, typically from a plugin's
 * {@code onEnable()}. {@code CRUDManager} and {@code SingleCRUD} register themselves when given a
 * registry.
 *
 * <pre>{@code
 * CRUDRegistry registry = new CRUDRegistry();
 * CRUDManager<Profile> profiles = new CRUDManager<>(profileCrud, Profile::new, registry);
 * registry.initialize(); // loads every registered manager
 * }</pre>
 */
public class CRUDRegistry {

  private static final Logger CRUD_LOGGER = Logger.getLogger("CRUDRegistry");
  private final Set<ILoadable> registeredManagers = ConcurrentHashMap.newKeySet();
  private volatile boolean loggingEnabled = false;

  public CRUDRegistry() {}

  /**
   * Enables or disables verbose informational logging for the CRUDRegistry. The manager
   * initialization count is always logged.
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

  public void register(ILoadable manager) {
    logVerbose("Successfully registered manager: " + manager.getClass().getSimpleName());
    registeredManagers.add(manager);
  }

  /** Loads every registered manager. */
  public void initialize() {
    CRUD_LOGGER.info("Initializing " + registeredManagers.size() + " registered managers...");

    registeredManagers.forEach(ILoadable::load);

    logVerbose("All managers initialized successfully.");
  }
}
