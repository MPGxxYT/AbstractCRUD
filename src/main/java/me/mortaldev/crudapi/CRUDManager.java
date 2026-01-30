package me.mortaldev.crudapi;

import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;
import me.mortaldev.crudapi.loading.IRegistrable;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Abstract manager class for managing CRUD operations on identifiable data objects.
 *
 * <p>This class provides thread-safe management of a collection of objects that can be
 * created, read, updated, and deleted (CRUD). It includes:
 * <ul>
 *   <li>In-memory caching with O(1) ID lookups via {@link ConcurrentHashMap}</li>
 *   <li>Thread-safe synchronized operations for add, remove, update, and load</li>
 *   <li>Automatic file system persistence through the {@link CRUD} interface</li>
 *   <li>Support for lazy loading and eager loading patterns</li>
 *   <li>Dependency injection support for testability and flexibility</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> All mutating operations (add, remove, update, load) are synchronized.
 * Read operations use a concurrent cache for efficient concurrent access.
 *
 * <p><b>Performance:</b> The ID cache provides O(1) lookup time compared to O(n) linear search.
 * Cache is automatically maintained during all data modifications.
 *
 * <p><b>Dependency Injection:</b> Use the constructor {@link #CRUDManager(CRUD, CRUDRegistry)}
 * to pass dependencies explicitly. The old pattern of overriding {@link #getCRUD()} is deprecated.
 *
 * <p><b>Example usage (new DI pattern):</b>
 * <pre>{@code
 * public class ProfileManager extends CRUDManager<Profile> {
 *     public ProfileManager(CRUD<Profile> crud, CRUDRegistry registry) {
 *         super(crud, registry);
 *     }
 *
 *     @Override
 *     public Profile getNewInstance(String id) {
 *         return Profile.create(id);
 *     }
 *
 *     @Override
 *     public void log(String message) {
 *         logger.info(message);
 *     }
 * }
 * }</pre>
 *
 * @param <T> The type of data object this manager handles, must implement {@link CRUD.Identifiable}
 */
public abstract class CRUDManager<T extends CRUD.Identifiable> implements IRegistrable, ILoadable {

  /** Primary data storage using HashSet for efficient contains/remove operations. */
  private final HashSet<T> set = new HashSet<>();

  /** Concurrent cache mapping IDs to objects for O(1) lookup performance. */
  private final Map<String, T> idCache = new ConcurrentHashMap<>();

  /** The CRUD implementation for persistence operations. */
  private CRUD<T> crud;

  /** The registry this manager is registered with. */
  private CRUDRegistry registry;

  /**
   * Constructor with dependency injection (recommended).
   *
   * <p>This constructor allows you to inject dependencies explicitly, enabling proper testing
   * and avoiding the singleton pattern. Use this constructor for all new code.
   *
   * <p><b>Example:</b>
   * <pre>{@code
   * // In your plugin's onEnable():
   * CRUDRegistry registry = new CRUDRegistry();
   * ProfileCRUD crud = new ProfileCRUD(storagePath, jackson);
   * crud.setRegistry(registry);
   * ProfileManager manager = new ProfileManager(crud, registry);
   * }</pre>
   *
   * @param crud The CRUD implementation for persistence
   * @param registry The registry to register this manager with (can be null if no registration needed)
   */
  protected CRUDManager(CRUD<T> crud, CRUDRegistry registry) {
    this.crud = crud;
    this.registry = registry;
    if (registry != null) {
      registry.register(this);
    }
  }

  /**
   * Get the CRUD object used by this manager.
   *
   * <p>When using the new DI constructor {@link #CRUDManager(CRUD, CRUDRegistry)},
   * this method returns the injected CRUD instance. When using the deprecated default constructor,
   * subclasses must override this method.
   *
   * @return The CRUD object used by this manager
   * @deprecated When using DI, you don't need to override this method. It's only needed for backward compatibility.
   */
  @Deprecated(since = "2.0", forRemoval = false)
  public CRUD<T> getCRUD() {
    if (crud != null) {
      return crud;
    }
    throw new UnsupportedOperationException(
        "getCRUD() must be overridden when using the default constructor. " +
        "Consider using the CRUDManager(CRUD, CRUDRegistry) constructor instead.");
  }

  /**
   * Log a message to the console. This is used for logging messages to the console from within this
   * manager.
   *
   * @param string The message to log.
   */
  public abstract void log(String string);

  /**
   * Loads all data from the directory specified by {@link #getCRUD()}'s {@link
   * CRUD#getData(String)} method. If a file in the directory fails to load, a message is logged
   * using the {@link #log(String)} method.
   *
   * <p>This method:
   * <ul>
   *   <li>Clears the existing data set and cache before loading</li>
   *   <li>Creates the directory if it doesn't exist</li>
   *   <li>Only processes files with .json extension</li>
   *   <li>Updates both the set and cache for each loaded object</li>
   * </ul>
   *
   * <p><b>Thread Safety:</b> This method is synchronized to prevent concurrent modifications.
   */
  @Override
  public synchronized void load() {
    set.clear();
    idCache.clear();
    File mineDir = new File(getCRUD().getPath());
    if (!mineDir.exists()) {
      if (!mineDir.mkdirs()) {
        log("Failed to create directory: " + mineDir.getPath());
        return;
      }
    }
    File[] files = mineDir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (!file.isFile() || !file.getName().endsWith(".json")) {
        continue;
      }
      String fileNameWithoutExtension = file.getName().replace(".json", "");
      Optional<T> data = getCRUD().getData(fileNameWithoutExtension);
      if (data.isEmpty()) {
        log("Failed to load data: " + file.getName());
        continue;
      }
      T loadedData = data.get();
      set.add(loadedData);
      idCache.put(loadedData.getId(), loadedData);
    }
  }

  public synchronized boolean loadByID(String id) {
    if (id == null || id.isEmpty()) {
      log("Cannot load data with null or empty ID");
      return false;
    }
    File file = new File(getCRUD().getPath(), id + ".json");
    if (!file.exists()) {
      return false;
    }
    Optional<T> data = getCRUD().getData(id);
    if (data.isEmpty()) {
      log("Failed to load data: " + file.getName());
      return false;
    }
    T loadedData = data.get();
    getByID(id).ifPresent(existing -> {
      set.remove(existing);
      idCache.remove(id);
    });
    set.add(loadedData);
    idCache.put(id, loadedData);
    return true;
  }

  /**
   * Retrieve a data object by its ID with optional creation if not found.
   *
   * <p><b>Performance:</b> This method uses a concurrent cache for O(1) lookup time
   * instead of O(n) linear search. Falls back to linear search if cache is out of sync.
   *
   * @param id The ID of the data object to retrieve (null-safe)
   * @param createNew If true, creates a new instance via {@link #getNewInstance(String)} if not found
   * @return An {@link Optional} containing the data object with the specified ID, or an empty
   *     {@link Optional} if no such data object exists and createNew is false
   */
  public Optional<T> getByID(String id, boolean createNew) {
    if (id == null) {
      return Optional.empty();
    }
    // Check cache first for O(1) lookup
    T cached = idCache.get(id);
    if (cached != null) {
      return Optional.of(cached);
    }
    // Fallback to linear search (in case cache is out of sync)
    for (T data : set) {
      if (id.equals(data.getId())) {
        idCache.put(id, data); // Update cache
        return Optional.of(data);
      }
    }
    if (createNew) {
      return Optional.of(getNewInstance(id));
    }
    return Optional.empty();
  }

  /**
   * Retrieve a data object by its ID.
   *
   * @param id The ID of the data object to retrieve.
   * @return An {@link Optional} containing the data object with the specified ID, or an empty
   *     {@link Optional} if no such data object exists.
   */

  public Optional<T> getByID(String id) {
    return getByID(id, false);
  }

  /**
   * Retrieve a data object by its ID, using the ID from the given data object.
   *
   * @param data The data object containing the ID of the data object to retrieve.
   * @return An {@link Optional} containing the data object with the specified ID, or an empty
   *     {@link Optional} if no such data object exists.
   * @see #getByID(CRUD.Identifiable)
   */
  public Optional<T> getByID(T data) {
    return getByID(data.getId(), false);
  }

  /**
   * Retrieves a data object by its ID, creating a new instance if it doesn't exist.
   *
   * @param id The ID of the data object to retrieve or create.
   * @return The data object with the specified ID, either existing or newly created.
   */

  public T getByIDCreate(String id) {
    return getByIDCreate(id, true);
  }

  /**
   * Retrieves a data object by its ID, creating a new instance if it doesn't exist.
   *
   * @param id The ID of the data object to retrieve or create.
   * @param add If true, the newly created data object will be added to the collection.
   * @return The data object with the specified ID, either existing or newly created.
   */

  public T getByIDCreate(String id, boolean add) {
    Optional<T> byID = getByID(id, true);
    T data = byID.orElse(null);
    if (data != null) {
      add(data);
    }
    return data;
  }

  /**
   * Creates a new instance of the data object with the given ID.
   *
   * @param string The ID for the new data object.
   * @return A new instance of the data object.
   */

  public abstract T getNewInstance(String string);

  /**
   * Determine if the given data object is in the collection.
   *
   * @param data The data object to check for.
   * @return True if the given data object is in the collection, false otherwise.
   */
  public boolean contains(T data) {
    return set.contains(data) || getByID(data.getId()).isPresent();
  }

  /**
   * Retrieve the collection of data objects.
   *
   * <p>If the collection is empty, this method will attempt to load the data from the underlying
   * data store.
   *
   * @return The collection of data objects.
   */
  public HashSet<T> getSet() {
    return getSet(false);
  }

  /**
   * Retrieve the collection of data objects.
   *
   * <p>If the collection is empty and the {@code load} parameter is true, this method will attempt
   * to load the data from the underlying data store.
   *
   * @param load If true, and the collection is empty, the data will be loaded from the underlying
   *     data store.
   * @return The collection of data objects.
   */
  public HashSet<T> getSet(boolean load) {
    if (set.isEmpty() && load) {
      load();
    }
    return set;
  }

  /**
   * Add a data object to the collection.
   *
   * <p>If the data object is already in the collection, or if a data object with the same ID is
   * already in the collection, this method will return false. Otherwise, the data object will be
   * added to the collection and the underlying data store will be updated.
   *
   * @param data The data object to add to the collection.
   * @return True if the data object was successfully added to the collection, false otherwise.
   */
  public synchronized boolean add(T data) {
    return add(data, true);
  }

  /**
   * Adds a data object to the collection.
   *
   * <p>If the data object is already in the collection, or if a data object with the same ID is
   * already in the collection, this method will return false. Otherwise, the data object will be
   * added to the collection and the underlying data store will be updated if the specified save
   * flag is true.
   *
   * @param data The data object to add to the collection.
   * @param saveToFile If true, the data object will be saved to the underlying data store.
   * @return True if the data object was successfully added to the collection, false otherwise.
   */
  public synchronized boolean add(T data, Boolean saveToFile) {
    if (data == null || data.getId() == null) {
      log("Cannot add null data or data with null ID");
      return false;
    }
    if (set.contains(data) || getByID(data.getId()).isPresent()) {
      return false;
    }
    set.add(data);
    idCache.put(data.getId(), data);
    if (saveToFile) {
      getCRUD().saveData(data);
    }
    return true;
  }

  /**
   * Removes a data object from the collection.
   *
   * <p>If the data object is not in the collection, or if a data object with the same ID is not in
   * the collection, this method will return false. Otherwise, the data object will be removed from
   * the collection and the underlying data store will be updated.
   *
   * @param data The data object to remove from the collection.
   * @return True if the data object was successfully removed from the collection, false otherwise.
   */
  public synchronized boolean remove(T data) {
    return remove(data, true);
  }

  public synchronized boolean remove(T data, Boolean deleteFile) {
    if (data == null || data.getId() == null) {
      log("Cannot remove null data or data with null ID");
      return false;
    }
    if (!set.contains(data) && getByID(data.getId()).isEmpty()) {
      return false;
    }
    set.remove(data);
    idCache.remove(data.getId());
    if (deleteFile) {
      getCRUD().deleteData(data);
    }
    return true;
  }

  /**
   * Updates a data object in the collection.
   *
   * <p>If the data object does not exist in the collection, this method will return false.
   * Otherwise, the data object will be replaced in the collection and the underlying data store
   * will be updated.
   *
   * @param data The data object to update in the collection.
   * @return True if the data object was successfully updated, false otherwise.
   */
  public synchronized boolean update(T data) {
    return update(data, true);
  }

  public synchronized boolean update(T data, Boolean updateFile) {
    if (data == null || data.getId() == null) {
      log("Cannot update null data or data with null ID");
      return false;
    }
    Optional<T> existing = getByID(data.getId());
    if (existing.isEmpty()) {
      return add(data, updateFile);
    }
    set.remove(existing.get());
    set.add(data);
    idCache.put(data.getId(), data);
    if (updateFile) {
      getCRUD().saveData(data);
    }
    return true;
  }
}
