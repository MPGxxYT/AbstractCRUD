package me.mortaldev.crudapi;

import java.util.HashMap;

public abstract class SingleCRUD<T> implements CRUD.Identifiable {
  protected Delete delete = new NormalDelete();
  protected Save save = new GsonSave();
  protected Get get = new GsonGet();
  private T object;

  public T get() {
    if (object == null) {
      log("Failed to load data: " + getID() + ".json");
    }
    return object;
  }

  public void load() {
    object = get.get(getID(), getPath(), getClazz(), getTypeAdapterHashMap()).orElse(null);
  }

  public abstract void log(String string);
  public abstract String getPath();

  public abstract HashMap<Class<?>, Object> getTypeAdapterHashMap();

  public abstract Class<T> getClazz();

  public void save() {
    save.save(object, getID(), getPath(), getTypeAdapterHashMap());
  }

  public boolean delete() {
    return delete.delete(getID(), getPath());
  }
}
