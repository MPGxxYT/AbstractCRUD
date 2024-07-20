package me.mortaldev.crudapi;

public interface Save {
  <T extends CRUD.Identifiable> void save(T object, String path);
}
