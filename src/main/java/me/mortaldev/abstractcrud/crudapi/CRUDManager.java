package me.mortaldev.abstractcrud.crudapi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import me.mortaldev.abstractcrud.crudapi.loading.CRUDRegistry;
import me.mortaldev.abstractcrud.crudapi.loading.ILoadable;

/**
 * Keeps every object of one class in memory, each saved in its own file through a {@link CRUD}.
 * Use it for player profiles, arenas, shops, or anything you look up by an id.
 *
 * <pre>{@code
 * // onEnable
 * CRUD<Profile> files = new CRUD<>(Profile.class, getDataFolder().toPath().resolve("profiles"), codec);
 * CRUDManager<Profile> profiles = new CRUDManager<>(files, Profile::new, registry);
 * registry.initialize();                       // reads profiles/*.json into memory
 *
 * // anywhere later
 * Profile p = profiles.getOrCreate(uuid.toString());
 * p.setKills(p.getKills() + 1);
 * profiles.save(p);                            // writes profiles/<uuid>.json
 * }</pre>
 *
 * <h2>Memory and disk</h2>
 *
 * <p>After {@link #load()}, every file in the CRUD's folder is in memory. Reads ({@link #get},
 * {@link #contains}, {@link #all}) only look at memory and never touch the disk. Writes ({@link
 * #save}, {@link #remove}, {@link #getOrCreate} when it creates) change memory and the file
 * together.
 *
 * <p>Changing an object you got from this manager changes it in memory only. Call {@link
 * #save(CRUD.Identifiable)} to write the change to its file.
 *
 * <h2>Errors</h2>
 *
 * <p>File problems are logged by the {@link CRUD} and reported through return values, never
 * thrown. The one exception is {@link #getOrCreate} when the factory returns a wrong object.
 *
 * <h2>Threads</h2>
 *
 * <p>Reads never block and are safe from any thread, including async tasks. Writes are {@code
 * synchronized} on this manager, so concurrent writes are applied one at a time. Iterating {@link
 * #all()} while another thread writes never throws. Disk access happens on the thread that calls a
 * write method.
 *
 * <h2>Subclassing</h2>
 *
 * <p>Extend this class to add lookups of your own; {@link #all()} and {@link #crud()} give a
 * subclass what it needs.
 *
 * @param <T> the class of the stored objects; it must implement {@link CRUD.Identifiable}
 * @see SingleCRUD
 * @see CRUDRegistry
 */
public class CRUDManager<T extends CRUD.Identifiable> implements ILoadable {
  private static final Logger LOGGER = Logger.getLogger("AbstractCRUD");

  private final Map<String, T> cache = new ConcurrentHashMap<>();
  private final Collection<T> view = Collections.unmodifiableCollection(cache.values());
  private final CRUD<T> crud;
  private final Function<String, ? extends T> factory;

  /**
   * Creates a manager. Memory starts empty: call {@link #load()}, or {@link
   * CRUDRegistry#initialize()} if you passed a registry, to read the files.
   *
   * @param crud the CRUD whose folder holds this manager's files. Give each manager its own
   *     folder, because {@link #load()} reads every {@code .json} file in it.
   * @param factory creates a new object for an id, used by {@link #getOrCreate}. It receives the
   *     id and must return an object whose {@link CRUD.Identifiable#getId()} equals that id, for
   *     example {@code Profile::new} pointing at a {@code Profile(String id)} constructor.
   * @param registry the registry to add this manager to, so {@link CRUDRegistry#initialize()}
   *     loads it; or {@code null} to load it yourself with {@link #load()}
   * @throws NullPointerException if {@code crud} or {@code factory} is {@code null}
   */
  public CRUDManager(CRUD<T> crud, Function<String, ? extends T> factory, CRUDRegistry registry) {
    this.crud = Objects.requireNonNull(crud, "crud");
    this.factory = Objects.requireNonNull(factory, "factory");
    if (registry != null) {
      registry.register(this);
    }
  }

  /**
   * Returns the CRUD this manager saves through, for subclasses that need direct file access.
   * Writing through it directly bypasses the in-memory copy, so memory and disk can differ until
   * the next {@link #load()} or {@link #reload}.
   *
   * @return the CRUD given to the constructor
   */
  protected CRUD<T> crud() {
    return crud;
  }

  /**
   * Returns the object with this id from memory. Never reads from disk, so it only finds objects
   * that were loaded, created or saved through this manager.
   *
   * @param id the id to look up; {@code null} is allowed and finds nothing
   * @return the object, or {@link Optional#empty()} if none is in memory with this id
   */
  public Optional<T> get(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(cache.get(id));
  }

  /**
   * Returns the object with this id, creating it if there is none.
   *
   * <p>If the id is in memory, that object is returned and nothing is written. Otherwise the
   * factory from the constructor creates a new object, which is put in memory and saved to its
   * file before it is returned. Two threads asking for the same new id get the same object.
   *
   * @param id the id to look up or create; must follow the id rules described on {@link CRUD}
   * @return the existing or newly created object, never {@code null}
   * @throws NullPointerException if {@code id} is {@code null}
   * @throws IllegalStateException if the factory returns {@code null} or an object whose {@code
   *     getId()} differs from {@code id}; nothing is stored in that case
   */
  public T getOrCreate(String id) {
    Objects.requireNonNull(id, "id");
    T existing = cache.get(id);
    if (existing != null) {
      return existing;
    }
    synchronized (this) {
      existing = cache.get(id);
      if (existing != null) {
        return existing;
      }
      T created = factory.apply(id);
      if (created == null || !id.equals(created.getId())) {
        throw new IllegalStateException(
            "Factory returned " + created + " for id '" + id + "'; its getId() must match");
      }
      cache.put(id, created);
      crud.save(id, created);
      return created;
    }
  }

  /**
   * Adds or replaces the object in memory, then writes it to {@code <folder>/<id>.json}.
   *
   * <p>Use it both for new objects and after changing an existing one. The object is stored under
   * its own {@link CRUD.Identifiable#getId()}.
   *
   * @param entity the object to save
   * @return {@code true} if the file was written. {@code false} if {@code entity} or its id is
   *     {@code null} (nothing changes), or if writing the file failed. When writing fails, memory
   *     still holds the new object; the cause is logged by the CRUD.
   */
  public synchronized boolean save(T entity) {
    if (entity == null || entity.getId() == null) {
      LOGGER.warning("Cannot save null entity or entity with null id");
      return false;
    }
    cache.put(entity.getId(), entity);
    return crud.save(entity.getId(), entity);
  }

  /**
   * Removes the object with this id from memory and deletes its file.
   *
   * @param id the id to remove; {@code null} is allowed and removes nothing
   * @return {@code true} if the object was in memory or had a file; {@code false} if neither
   *     existed
   */
  public synchronized boolean remove(String id) {
    if (id == null) {
      return false;
    }
    boolean cached = cache.remove(id) != null;
    boolean deleted = crud.delete(id);
    return cached || deleted;
  }

  /**
   * Checks whether an object with this id is in memory. Does not check the disk.
   *
   * @param id the id to check; {@code null} is allowed
   * @return {@code true} if {@link #get(String)} would find an object
   */
  public boolean contains(String id) {
    return id != null && cache.containsKey(id);
  }

  /**
   * Returns every object in memory.
   *
   * <p>The collection is read-only: adding or removing through it throws {@link
   * UnsupportedOperationException}. It is live, so it reflects later saves and removals without
   * calling this method again. Iterating it while other threads write is safe and never throws; the
   * loop may or may not include changes made during it. Order is not defined.
   *
   * <p>To keep a snapshot that does not change, copy it: {@code List.copyOf(manager.all())}.
   *
   * @return a read-only live view of the objects in memory
   */
  public Collection<T> all() {
    return view;
  }

  /**
   * Reads every file in the CRUD's folder into memory, replacing what was there.
   *
   * <p>Objects whose files no longer exist are dropped from memory. Files that cannot be read are
   * logged and left out. Each object is stored under its own {@link CRUD.Identifiable#getId()}; if
   * that differs from its file name, a warning is logged, and an object with a {@code null} id is
   * skipped. Other threads can keep reading while this runs; they see the old or the new objects,
   * never an empty manager.
   *
   * <p>{@link CRUDRegistry#initialize()} calls this for every registered manager. Call it yourself
   * to reload after editing files by hand, for example from a reload command.
   */
  @Override
  public synchronized void load() {
    Map<String, T> fresh = new HashMap<>();
    crud.loadAll()
        .forEach(
            (fileId, entity) -> {
              String id = entity.getId();
              if (id == null) {
                LOGGER.warning("Skipping entity with null id loaded from '" + fileId + "'");
                return;
              }
              if (!id.equals(fileId)) {
                LOGGER.warning(
                    "Entity in file '" + fileId + "' has id '" + id + "'; caching it as '" + id
                        + "'");
              }
              fresh.put(id, entity);
            });
    cache.putAll(fresh);
    cache.keySet().retainAll(fresh.keySet());
  }

  /**
   * Reads one object's file again and puts it in memory, replacing the copy that was there.
   *
   * @param id the id of the file to read
   * @return {@code true} if the file was read. {@code false} if it is missing or cannot be read; in
   *     that case memory is left unchanged, so a deleted file does not remove the object from
   *     memory. Use {@link #remove} for that.
   */
  public synchronized boolean reload(String id) {
    Optional<T> loaded = crud.get(id);
    loaded.ifPresent(entity -> cache.put(id, entity));
    return loaded.isPresent();
  }
}
