package me.mortaldev.abstractcrud.crudapi.loading;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Collects the storage objects of a plugin so they can all be loaded with one call.
 *
 * <p>{@code CRUDManager} and {@code SingleCRUD} add themselves when you pass a registry to their
 * constructor. Create the registry and every storage object first, then call {@link
 * #initialize()} once:
 *
 * <pre>{@code
 * CRUDRegistry registry = new CRUDRegistry();
 * profiles = new CRUDManager<>(profileFiles, Profile::new, registry);
 * settings = new SingleCRUD<>(settingsFiles, "settings", Settings::new, registry);
 * registry.initialize();   // calls load() on both
 * }</pre>
 *
 * <p>Using a registry is optional. Without one, call {@code load()} on each manager yourself.
 *
 * <p>Registering and initializing are safe from any thread. The registry holds each object once,
 * and the order in which objects are loaded is not defined.
 */
public class CRUDRegistry {

  private static final Logger CRUD_LOGGER = Logger.getLogger("CRUDRegistry");
  private final Set<ILoadable> registeredManagers = ConcurrentHashMap.newKeySet();
  private volatile boolean loggingEnabled = false;

  /** Creates an empty registry. Use one registry per plugin. */
  public CRUDRegistry() {}

  /**
   * Turns verbose logging on or off. When on, each registration and the end of {@link
   * #initialize()} are logged at INFO under the {@code "CRUDRegistry"} logger. The number of
   * objects being initialized is always logged. Off by default.
   *
   * @param enabled {@code true} to log more, {@code false} for the default
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

  /**
   * Adds an object to be loaded by {@link #initialize()}. You rarely need this:
   * {@code CRUDManager} and {@code SingleCRUD} call it from their constructors. Use it for your own
   * {@link ILoadable} classes. Registering the same object twice has no effect.
   *
   * @param manager the object to load on {@link #initialize()}
   * @throws NullPointerException if {@code manager} is {@code null}
   */
  public void register(ILoadable manager) {
    logVerbose("Successfully registered manager: " + manager.getClass().getSimpleName());
    registeredManagers.add(manager);
  }

  /**
   * Calls {@link ILoadable#load()} on every registered object, on the calling thread.
   *
   * <p>Call it once in {@code onEnable}, after creating all storage objects. Calling it again
   * reloads everything from disk, replacing what is in memory. Objects registered after a call are
   * only loaded by the next one.
   *
   * <p>If a {@code load()} throws, the exception is passed on and objects after it may not be
   * loaded. The library's own {@code load()} methods log file problems instead of throwing.
   */
  public void initialize() {
    CRUD_LOGGER.info("Initializing " + registeredManagers.size() + " registered managers...");

    registeredManagers.forEach(ILoadable::load);

    logVerbose("All managers initialized successfully.");
  }
}
