package me.mortaldev.crudapi;

import me.mortaldev.crudapi.loading.ILoadable;
import me.mortaldev.crudapi.loading.IRegistrable;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

public abstract class CRUDManager<T extends CRUD.Identifiable> implements IRegistrable, ILoadable {

  private HashSet<T> set = new HashSet<>();

  /**
   * Get the CRUD object used by this manager. This is used to load and save data.
   *
   * @return The CRUD object used by this manager.
   */
  public abstract CRUD<T> getCRUD();

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
   */
  @Override
  public void load() {
    set = new HashSet<>();
    File mineDir = new File(getCRUD().getPath());
    if (!mineDir.exists()) {
      if (!mineDir.mkdirs()) {
        return;
      }
    }
    File[] files = mineDir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      String fileNameWithoutExtension = file.getName().replace(".json", "");
      Optional<T> data = getCRUD().getData(fileNameWithoutExtension);
      if (data.isEmpty()) {
        log("Failed to load data: " + file.getName() + ".json");
        continue;
      }
      set.add(data.get());
    }
  }

  public boolean loadByID(String id) {
    File file = new File(getCRUD().getPath() + id + ".json");
    if (!file.exists()) {
      return false;
    }
    Optional<T> data = getCRUD().getData(id);
    if (data.isEmpty()) {
      log("Failed to load data: " + file.getName() + ".json");
      return false;
    }
    getByID(id).ifPresent(set::remove);
    set.add(data.get());
    return true;
  }

  /**
   * Retrieve a data object by its ID.
   *
   * @param id The ID of the data object to retrieve.
   * @return An {@link Optional} containing the data object with the specified ID, or an empty
   *     {@link Optional} if no such data object exists.
   */
  public Optional<T> getByID(String id, boolean createNew) {
    for (T data : set) {
      if (data.getId().equals(id)) {
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
    if (set.contains(data) || getByID(data.getId()).isPresent()) {
      return false;
    }
    set.add(data);
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
    if (!set.contains(data) || getByID(data.getId()).isEmpty()) {
      return false;
    }
    set.remove(data);
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
    if (getByID(data.getId()).isEmpty()) {
      add(data, updateFile);
      return true;
    }
    set.remove(getByID(data.getId()).get());
    set.add(data);
    if (updateFile) {
      getCRUD().saveData(data);
    }
    return true;
  }
}
