package me.mortaldev.crud;

import java.io.File;
import java.text.MessageFormat;
import me.mortaldev.jbtutorial.Main;
import me.mortaldev.jbtutorial.modules.crud.AbstractCRUD;
import me.mortaldev.jbtutorial.modules.crud.IDelete;

public class NormalDelete implements IDelete {
  private static final String DATA_DELETED = "Data Deleted: {0}";

  @Override
  public <T extends AbstractCRUD.Identifiable> void delete(T object, String path) {
    File filePath = new File(path + object.getID() + ".json");
    if (filePath.exists()) {
      if (filePath.delete()) {
        String message = MessageFormat.format(DATA_DELETED, object.getID());
        Main.log(message);
      } else {
        Main.log("Failed to delete: " + object.getID() + " as the file could not be deleted.");
      }
    } else {
      Main.log("Could not delete data: '" + object.getID() + "' does not exist.");
    }
  }
}
