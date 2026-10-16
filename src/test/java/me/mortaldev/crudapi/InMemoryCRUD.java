package me.mortaldev.crudapi;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import me.mortaldev.abstractcrud.crudapi.CRUD;
import me.mortaldev.abstractcrud.crudapi.codec.GsonCodec;

/** A CRUD adapter that keeps values in a map and never touches the disk. */
public class InMemoryCRUD<T> extends CRUD<T> {
  /** The "disk": what a file-backed CRUD would have written. */
  public final Map<String, T> stored = new ConcurrentHashMap<>();

  public int saves;
  public int deletes;

  public InMemoryCRUD(Class<T> type) {
    super(type, Path.of("in-memory"), new GsonCodec());
  }

  @Override
  public Optional<T> get(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(stored.get(id));
  }

  @Override
  public boolean save(String id, T value) {
    saves++;
    stored.put(id, value);
    return true;
  }

  @Override
  public boolean delete(String id) {
    deletes++;
    return stored.remove(id) != null;
  }

  @Override
  public Map<String, T> loadAll() {
    return new LinkedHashMap<>(stored);
  }
}
