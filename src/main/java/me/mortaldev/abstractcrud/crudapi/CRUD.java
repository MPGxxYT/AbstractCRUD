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
 * Stores values of one type as JSON files in one directory, one file per id: {@code
 * <directory>/<id>.json}.
 *
 * <p>Writes are atomic: the JSON goes to {@code <id>.json.tmp} first and is then moved over the
 * target, so an interrupted save never leaves a half-written file.
 *
 * <p>Failures are logged, not thrown. Reads of a missing, unreadable or corrupt file return
 * {@link Optional#empty()}; {@link #save} and {@link #delete} return {@code false}.
 *
 * <p>Ids must be non-empty and must not contain path separators or be {@code "."} or {@code
 * ".."}.
 *
 * @param <T> the stored type
 */
public class CRUD<T> {
  private static final Logger LOGGER = Logger.getLogger("AbstractCRUD");
  private static final String EXTENSION = ".json";
  private static final String TEMP_SUFFIX = ".tmp";

  private final Class<T> type;
  private final Path directory;
  private final Codec codec;

  /**
   * @param type the class files are decoded into
   * @param directory where the files go; created on first save or {@link #loadAll()}
   * @param codec converts values to and from JSON
   */
  public CRUD(Class<T> type, Path directory, Codec codec) {
    this.type = Objects.requireNonNull(type, "type");
    this.directory = Objects.requireNonNull(directory, "directory");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  public Class<T> type() {
    return type;
  }

  public Path directory() {
    return directory;
  }

  /** Reads the value stored under {@code id}. */
  public Optional<T> get(String id) {
    if (!isValidId(id)) {
      return Optional.empty();
    }
    return read(fileFor(id));
  }

  /**
   * Writes {@code value} under {@code id}, replacing any existing file.
   *
   * @return {@code true} if the file was written
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
   * Deletes the file stored under {@code id}.
   *
   * @return {@code true} if a file existed and was deleted
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
   * Reads every {@code .json} file in the directory, creating the directory if it is missing.
   * Files that cannot be read or parsed are logged and skipped.
   *
   * @return values keyed by the id taken from each file name
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

  /** An entity that knows its own id. {@link CRUDManager} caches entities by this id. */
  public interface Identifiable {
    /**
     * Returns the unique identifier for this object. It becomes the file name, so it must follow
     * the id rules in {@link CRUD}.
     */
    String getId();
  }
}
