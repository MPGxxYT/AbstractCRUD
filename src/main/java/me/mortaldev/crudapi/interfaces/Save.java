package me.mortaldev.crudapi.interfaces;

import me.mortaldev.crudapi.CRUDAdapters;

public interface Save {
  <T> void save(T object, String id, String path, CRUDAdapters crudAdapters);
}
