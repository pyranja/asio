package at.ac.univie.isc.asio.jena;

import static at.ac.univie.isc.asio.DatasetOperation.Action.QUERY;

import java.util.Locale;
import java.util.Set;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;

public enum JenaFormats implements SerializationFormat {

  /**
   * official sparql protocol xml format mime
   */
  SPARQL_XML(MediaType.create("application", "sparql-results+xml").withCharset(Charsets.UTF_8),
      ResultsFormat.lookup("xml"), QUERY),
  /**
   * official sparql protocol json format mime
   */
  SPARQL_JSON(MediaType.create("application", "sparql-results+json").withCharset(Charsets.UTF_8),
      ResultsFormat.lookup("json"), QUERY),
  /**
   * sparql protocol xml alias
   */
  XML(MediaType.create("application", "xml").withCharset(Charsets.UTF_8), ResultsFormat
      .lookup("xml"), QUERY),
  /**
   * sparql protocol json alias
   */
  JSON(MediaType.create("application", "json").withCharset(Charsets.UTF_8), ResultsFormat
      .lookup("json"), QUERY),
  /**
   * plain table representation
   */
  CSV(MediaType.create("text", "csv").withCharset(Charsets.UTF_8), ResultsFormat.lookup("csv"),
      QUERY)
  // XXX add TSV
  ;

  private static final Set<SerializationFormat> valueSet = ImmutableSet
      .<SerializationFormat>copyOf(JenaFormats.values());

  public static Set<SerializationFormat> asSet() {
    return valueSet;
  }

  private final MediaType mime;
  private final Set<Action> operations;
  private final ResultsFormat jenaType;

  private JenaFormats(final MediaType mediaType, final ResultsFormat jenaType,
      final Action... operationTypes) {
    mime = mediaType;
    this.jenaType = jenaType;
    operations = ImmutableSet.copyOf(operationTypes);
    validate();
  }

  private void validate() {
    assert (mime != null) : "invalid format " + this + ": no mime type given";
    assert (!operations.isEmpty()) : "invalid format " + this + ": no operations given";
    assert (jenaType != null) : "invalid format " + this + ": no matching jena type found";
  }

  @Override
  public MediaType asMediaType() {
    return mime;
  }

  public ResultsFormat asJenaFormat() {
    return jenaType;
  }

  @Override
  public boolean applicableOn(final Action action) {
    return operations.contains(action);
  }

  @Override
  public final String toString() {
    return String.format(Locale.ENGLISH, "JENA_%s(%s->%s)", name(), mime, jenaType);
  }
}
