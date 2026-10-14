package me.mortaldev.crudapi;

import java.nio.file.Path;
import me.mortaldev.crudapi.codec.Codec;
import me.mortaldev.crudapi.loading.CRUDRegistry;
import me.mortaldev.crudapi.loading.ILoadable;

/** Persists a single object, such as plugin settings, as one JSON file. */
public abstract class SingleCRUD<T> implements CRUD.Identifiable, ILoadable {

  private final Codec codec;
  protected CRUDRegistry crudRegistry;
  private CRUD<T> crud;
  private T object;

  public SingleCRUD(Codec codec, CRUDRegistry crudRegistry) {
    this.codec = codec;
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
    this.object = crud().get(getId()).orElse(construct());
  }

  public abstract T construct();

  public abstract String getPath();

  public abstract Class<T> getClazz();

  private CRUD<T> crud() {
    if (crud == null) {
      crud = new CRUD<>(getClazz(), Path.of(getPath()), codec);
    }
    return crud;
  }

  public void save(T newObject) {
    this.object = newObject;
    crud().save(getId(), object);
  }

  public boolean delete() {
    return crud().delete(getId());
  }
}
