package me.mortaldev.abstractcrud.crudapi;

import java.util.Objects;
import java.util.function.Supplier;
import me.mortaldev.abstractcrud.crudapi.loading.CRUDRegistry;
import me.mortaldev.abstractcrud.crudapi.loading.ILoadable;

/**
 * Keeps one object in memory, such as plugin settings or a spawn point, saved in one file through
 * a {@link CRUD}.
 *
 * <pre>{@code
 * // onEnable
 * CRUD<Settings> files = new CRUD<>(Settings.class, getDataFolder().toPath(), codec);
 * SingleCRUD<Settings> settings = new SingleCRUD<>(files, "settings", Settings::new, registry);
 * registry.initialize();               // reads settings.json, or uses the default
 *
 * // anywhere later
 * Settings s = settings.get();
 * s.motd = "Welcome back";
 * settings.save(s);                    // writes settings.json
 * }</pre>
 *
 * <p>The stored class does not need to implement {@link CRUD.Identifiable}; the id given to the
 * constructor is the file name.
 *
 * <h2>The default object</h2>
 *
 * <p>When the file does not exist, or exists but cannot be read, {@link #get()} returns a default
 * object created by the supplier given to the constructor. The default lives in memory only until
 * {@link #save} is called. Because an unreadable file also produces the default, a {@link #save}
 * after a failed read replaces that file. The failed read is logged as SEVERE under the {@code
 * "AbstractCRUD"} logger.
 *
 * <h2>Threads</h2>
 *
 * <p>All methods are safe from any thread. {@link #get()} does not block once the object is
 * loaded; the other methods are {@code synchronized} on this object. Disk access happens on the
 * calling thread.
 *
 * @param <T> the class of the stored object
 * @see CRUDManager
 * @see CRUDRegistry
 */
public class SingleCRUD<T> implements ILoadable {
  private final CRUD<T> crud;
  private final String id;
  private final Supplier<? extends T> defaults;
  private volatile T object;

  /**
   * Creates a SingleCRUD. Nothing is read until {@link #get()}, {@link #load()} or {@link
   * CRUDRegistry#initialize()} is called.
   *
   * @param crud the CRUD whose folder holds the file. Several SingleCRUDs can share a folder as
   *     long as their ids differ, but don't use a folder that a {@link CRUDManager} reads.
   * @param id the file name without {@code .json}, for example {@code "settings"} for {@code
   *     settings.json}. It must follow the id rules described on {@link CRUD}.
   * @param defaults creates the object used when there is no readable file. It must not return
   *     {@code null}. {@code Settings::new} needs a no-argument constructor; a lambda such as
   *     {@code () -> new Settings("Welcome")} can call any constructor.
   * @param registry the registry to add this to, so {@link CRUDRegistry#initialize()} loads it; or
   *     {@code null} to load on the first {@link #get()}
   * @throws NullPointerException if {@code crud}, {@code id} or {@code defaults} is {@code null}
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

  /**
   * Returns the file name this object is stored under.
   *
   * @return the id given to the constructor, without {@code .json}
   */
  public String id() {
    return id;
  }

  /**
   * Returns the object in memory, loading it first if this is the first call.
   *
   * <p>The first call reads the file, or creates the default if there is no readable file. Later
   * calls return the same object without reading the file again, until {@link #load()} or {@link
   * #save} replaces it. Changes you make to the returned object stay in memory until you call
   * {@link #save}.
   *
   * @return the stored object or the default, never {@code null}
   * @throws NullPointerException if the default is needed and the supplier returns {@code null}
   */
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

  /**
   * Reads the file again and replaces the object in memory. If there is no readable file, the
   * object in memory becomes a new default instead.
   *
   * <p>{@link CRUDRegistry#initialize()} calls this when the SingleCRUD is registered. Call it
   * yourself to pick up changes made to the file by hand, for example from a reload command.
   *
   * @throws NullPointerException if the default is needed and the supplier returns {@code null}
   */
  @Override
  public synchronized void load() {
    T loaded = crud.get(id).orElse(null);
    if (loaded == null) {
      loaded = Objects.requireNonNull(defaults.get(), "defaults returned null");
    }
    object = loaded;
  }

  /**
   * Replaces the object in memory and writes it to the file.
   *
   * <p>Pass the object from {@link #get()} after changing it, or a new object to replace it.
   *
   * @param newObject the object to keep and save
   * @return {@code true} if the file was written. {@code false} if writing failed; the object in
   *     memory is replaced either way, and the cause is logged by the CRUD.
   * @throws NullPointerException if {@code newObject} is {@code null}
   */
  public synchronized boolean save(T newObject) {
    Objects.requireNonNull(newObject, "newObject");
    object = newObject;
    return crud.save(id, newObject);
  }

  /**
   * Deletes the file. The object in memory is kept, so {@link #get()} still returns it until the
   * next {@link #load()}, which then creates a new default.
   *
   * @return {@code true} if the file existed and was deleted
   */
  public synchronized boolean delete() {
    return crud.delete(id);
  }
}
