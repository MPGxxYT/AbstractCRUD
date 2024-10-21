package me.mortaldev.crudapi;

import java.util.HashMap;
import java.util.Optional;

public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Delete delete = new NormalDelete();
  protected Save save = new GsonSave();
  protected Get get = new GsonGet();

  public abstract String getPath();

  protected Optional<T> getData(String id, Class<T> clazz, HashMap<Class<?>, Object> typeAdapterHashMap) {
    return get.get(id, getPath(), clazz, typeAdapterHashMap);
  }

  protected Optional<T> getData(String id, Class<T> clazz) {
    HashMap<Class<?>, Object> typeAdapterHashMap = new HashMap<>();
    return get.get(id, getPath(), clazz, typeAdapterHashMap);
  }

  public boolean deleteData(T object) {
    return delete.delete(object, getPath());
  }

  public void saveData(T object, HashMap<Class<?>, Object> typeAdapterHashMap) {
    save.save(object, getPath(), typeAdapterHashMap);
  }

  public void saveData(T object) {
    HashMap<Class<?>, Object> typeAdapterHashMap = new HashMap<>();
    save.save(object, getPath(), typeAdapterHashMap);
  }

  public interface Identifiable {
    String getID();
  }
}
