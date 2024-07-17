package me.mortaldev.crud;

import java.io.File;
import java.text.MessageFormat;

public class NormalDelete implements IDelete {
  private static final String DATA_DELETED = "Data Deleted: {0}";

  @Override
  public <T extends AbstractCRUD.Identifiable> boolean delete(T object, String path) {
    File filePath = new File(path + object.getID() + ".json");
    if (filePath.exists()) {
      if (filePath.delete()) {
        String message = MessageFormat.format(DATA_DELETED, object.getID());
        System.out.println(message);
        return true;
      } else {
        System.out.println(
            "Failed to delete: " + object.getID() + " as the file could not be deleted.");
        return false;
      }
    } else {
      System.out.println("Could not delete data: '" + object.getID() + "' does not exist.");
      return false;
    }
  }
}
