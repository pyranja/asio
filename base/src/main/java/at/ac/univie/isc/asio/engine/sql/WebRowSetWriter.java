package at.ac.univie.isc.asio.engine.sql;

import com.google.common.base.Charsets;

import org.jooq.Cursor;
import org.jooq.Record;
import org.jooq.RecordMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Objects;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.tool.Resources;

final class WebRowSetWriter implements SelectInvocation.CursorWriter {

  /* FIXME : conformity switch
   * ? introduce a setting to switch between
   *  a) schema valid serialization with dummy values
   *  b) schema invalid serialization with <null />
   */
  static RecordMapper<Record, String[]> RECORD_MAPPER() {
    final ValuePresenter presenter = ValuePresenter
        .withDefault(Representations.javaString())
        .register(Representations.plainNull(), Void.class)
        .register(Representations.dateTicks(), Timestamp.class, Date.class, Time.class)
        .register(Representations.plainBinary(), byte[].class)
        .build();
    return new StringArrayMapper(presenter);
  }

  private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();

  public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
  public static final String WRS = "http://java.sun.com/xml/ns/jdbc";

  private Cursor<Record> cursor;
  private XMLStreamWriter xml;
  @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
  private boolean useNullTag = true; // FIXME : conformity switch

  @Override
  public void serialize(final OutputStream output, final String statement,
                        final Cursor<Record> cursor) throws IOException {
    try {
      this.cursor = cursor;
      xml = XML_OUTPUT_FACTORY.createXMLStreamWriter(output, Charsets.UTF_8.name());
      prelude();
      properties(statement);
      metadata();
      data();
      postlude();
    } catch (XMLStreamException e) {
      throw new IOException(e);
    } catch (SQLException e) {
      throw new DatasetFailureException(e);
    } finally {
      Resources.close(xml);
    }
  }

  private void prelude() throws XMLStreamException {
    xml.setDefaultNamespace(WRS);
    xml.setPrefix("xsi", XSI);
    xml.writeStartDocument();
    xml.writeStartElement(WRS, "webRowSet");
    xml.writeDefaultNamespace(WRS);
    // FIXME : conformity switch
    //    xml.writeNamespace("xsi", XSI);
    //    xml.writeAttribute(XSI, "schemaLocation",
    //        "http://java.sun.com/xml/ns/jdbc http://java.sun.com/xml/ns/jdbc/webrowset.xsd");
  }

  private void properties(final String statement) throws XMLStreamException {
    // @formatter:off
    xml.writeStartElement(WRS, "properties");
      _("command", statement);
      _("concurrency", ResultSet.CONCUR_UPDATABLE);
      _("datasource", null);
      _("escape-processing", Boolean.TRUE);
      _("fetch-direction", ResultSet.FETCH_FORWARD);
      _("fetch-size", 0);
      _("isolation-level", Connection.TRANSACTION_NONE);
      _("key-columns");
      _("map");
      _("max-field-size", 0);
      _("max-rows", 0);
      _("query-timeout", 0);
      _("read-only", Boolean.TRUE);
      _("rowset-type", "ResultSet.TYPE_SCROLL_INSENSITIVE");  // must be constant name !
      _("show-deleted", Boolean.FALSE);
      _("table-name");  // <null /> would represent java null - but is invalid according to schema
      _("url", null);
      emptySyncProvider();
    xml.writeEndElement();
    // @formatter:on
  }

  private void emptySyncProvider() throws XMLStreamException {
    // @formatter:off
    xml.writeStartElement(WRS, "sync-provider");
      _("sync-provider-name");
      _("sync-provider-vendor");
      _("sync-provider-version");
      _("sync-provider-grade");
      _("data-source-lock");
    xml.writeEndElement();
    // @formatter:on
  }

  private void metadata() throws XMLStreamException, SQLException {
    final ResultSetMetaData rsmd = context();
    // @formatter:off
    xml.writeStartElement(WRS, "metadata");
      _("column-count", rsmd.getColumnCount());
      for (int index = 1; index <= rsmd.getColumnCount(); index++) {
        columnDefinition(index, rsmd);
      }
    xml.writeEndElement();
    // @formatter:on
  }

  private ResultSetMetaData context() throws SQLException {
    final ResultSet rs = cursor.resultSet();
    if (rs == null) {
      throw new JooqEngine.Cancelled();
    }
    return rs.getMetaData();
  }

  private void columnDefinition(final int idx, final ResultSetMetaData context)
      throws XMLStreamException, SQLException {
    // @formatter:off
    xml.writeStartElement(WRS, "column-definition");
      _("column-index", idx);
      _("auto-increment", context.isAutoIncrement(idx));
      _("case-sensitive", context.isCaseSensitive(idx));
      _("currency", context.isCurrency(idx));
      _("nullable", context.isNullable(idx));
      _("signed", context.isSigned(idx));
      _("searchable", context.isSearchable(idx));
      _("column-display-size", context.getColumnDisplaySize(idx));
      _("column-label", context.getColumnLabel(idx));
      _("column-name", context.getColumnName(idx));
      _("schema-name", context.getSchemaName(idx));
      _("column-precision", context.getPrecision(idx));
      _("column-scale", context.getScale(idx));
      _("table-name", context.getTableName(idx));
      _("catalog-name", context.getCatalogName(idx));
      _("column-type", context.getColumnType(idx));
      _("column-type-name", context.getColumnTypeName(idx));
    xml.writeEndElement();
    // @formatter:on
  }

  private void data() throws XMLStreamException {
    final RecordMapper<Record, String[]> mapper = RECORD_MAPPER();
    // @formatter:off
    xml.writeStartElement(WRS, "data");
      while (cursor.hasNext()) {
        final String[] values = cursor.fetchOne(mapper);
        row(values);
      }
    xml.writeEndElement();
    // @formatter:on
  }

  private void row(final String[] values) throws XMLStreamException {
    // @formatter:off
    xml.writeStartElement(WRS, "currentRow");
      for (final String value : values) {
        cell(value);
      }
    xml.writeEndElement();
    // @formatter:on
  }

  private void cell(final String value) throws XMLStreamException {
    // @formatter:off
    xml.writeStartElement(WRS, "columnValue");
      //noinspection StringEquality
      if (value == Representations.NULL_VALUE) {
        // must use <null /> tag to capture nullability
        _("null");
      } else {
        xml.writeCharacters(value);
      }
    xml.writeEndElement();
    // @formatter:on
  }

  private void postlude() throws XMLStreamException {
    xml.writeEndElement();  // </ webRowSet>
    xml.writeEndDocument();
    xml.flush();
  }

  // shortcuts for qualified element writing

  private void _(final String elementName, final Object content) throws XMLStreamException {
    xml.writeStartElement(WRS, elementName);
    final String text = Objects.toString(content);
    if ((content == null || text.isEmpty()) && useNullTag) {
      xml.writeEmptyElement(WRS, "null");
    } else if (!text.isEmpty()) {
      xml.writeCharacters(text);
    } // else { ; }
    xml.writeEndElement();
  }

  private void _(final String elementName) throws XMLStreamException {
    xml.writeEmptyElement(WRS, elementName);
  }
}
