/**
 * Save Java objects as JSON files, one file per object, and keep them in memory.
 *
 * <p>Pick the class for what you store:
 *
 * <ul>
 *   <li>{@link me.mortaldev.abstractcrud.crudapi.SingleCRUD}: one object for the whole plugin,
 *       such as settings.
 *   <li>{@link me.mortaldev.abstractcrud.crudapi.CRUDManager}: many objects of one class, each with
 *       an id, such as player profiles.
 *   <li>{@link me.mortaldev.abstractcrud.crudapi.CRUD}: the files themselves, with nothing kept in
 *       memory. The other two are built on it.
 * </ul>
 *
 * <p>Every CRUD needs a {@link me.mortaldev.abstractcrud.crudapi.codec.Codec} from the {@code
 * codec} package, which turns objects into JSON. A {@link
 * me.mortaldev.abstractcrud.crudapi.loading.CRUDRegistry} from the {@code loading} package loads
 * everything at startup.
 *
 * <pre>{@code
 * Codec codec = new GsonCodec();
 * CRUDRegistry registry = new CRUDRegistry();
 * Path data = getDataFolder().toPath();
 *
 * settings = new SingleCRUD<>(new CRUD<>(Settings.class, data, codec), "settings", Settings::new, registry);
 * profiles = new CRUDManager<>(new CRUD<>(Profile.class, data.resolve("profiles"), codec), Profile::new, registry);
 * registry.initialize();
 * }</pre>
 *
 * <p>Nothing in this package throws because of a bad file or a failed write. Problems are logged
 * under the {@code "AbstractCRUD"} logger and reported through return values: an empty {@link
 * java.util.Optional} or {@code false}.
 *
 * @see <a href="https://github.com/MPGxxYT/AbstractCRUD/blob/master/docs/quick-start.md">Quick
 *     start</a>
 */
package me.mortaldev.abstractcrud.crudapi;
