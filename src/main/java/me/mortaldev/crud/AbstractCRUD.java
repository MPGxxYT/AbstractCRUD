package me.mortaldev.crud;

import java.util.Optional;

public abstract class AbstractCRUD<T extends AbstractCRUD.Identifiable> {
  protected IDelete iDelete = new NormalDelete();
  protected ISave iSave = new GsonSave();
  protected IGet iGet = new GsonGet();

  public abstract String getPath();

  protected Optional<T> getData(String id, Class<T> clazz) {
    return iGet.get(id, getPath(), clazz);
  }

  public boolean deleteData(T object) {
    return iDelete.delete(object, getPath());
  }

  public void saveData(T object) {
    iSave.save(object, getPath());
  }

  public interface Identifiable {
    String getID();
  }
}
