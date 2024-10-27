package me.mortaldev.crudapi;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

public abstract class CRUDManager<T extends CRUD.Identifiable> {

  private HashSet<T> set = new HashSet<>();

  /**
   * Get the CRUD object used by this manager. This is used to load and save data.
   *
   * @return The CRUD object used by this manager.
   */
  public abstract CRUD<T> getCRUD();

  /**
   * Log a message to the console. This is used for logging messages to the
   * console from within this manager.
   *
   * @param string The message to log.
   */
  public abstract void log(String string);

  /**
   * Loads all data from the directory specified by {@link #getCRUD()}'s
   * {@link CRUD#getData(String)} method. If a file in the directory fails to
   * load, a message is logged using the {@link #log(String)} method.
   */
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

  /**
   * Retrieve a data object by its ID.
   *
   * @param id The ID of the data object to retrieve.
   * @return An {@link Optional} containing the data object with the specified
   *     ID, or an empty {@link Optional} if no such data object exists.
   */
  public Optional<T> getByID(String id) {
    for (T data : set) {
      if (data.getID().equals(id)) {
        return Optional.of(data);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieve a data object by its ID, using the ID from the given data object.
   *
   * @param data The data object containing the ID of the data object to retrieve.
   * @return An {@link Optional} containing the data object with the specified
   *     ID, or an empty {@link Optional} if no such data object exists.
   * @see #getByID(String)
   */
  public Optional<T> getByID(T data) {
    return getByID(data.getID());
  }

  /**
   * Determine if the given data object is in the collection.
   *
   * @param data The data object to check for.
   * @return True if the given data object is in the collection, false
   *     otherwise.
   */
  public boolean contains(T data) {
    return set.contains(data) || getByID(data.getID()).isPresent();
  }

  /**
   * Returns a {@link HashSet} containing all data objects in the collection.
   * <p>
   * If the collection has not been loaded yet, this method will call
   * {@link #load()} to load the data objects.
   * <p>
   * The returned set is a defensive copy of the actual collection, so
   * modifications to the returned set will not affect the actual collection.
   * @return A {@link HashSet} containing all data objects in the collection.
   */
  public HashSet<T> getSet() {
    if (set.isEmpty()) {
      load();
    }
    return set;
  }

  /**
   * Add a data object to the collection.
   * <p>
   * If the data object is already in the collection, or if a data object with
   * the same ID is already in the collection, this method will return false.
   * Otherwise, the data object will be added to the collection and the
   * underlying data store will be updated.
   *
   * @param data The data object to add to the collection.
   * @return True if the data object was successfully added to the collection,
   *     false otherwise.
   */
  public synchronized boolean add(T data) {
    if (set.contains(data) || getByID(data.getID()).isPresent()) {
      return false;
    }
    set.add(data);
    getCRUD().saveData(data);
    return true;
  }

  /**
   * Removes a data object from the collection.
   * <p>
   * If the data object is not in the collection, or if a data object with the
   * same ID is not in the collection, this method will return false. Otherwise,
   * the data object will be removed from the collection and the underlying data
   * store will be updated.
   *
   * @param data The data object to remove from the collection.
   * @return True if the data object was successfully removed from the
   *     collection, false otherwise.
   */
  public synchronized boolean remove(T data) {
    if (!set.contains(data) || getByID(data.getID()).isEmpty()) {
      return false;
    }
    set.remove(data);
    getCRUD().deleteData(data);
    return true;
  }

  /**
   * Updates a data object in the collection.
   * <p>
   * If the data object does not exist in the collection, this method will return
   * false. Otherwise, the data object will be replaced in the collection and the
   * underlying data store will be updated.
   *
   * @param data The data object to update in the collection.
   * @return True if the data object was successfully updated, false otherwise.
   */
  public synchronized boolean update(T data) {
    if (getByID(data.getID()).isEmpty()) {
      return false;
    }
    set.remove(getByID(data.getID()).get());
    set.add(data);
    getCRUD().saveData(data);
    return true;
  }

}
