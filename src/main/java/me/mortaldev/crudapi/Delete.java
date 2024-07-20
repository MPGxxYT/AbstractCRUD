package me.mortaldev.crudapi;

public interface Delete {
  <T extends CRUD.Identifiable> boolean delete(T object, String path);
}
