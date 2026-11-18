package me.mortaldev.abstractcrud.crudapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.mortaldev.abstractcrud.crudapi.codec.Codec;

/**
 * Reads and writes objects of one class as JSON files in one folder, one file per id.
 *
 * <p>A value saved under the id {@code "steve"} is stored as {@code <directory>/steve.json}. The
 * folder is created the first time it is needed. Use one folder per class: {@link #loadAll()}
 * reads every {@code .json} file in it.
 *
 * <pre>{@code
 * CRUD<Arena> arenas = new CRUD<>(Arena.class, getDataFolder().toPath().resolve("arenas"), codec);
 * arenas.save("spleef", arena);                  // writes arenas/spleef.json
 * Optional<Arena> loaded = arenas.get("spleef"); // reads it back
 * }</pre>
 *
 * <p>A {@code CRUD} keeps nothing in memory: every {@link #get} reads the file again. Wrap it in a
 * {@link CRUDManager} (many objects) or a {@link SingleCRUD} (one object) to keep values in memory.
 *
 * <h2>Writes are atomic</h2>
 *
 * <p>{@link #save} writes the JSON to {@code <id>.json.tmp} and then moves it over {@code
 * <id>.json}. If the server stops during a save, the previous file is still intact. On file
 * systems that cannot move atomically, the move falls back to a plain replace.
 *
 * <h2>Errors are logged, not thrown</h2>
 *
 * <p>No method of this class throws on a bad file or a failed write, so one broken file cannot
 * stop a plugin from enabling. Instead the problem is logged through {@link java.util.logging}
 * under the logger name {@code "AbstractCRUD"}, with the file path and the cause, and the method
 * reports it through its return value:
 *
 * <ul>
 *   <li>{@link #get} returns {@link Optional#empty()} for a missing, unreadable or invalid file.
 *       A missing file is not logged.
 *   <li>{@link #save} and {@link #delete} return {@code false}.
 *   <li>{@link #loadAll()} skips files it cannot read.
 * </ul>
 *
 * <h2>Ids</h2>
 *
 * <p>The id is used as the file name, so it must not be {@code null} or empty, must not be {@code
 * "."} or {@code ".."}, and must not contain {@code /} or {@code \}. Every method logs a warning
 * and returns empty or {@code false} for an invalid id. UUID strings and simple names are safe.
 *
 * <h2>Threads</h2>
 *
 * <p>All methods may be called from any thread. Disk access happens on the calling thread. Saving
 * the same id from two threads at the same time leaves one of the two values in the file.
 *
 * <h2>Subclassing</h2>
 *
 * <p>The public methods are not final, so tests can replace the disk with an in-memory subclass.
 *
 * @param <T> the class of the stored objects. It does not need to implement {@link Identifiable};
 *     the id is passed to each method.
 * @see CRUDManager
 * @see SingleCRUD
 * @see Codec
 */
public class CRUD<T> {
  private static final Logger LOGGER = Logger.getLogger("AbstractCRUD");
  private static final String EXTENSION = ".json";
  private static final String TEMP_SUFFIX = ".tmp";

  private final Class<T> type;
  private final Path directory;
  private final Codec codec;

  /**
   * Creates a CRUD for one class and one folder. Nothing is read or written until a method is
   * called.
   *
   * @param type the class that files are read into, for example {@code Arena.class}
   * @param directory the folder for the files, for example {@code
   *     getDataFolder().toPath().resolve("arenas")}. It does not need to exist yet; it is created
   *     by the first {@link #save} or {@link #loadAll()}.
   * @param codec converts objects to and from JSON. Several CRUDs can share one codec.
   * @throws NullPointerException if any argument is {@code null}
   */
  public CRUD(Class<T> type, Path directory, Codec codec) {
    this.type = Objects.requireNonNull(type, "type");
    this.directory = Objects.requireNonNull(directory, "directory");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  /**
   * Returns the class that files are read into.
   *
   * @return the class given to the constructor
   */
  public Class<T> type() {
    return type;
  }

  /**
   * Returns the folder the files are stored in.
   *
   * @return the folder given to the constructor; it may not exist yet
   */
  public Path directory() {
    return directory;
  }

  /**
   * Reads the object stored under {@code id} from {@code <directory>/<id>.json}.
   *
   * <p>The file is read on every call; nothing is cached. If the file exists but cannot be read or
   * is not valid JSON for {@link #type()}, the cause is logged as SEVERE.
   *
   * @param id the id the object was saved under
   * @return the object, or {@link Optional#empty()} if the id is invalid, the file does not exist,
   *     cannot be read, is not valid JSON for the class, or contains only {@code null}
   */
  public Optional<T> get(String id) {
    if (!isValidId(id)) {
      return Optional.empty();
    }
    return read(fileFor(id));
  }

  /**
   * Writes {@code value} to {@code <directory>/<id>.json}, replacing the file if it exists.
   *
   * <p>The folder is created if needed. The write is atomic: the previous file stays intact until
   * the new one is complete. On failure the cause is logged as SEVERE and no partial file is left
   * behind.
   *
   * @param id the id to save under; becomes the file name
   * @param value the object to save; {@code null} is rejected with a warning
   * @return {@code true} if the file was written; {@code false} if the id is invalid, the value is
   *     {@code null}, the codec could not convert the value, or the file could not be written
   */
  public boolean save(String id, T value) {
    if (!isValidId(id)) {
      return false;
    }
    if (value == null) {
      LOGGER.warning("Cannot save null value for id '" + id + "' in " + directory);
      return false;
    }
    Path file = fileFor(id);
    Path temp = directory.resolve(id + EXTENSION + TEMP_SUFFIX);
    try {
      String json = codec.encode(value);
      Files.createDirectories(directory);
      Files.writeString(temp, json, StandardCharsets.UTF_8);
      moveIntoPlace(temp, file);
      return true;
    } catch (IOException | RuntimeException e) {
      LOGGER.log(Level.SEVERE, "Failed to save " + file, e);
      deleteQuietly(temp);
      return false;
    }
  }

  /**
   * Deletes {@code <directory>/<id>.json}.
   *
   * @param id the id of the file to delete
   * @return {@code true} if the file existed and was deleted; {@code false} if the id is invalid,
   *     there was no file, or deleting failed (logged as SEVERE)
   */
  public boolean delete(String id) {
    if (!isValidId(id)) {
      return false;
    }
    Path file = fileFor(id);
    try {
      return Files.deleteIfExists(file);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to delete " + file, e);
      return false;
    }
  }

  /**
   * Reads every {@code .json} file in the folder.
   *
   * <p>The folder is created if it does not exist, in which case the result is empty. Files that
   * cannot be read are logged and left out; the rest still load. Subfolders and files with other
   * extensions are ignored.
   *
   * @return a new, modifiable map from id to object, where each id is the file name without {@code
   *     .json}. Changing the map does not change any file.
   */
  public Map<String, T> loadAll() {
    Map<String, T> values = new LinkedHashMap<>();
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to create directory " + directory, e);
      return values;
    }
    try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + EXTENSION)) {
      for (Path file : files) {
        if (!Files.isRegularFile(file)) {
          continue;
        }
        String name = file.getFileName().toString();
        String id = name.substring(0, name.length() - EXTENSION.length());
        read(file).ifPresent(value -> values.put(id, value));
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to list " + directory, e);
    }
    return values;
  }

  private Optional<T> read(Path file) {
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      T value = codec.decode(Files.readString(file, StandardCharsets.UTF_8), type);
      if (value == null) {
        LOGGER.warning("File contains no value: " + file);
      }
      return Optional.ofNullable(value);
    } catch (IOException | RuntimeException e) {
      LOGGER.log(Level.SEVERE, "Failed to read " + file, e);
      return Optional.empty();
    }
  }

  private Path fileFor(String id) {
    return directory.resolve(id + EXTENSION);
  }

  private boolean isValidId(String id) {
    if (id == null
        || id.isEmpty()
        || id.equals(".")
        || id.equals("..")
        || id.indexOf('/') >= 0
        || id.indexOf('\\') >= 0) {
      LOGGER.warning("Invalid id '" + id + "' for " + directory);
      return false;
    }
    return true;
  }

  private static void moveIntoPlace(Path temp, Path file) throws IOException {
    try {
      Files.move(
          temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void deleteQuietly(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to delete temporary file " + file, e);
    }
  }

  /**
   * An object that knows its own id. Classes stored in a {@link CRUDManager} must implement it.
   *
   * <pre>{@code
   * public class Profile implements CRUD.Identifiable {
   *     private String id;
   *
   *     private Profile() {}                        // for the JSON library
   *     public Profile(String id) { this.id = id; } // for CRUDManager.getOrCreate
   *
   *     public String getId() { return id; }        // from Identifiable
   * }
   * }</pre>
   *
   * <p>The id must be saved with the object (a field, as above), so it is still there after the
   * object is read back from its file.
   */
  public interface Identifiable {
    /**
     * Returns this object's id. It is the key in {@link CRUDManager} and the file name on disk.
     *
     * <p>It must follow the id rules described on {@link CRUD}: not {@code null} or empty, not
     * {@code "."} or {@code ".."}, and no {@code /} or {@code \}. It must not change after the
     * object is saved, or the object and its file no longer match.
     *
     * @return the id, never {@code null} for a saved object
     */
    String getId();
  }
}
