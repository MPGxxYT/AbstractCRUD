package me.mortaldev.crudapi;

import java.util.Optional;

public interface Get {
  <T> Optional<T> get(String id, String path, Class<T> clazz);
}
