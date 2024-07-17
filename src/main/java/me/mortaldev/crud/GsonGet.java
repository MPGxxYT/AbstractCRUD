package me.mortaldev.crud;

import java.io.File;
import java.util.Optional;
import me.mortaldev.jbtutorial.Main;
import me.mortaldev.jbtutorial.utils.GSON;

public class GsonGet implements IGet {
  @Override
  public <T> Optional<T> get(String id, String path, Class<T> clazz) {
    File filePath = new File(path + id + ".json");
    if (filePath.exists()) {
      return Optional.ofNullable(GSON.getJsonObject(filePath, clazz));
    } else {
      Main.log("Could not get data: '" + id + "' does not exist.");
      return Optional.empty();
    }
  }
}
