package me.mortaldev.crudapi;

import me.mortaldev.crudapi.interfaces.Handler;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;
import me.mortaldev.crudapi.loading.IRegistrable;

public abstract class SingleCRUD<T> implements CRUD.Identifiable, IRegistrable, ILoadable {

  protected Handler handler;
  private T object;

  public T get() {
    return object == null ? construct() : object;
  }

  @Override
  public void load() {
    this.object = handler.get().get(getId(), getPath(), getClazz(), getCombinedAdapters()).orElse(construct());
  }

  public abstract T construct();

  public abstract String getPath();

  public abstract CRUDAdapters getCRUDAdapters();

  public abstract Class<T> getClazz();

  /**
   * Returns a new {@link CRUDAdapters} instance containing both the global adapters from {@link
   * CRUDRegistry} and the specific adapters for this CRUD implementation.
   *
   * @return A combined {@link CRUDAdapters} instance.
   */
  protected CRUDAdapters getCombinedAdapters() {
    return getCRUDAdapters().mergeWith(CRUDRegistry.getInstance().getGlobalAdapters());
  }

  private void save() {
    handler.save().save(object, getId(), getPath(), getCombinedAdapters());
  }

  public SingleCRUD(Handler handler) {
    this.handler = handler;
  }

  public void save(T newObject) {
    this.object = newObject;
    save();
  }

  public boolean delete() {
    return handler.delete().delete(getId(), getPath());
  }
}
