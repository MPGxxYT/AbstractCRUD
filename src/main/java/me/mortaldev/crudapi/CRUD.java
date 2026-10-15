package me.mortaldev.crudapi;

import com.google.gson.TypeAdapter;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.interfaces.Handler;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Persists one entity type as JSON files in a directory.
 *
 * <p>Most of this class changes in 2.0.0. Members marked for removal are replaced; see
 * https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Handler handler;
  private CRUDRegistry registry;

  @Deprecated(since = "1.1.0", forRemoval = true)
  public CRUD(Handler handler, CRUDRegistry registry) {
    this.handler = handler;
    this.registry = registry;
  }

  /**
   * Returns the class of the objects that this CRUD system manages.
   *
   * @return the class of the objects that this CRUD system manages
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract Class<T> getClazz();

  /**
   * Returns a {@link HashMap} containing {@link TypeAdapter}s for
   * custom serialization and deserialization.
   *
   * @return a {@link HashMap} containing {@link TypeAdapter}s
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract CRUDAdapters getCRUDAdapters();

  /**
   * Returns the path to the directory where the data files for this CRUD
   * system are stored.
   *
   * @return the path to the directory where the data files for this CRUD
   * system are stored
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract String getPath();

  /**
   * Returns a new {@link CRUDAdapters} instance containing both the global adapters from {@link
   * CRUDRegistry} and the specific adapters for this CRUD implementation.
   *
   * @return A combined {@link CRUDAdapters} instance.
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  protected CRUDAdapters getCombinedAdapters() {
    if (registry == null) {
      Logger.getLogger("CRUD").severe("Registry not found in CRUD! Must set the setRegistry() before trying.");
      throw new NullPointerException();
    }
    return getCRUDAdapters().mergeWith(registry.getGlobalAdapters());
  }

  /**
   * Returns the data associated with the given ID from the data storage.
   *
   * @param id the ID of the data to retrieve
   * @return an {@link Optional} containing the data associated with the given ID,
   * or an empty {@link Optional} if no data with the given ID exists
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public Optional<T> getData(String id) {
    return handler.get().get(id, getPath(), getClazz(), getCombinedAdapters());
  }

  /**
   * Deletes the given object from the data storage.
   *
   * @param object the object to delete
   * @return true if the object was successfully deleted, false otherwise
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public boolean deleteData(T object) {
    return handler.delete().delete(object.getId(), getPath());
  }

  @Deprecated(since = "1.1.0", forRemoval = true)
  public CRUD(Handler handler) {
    this.handler = handler;
  }

  /**
   * Saves the given object to the data storage.
   *
   * @param object the object to save
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  public void saveData(T object) {
    handler.save().save(object, object.getId(), getPath(), getCombinedAdapters());
  }

  public interface Identifiable {
    /**
     * Returns the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    String getId();
  }
}
