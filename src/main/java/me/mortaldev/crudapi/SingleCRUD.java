package me.mortaldev.crudapi;

import me.mortaldev.crudapi.interfaces.Handler;

import java.util.logging.Logger;

public abstract class SingleCRUD<T> implements CRUD.Identifiable {

  protected Handler handler;
  private T object;

  public T get() {
    if (object == null) {
      load();
    }
    return object;
  }

  public void load() {
    Logger.getLogger("CRUD").info("Loading: " + getClazz() + "," + getID());
    this.object =
        handler.get().get(getID(), getPath(), getClazz(), getCRUDAdapters()).orElse(construct());
  }

  public abstract T construct();

  public abstract String getPath();

  public abstract CRUDAdapters getCRUDAdapters();

  public abstract Class<T> getClazz();

  private void save() {
    handler.save().save(object, getID(), getPath(), getCRUDAdapters());
  }

  public SingleCRUD(Handler handler) {
    this.handler = handler;
  }

  public void save(T object) {
    this.object = object;
    save();
  }

  public boolean delete() {
    return handler.delete().delete(getID(), getPath());
  }
}
