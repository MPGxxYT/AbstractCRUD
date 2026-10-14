package me.mortaldev.crudapi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;

/**
 * An in-memory cache of entities in front of a {@link CRUD}. Every change goes to both the cache
 * and the CRUD, so the two stay in step.
 *
 * <pre>{@code
 * CRUD<Profile> crud = new CRUD<>(Profile.class, dataDir.resolve("profiles"), codec);
 * CRUDManager<Profile> profiles = new CRUDManager<>(crud, Profile::new, registry);
 * registry.initialize();             // calls profiles.load()
 * Profile p = profiles.getOrCreate(uuid.toString());
 * }</pre>
 *
 * <p>Subclass it to add lookups of your own; {@link #crud()} gives subclasses the underlying CRUD.
 *
 * <p><b>Thread safety:</b> reads go straight to a {@link ConcurrentHashMap} and never block.
 * Writes ({@link #getOrCreate} when it creates, {@link #save}, {@link #remove}, {@link #load},
 * {@link #reload}) are synchronized on the manager. {@link #load} swaps the contents in without
 * ever leaving the cache empty.
 *
 * @param <T> the entity type
 */
public class CRUDManager<T extends CRUD.Identifiable> implements ILoadable {
  private static final Logger LOGGER = Logger.getLogger("AbstractCRUD");

  private final Map<String, T> cache = new ConcurrentHashMap<>();
  private final Collection<T> view = Collections.unmodifiableCollection(cache.values());
  private final CRUD<T> crud;
  private final Function<String, ? extends T> factory;

  /**
   * @param crud where entities are persisted
   * @param factory creates a new entity for an id; used by {@link #getOrCreate}
   * @param registry registers this manager for {@link CRUDRegistry#initialize()}; may be null
   */
  public CRUDManager(CRUD<T> crud, Function<String, ? extends T> factory, CRUDRegistry registry) {
    this.crud = Objects.requireNonNull(crud, "crud");
    this.factory = Objects.requireNonNull(factory, "factory");
    if (registry != null) {
      registry.register(this);
    }
  }

  protected CRUD<T> crud() {
    return crud;
  }

  /** Returns the cached entity with this id. Does not read from disk. */
  public Optional<T> get(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(cache.get(id));
  }

  /**
   * Returns the cached entity with this id, or creates one with the factory, caches it and saves
   * it.
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
   * Inserts or replaces the entity in the cache and writes it to the CRUD.
   *
   * @return {@code true} if the file was written. The cache is updated either way.
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
   * Removes the entity from the cache and deletes its file.
   *
   * @return {@code true} if the entity was cached or had a file
   */
  public synchronized boolean remove(String id) {
    if (id == null) {
      return false;
    }
    boolean cached = cache.remove(id) != null;
    boolean deleted = crud.delete(id);
    return cached || deleted;
  }

  public boolean contains(String id) {
    return id != null && cache.containsKey(id);
  }

  /**
   * A read-only live view of every cached entity. Safe to iterate while other threads write; the
   * iteration may or may not reflect those writes.
   */
  public Collection<T> all() {
    return view;
  }

  /** Replaces the cache with every entity the CRUD holds. */
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
   * Re-reads one entity from the CRUD into the cache.
   *
   * @return {@code true} if it was read; if not, the cache is left unchanged
   */
  public synchronized boolean reload(String id) {
    Optional<T> loaded = crud.get(id);
    loaded.ifPresent(entity -> cache.put(id, entity));
    return loaded.isPresent();
  }
}
