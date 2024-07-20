package me.mortaldev.crudapi;

import java.util.Optional;

public abstract class CRUD<T extends CRUD.Identifiable> {
  protected Delete delete = new NormalDelete();
  protected Save save = new GsonSave();
  protected Get get = new GsonGet();

  public abstract String getPath();

  protected Optional<T> getData(String id, Class<T> clazz) {
    return get.get(id, getPath(), clazz);
  }

  public boolean deleteData(T object) {
    return delete.delete(object, getPath());
  }

  public void saveData(T object) {
    save.save(object, getPath());
  }

  public interface Identifiable {
    String getID();
  }
}
