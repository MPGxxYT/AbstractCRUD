/**
 * Converting objects to and from JSON.
 *
 * <p>Choose {@link me.mortaldev.abstractcrud.crudapi.codec.JacksonCodec} or {@link
 * me.mortaldev.abstractcrud.crudapi.codec.GsonCodec}, add any custom serializers in its
 * constructor, and pass the same instance to every CRUD. The two libraries read your classes
 * differently; each codec's documentation lists the rules.
 *
 * @see <a href="https://github.com/MPGxxYT/AbstractCRUD/blob/master/docs/serialization.md">Serialization
 *     guide</a>
 */
package me.mortaldev.abstractcrud.crudapi.codec;
