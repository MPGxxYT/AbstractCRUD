package me.mortaldev.crudapi;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

public abstract class CRUDManager<T extends CRUD.Identifiable> {

  public abstract CRUD<T> getCRUD();

  public abstract void log(String string);

  private HashSet<T> set = new HashSet<>();

  public void load() {
    set = new HashSet<>();
    File mineDir = new File(getCRUD().getPath());
    if (!mineDir.exists()) {
      if (!mineDir.mkdirs()) {
        return;
      }
    }
    File[] files = mineDir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      String fileNameWithoutExtension = file.getName().replace(".json", "");
      Optional<T> data = getCRUD().getData(fileNameWithoutExtension);
      if (data.isEmpty()) {
        log("Failed to load data: " + file.getName() + ".json");
        continue;
      }
      set.add(data.get());
    }
  }

  public Optional<T> getByID(String id) {
    for (T data : set) {
      if (data.getID().equals(id)) {
        return Optional.of(data);
      }
    }
    return Optional.empty();
  }

  public Optional<T> getByID(T data) {
    return getByID(data.getID());
  }

  public boolean contains(T data) {
    return set.contains(data) || getByID(data.getID()).isPresent();
  }

  public HashSet<T> getSet() {
    if (set.isEmpty()) {
      load();
    }
    return set;
  }

  public synchronized boolean add(T data) {
    if (set.contains(data) || getByID(data.getID()).isPresent()) {
      return false;
    }
    set.add(data);
    getCRUD().saveData(data);
    return true;
  }

  public synchronized boolean remove(T data) {
    if (!set.contains(data) || getByID(data.getID()).isEmpty()) {
      return false;
    }
    set.remove(data);
    getCRUD().deleteData(data);
    return true;
  }

  public synchronized boolean update(T data) {
    if (getByID(data.getID()).isEmpty()) {
      return false;
    }
    set.remove(getByID(data.getID()).get());
    set.add(data);
    getCRUD().saveData(data);
    return true;
  }

}
