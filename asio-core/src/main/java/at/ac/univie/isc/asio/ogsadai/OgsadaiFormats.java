package at.ac.univie.isc.asio.ogsadai;

import static at.ac.univie.isc.asio.DatasetOperation.Action.QUERY;
import static at.ac.univie.isc.asio.DatasetOperation.Action.SCHEMA;
import static at.ac.univie.isc.asio.DatasetOperation.Action.UPDATE;

import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;

/**
 * {@link SerializationFormat}s supported by the {@link OgsadaiEngine}.
 * 
 * @author Chris Borckholder
 */
public enum OgsadaiFormats implements SerializationFormat {

  /**
   * webrowset/tablemetadata representation
   */
  XML(MediaType.create("application", "xml").withCharset(Charsets.UTF_8), QUERY, UPDATE, SCHEMA),
  /**
   * tabular, comma-separated representation
   */
  CSV(MediaType.CSV_UTF_8, QUERY),
  /**
   * simple plain text representation
   */
  PLAIN(MediaType.PLAIN_TEXT_UTF_8, UPDATE);

  private static final Set<SerializationFormat> valueSet = ImmutableSet
      .<SerializationFormat>copyOf(OgsadaiFormats.values());

  public static Set<SerializationFormat> asSet() {
    return valueSet;
  }

  private final MediaType mime;
  private final Set<Action> operations;

  private OgsadaiFormats(final MediaType mediaType, final Action... operationTypes) {
    mime = mediaType;
    operations = ImmutableSet.copyOf(operationTypes);
    validate();
  }

  private void validate() {
    assert (mime != null) : "invalid format " + this + ": no mime type given";
    assert (!operations.isEmpty()) : "invalid format " + this + ": no operations given";
  }

  @Override
  public final MediaType asMediaType() {
    return mime;
  }

  @Override
  public final boolean applicableOn(final Action type) {
    return operations.contains(type);
  }

  @Override
  public final String toString() {
    return String.format("OGSADAI_%s(%s)", name(), mime);
  }
}
