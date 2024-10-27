package me.mortaldev.crudapi;

import java.util.HashMap;
import java.util.Optional;

public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Delete delete = new NormalDelete();
  protected Save save = new GsonSave();
  protected Get get = new GsonGet();

  public abstract Class<T> getClazz();

  public abstract HashMap<Class<?>, Object> getTypeAdapterHashMap();

  public abstract String getPath();

  protected Optional<T> getData(String id) {
    return get.get(id, getPath(), getClazz(), getTypeAdapterHashMap());
  }

  public boolean deleteData(T object) {
    return delete.delete(object, getPath());
  }

  public void saveData(T object) {
    save.save(object, getPath(), getTypeAdapterHashMap());
  }

  public interface Identifiable {
    String getID();
  }
}
