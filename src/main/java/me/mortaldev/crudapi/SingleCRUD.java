package me.mortaldev.crudapi;

import java.util.Objects;
import java.util.function.Supplier;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;

/**
 * Holds a single object, such as plugin settings, stored under a fixed id in a {@link CRUD}.
 *
 * <pre>{@code
 * CRUD<Settings> crud = new CRUD<>(Settings.class, dataDir, codec);
 * SingleCRUD<Settings> settings = new SingleCRUD<>(crud, "settings", Settings::new, registry);
 * settings.get().motd;  // loads settings.json, or uses the default if there is no file
 * }</pre>
 *
 * <p>The default is only kept in memory until {@link #save} is called.
 *
 * @param <T> the stored type
 */
public class SingleCRUD<T> implements ILoadable {
  private final CRUD<T> crud;
  private final String id;
  private final Supplier<? extends T> defaults;
  private volatile T object;

  /**
   * @param crud where the object is persisted
   * @param id the file name, without {@code .json}
   * @param defaults creates the object when no file exists
   * @param registry registers this for {@link CRUDRegistry#initialize()}; may be null
   */
  public SingleCRUD(
      CRUD<T> crud, String id, Supplier<? extends T> defaults, CRUDRegistry registry) {
    this.crud = Objects.requireNonNull(crud, "crud");
    this.id = Objects.requireNonNull(id, "id");
    this.defaults = Objects.requireNonNull(defaults, "defaults");
    if (registry != null) {
      registry.register(this);
    }
  }

  public String id() {
    return id;
  }

  /** Returns the object, loading it on first use. */
  public T get() {
    T current = object;
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (object == null) {
        load();
      }
      return object;
    }
  }

  /** Re-reads the object from the CRUD, or creates the default if there is no readable file. */
  @Override
  public synchronized void load() {
    T loaded = crud.get(id).orElse(null);
    if (loaded == null) {
      loaded = Objects.requireNonNull(defaults.get(), "defaults returned null");
    }
    object = loaded;
  }

  /**
   * Replaces the object and writes it to the CRUD.
   *
   * @return {@code true} if the file was written. The in-memory object is replaced either way.
   */
  public synchronized boolean save(T newObject) {
    Objects.requireNonNull(newObject, "newObject");
    object = newObject;
    return crud.save(id, newObject);
  }

  /**
   * Deletes the file. The next {@link #get()} after a {@link #load()} returns the default.
   *
   * @return {@code true} if a file existed and was deleted
   */
  public synchronized boolean delete() {
    return crud.delete(id);
  }
}
