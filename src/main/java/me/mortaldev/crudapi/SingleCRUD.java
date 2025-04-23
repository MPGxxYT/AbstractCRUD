package me.mortaldev.crudapi;

import me.mortaldev.crudapi.interfaces.Handler;

import java.util.Optional;
import java.util.logging.Logger;

public abstract class SingleCRUD<T> implements CRUD.Identifiable {

  protected Handler handler;
  private T object;

  public T get() {
    return object == null ? construct() : object;
  }

  public void load() {
    Logger.getLogger("CRUD").info("Loading: " + getClazz());
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

  public void save(T newObject) {
    this.object = newObject;
    save();
  }

  public boolean delete() {
    return handler.delete().delete(getID(), getPath());
  }
}
