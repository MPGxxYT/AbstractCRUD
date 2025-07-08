package me.mortaldev.crudapi;

import com.google.gson.TypeAdapter;
import me.mortaldev.crudapi.interfaces.Handler;

import java.util.HashMap;
import java.util.Optional;

public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Handler handler;

  /**
   * Returns the class of the objects that this CRUD system manages.
   *
   * @return the class of the objects that this CRUD system manages
   */
  public abstract Class<T> getClazz();

  /**
   * Returns a {@link HashMap} containing {@link TypeAdapter}s for
   * custom serialization and deserialization.
   *
   * @return a {@link HashMap} containing {@link TypeAdapter}s
   */
  public abstract CRUDAdapters getCRUDAdapters();

  /**
   * Returns the path to the directory where the data files for this CRUD
   * system are stored.
   *
   * @return the path to the directory where the data files for this CRUD
   * system are stored
   */
  public abstract String getPath();

  /**
   * Returns the data associated with the given ID from the data storage.
   *
   * @param id the ID of the data to retrieve
   * @return an {@link Optional} containing the data associated with the given ID,
   * or an empty {@link Optional} if no data with the given ID exists
   */
  public Optional<T> getData(String id) {
    return handler.get().get(id, getPath(), getClazz(), getCRUDAdapters());
  }

  /**
   * Deletes the given object from the data storage.
   *
   * @param object the object to delete
   * @return true if the object was successfully deleted, false otherwise
   */
  public boolean deleteData(T object) {
    return handler.delete().delete(object.getId(), getPath());
  }

  public CRUD(Handler handler) {
    this.handler = handler;
  }

  /**
   * Saves the given object to the data storage.
   *
   * @param object the object to save
   */
  public void saveData(T object) {
    handler.save().save(object, object.getId(), getPath(), getCRUDAdapters());
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
