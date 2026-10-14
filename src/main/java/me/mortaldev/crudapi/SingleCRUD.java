package me.mortaldev.crudapi;

import me.mortaldev.crudapi.interfaces.Handler;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;
import me.mortaldev.crudapi.loading.IRegistrable;

/**
 * Persists a single object, such as plugin settings, as one JSON file.
 *
 * <p>Most of this class changes in 2.0.0. Members marked for removal are replaced; see
 * https://github.com/MPGxxYT/AbstractCRUD for the new usage.
 */
public abstract class SingleCRUD<T> implements CRUD.Identifiable, IRegistrable, ILoadable {

  protected Handler handler;
  protected CRUDRegistry crudRegistry;
  private T object;

  @Deprecated(since = "1.1.0", forRemoval = true)
  public SingleCRUD(Handler handler, CRUDRegistry crudRegistry) {
    this.handler = handler;
    this.crudRegistry = crudRegistry;
  }

  public T get() {
    if (object == null) {
      load();
    }
    return object;
  }

  @Override
  public void load() {
    this.object = handler.get().get(getId(), getPath(), getClazz(), getCombinedAdapters()).orElse(construct());
  }

  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract T construct();

  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract String getPath();

  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract CRUDAdapters getCRUDAdapters();

  @Deprecated(since = "1.1.0", forRemoval = true)
  public abstract Class<T> getClazz();

  /**
   * Returns a new {@link CRUDAdapters} instance containing both the global adapters from {@link
   * CRUDRegistry} and the specific adapters for this CRUD implementation.
   *
   * @return A combined {@link CRUDAdapters} instance.
   */
  @Deprecated(since = "1.1.0", forRemoval = true)
  protected CRUDAdapters getCombinedAdapters() {
    return getCRUDAdapters().mergeWith(crudRegistry.getGlobalAdapters());
  }

  private void save() {
    handler.save().save(object, getId(), getPath(), getCombinedAdapters());
  }

  public void save(T newObject) {
    this.object = newObject;
    save();
  }

  public boolean delete() {
    return handler.delete().delete(getId(), getPath());
  }
}
