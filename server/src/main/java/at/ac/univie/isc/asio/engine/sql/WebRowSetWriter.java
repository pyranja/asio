/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.tool.Closer;
import com.google.common.base.Charsets;
import org.jooq.Cursor;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.springframework.jdbc.UncategorizedSQLException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Objects;

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
      throw new UncategorizedSQLException("webrowset serialization", statement, e);
    } finally {
      Closer.quietly(xml, Closer.xmlStreamWriter());
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
      tag("command", statement);
      tag("concurrency", ResultSet.CONCUR_UPDATABLE);
      tag("datasource", null);
      tag("escape-processing", Boolean.TRUE);
      tag("fetch-direction", ResultSet.FETCH_FORWARD);
      tag("fetch-size", 0);
      tag("isolation-level", Connection.TRANSACTION_NONE);
      emptyTag("key-columns");
      emptyTag("map");
      tag("max-field-size", 0);
      tag("max-rows", 0);
      tag("query-timeout", 0);
      tag("read-only", Boolean.TRUE);
      tag("rowset-type", "ResultSet.TYPE_SCROLL_INSENSITIVE");  // must be constant name !
      tag("show-deleted", Boolean.FALSE);
      emptyTag("table-name");  // <null /> would represent java null - but is invalid according to schema
      tag("url", null);
      emptySyncProvider();
    xml.writeEndElement();
    // @formatter:on
  }

  private void emptySyncProvider() throws XMLStreamException {
    // @formatter:off
    xml.writeStartElement(WRS, "sync-provider");
      emptyTag("sync-provider-name");
      emptyTag("sync-provider-vendor");
      emptyTag("sync-provider-version");
      emptyTag("sync-provider-grade");
      emptyTag("data-source-lock");
    xml.writeEndElement();
    // @formatter:on
  }

  private void metadata() throws XMLStreamException, SQLException {
    final ResultSetMetaData rsmd = context();
    // @formatter:off
    xml.writeStartElement(WRS, "metadata");
      tag("column-count", rsmd.getColumnCount());
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
      tag("column-index", idx);
      tag("auto-increment", context.isAutoIncrement(idx));
      tag("case-sensitive", context.isCaseSensitive(idx));
      tag("currency", context.isCurrency(idx));
      tag("nullable", context.isNullable(idx));
      tag("signed", context.isSigned(idx));
      tag("searchable", context.isSearchable(idx));
      tag("column-display-size", context.getColumnDisplaySize(idx));
      tag("column-label", context.getColumnLabel(idx));
      tag("column-name", context.getColumnName(idx));
      tag("schema-name", context.getSchemaName(idx));
      tag("column-precision", context.getPrecision(idx));
      tag("column-scale", context.getScale(idx));
      tag("table-name", context.getTableName(idx));
      tag("catalog-name", context.getCatalogName(idx));
      tag("column-type", context.getColumnType(idx));
      tag("column-type-name", context.getColumnTypeName(idx));
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
        emptyTag("null");
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

  private void tag(final String elementName, final Object content) throws XMLStreamException {
    xml.writeStartElement(WRS, elementName);
    final String text = Objects.toString(content);
    if ((content == null || text.isEmpty()) && useNullTag) {
      xml.writeEmptyElement(WRS, "null");
    } else if (!text.isEmpty()) {
      xml.writeCharacters(text);
    } // else { ; }
    xml.writeEndElement();
  }

  private void emptyTag(final String elementName) throws XMLStreamException {
    xml.writeEmptyElement(WRS, elementName);
  }
}
