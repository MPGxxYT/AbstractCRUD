package me.mortaldev.crudapi.operations.delete;

import me.mortaldev.crudapi.interfaces.Delete;

import java.io.File;
import java.text.MessageFormat;

public class NormalDelete implements Delete {
  private static final String DATA_DELETED = "Data Deleted: {0}";

  @Override
  public boolean delete(String id, String path) {
    File filePath = new File(path + id + ".json");
    if (filePath.exists()) {
      if (filePath.delete()) {
        String message = MessageFormat.format(DATA_DELETED, id);
        System.out.println(message);
        return true;
      } else {
        System.out.println(
            "Failed to delete: " + id + " as the file could not be deleted.");
        return false;
      }
    } else {
      System.out.println("Could not delete data: '" + id + "' does not exist.");
      return false;
    }
  }
}
