package me.mortaldev.crudapi.interfaces;

import me.mortaldev.crudapi.CRUDAdapters;

import java.util.Optional;

public interface Get {
  <T> Optional<T> get(String id, String path, Class<T> clazz, CRUDAdapters crudAdapters);
}
