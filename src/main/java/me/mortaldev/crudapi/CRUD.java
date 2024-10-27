package me.mortaldev.crudapi;

import com.google.gson.TypeAdapter;

import java.util.HashMap;
import java.util.Optional;

public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Delete delete = new NormalDelete();
  protected Save save = new GsonSave();
  protected Get get = new GsonGet();

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
  public abstract HashMap<Class<?>, Object> getTypeAdapterHashMap();

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
  protected Optional<T> getData(String id) {
    return get.get(id, getPath(), getClazz(), getTypeAdapterHashMap());
  }

  /**
   * Deletes the given object from the data storage.
   *
   * @param object the object to delete
   * @return true if the object was successfully deleted, false otherwise
   */
  public boolean deleteData(T object) {
    return delete.delete(object, getPath());
  }

  /**
   * Saves the given object to the data storage.
   *
   * @param object the object to save
   */
  public void saveData(T object) {
    save.save(object, getPath(), getTypeAdapterHashMap());
  }

  public interface Identifiable {
    /**
     * Returns the unique identifier for this object.
     *
     * @return the unique identifier for this object
     */
    String getID();
  }
}
