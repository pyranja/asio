package at.ac.univie.isc.asio.engine.sql;

import com.google.common.base.Charsets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.tool.Resources;

final class UpdateInvocation implements Invocation {

  private final ModCountWriter writer;
  private final MediaType contentType;
  private final JdbcContext jdbc;
  private final String sql;

  private int rowCount = -1;

  public UpdateInvocation(final JdbcContext jdbc, final String sql, final ModCountWriter writer,
                          final MediaType contentType) {
    this.writer = writer;
    this.jdbc = jdbc;
    this.sql = sql;
    this.contentType = contentType;
  }

  @Override
  public Role requires() {
    return Role.WRITE;
  }

  @Override
  public void execute() throws DatasetException {
    rowCount = jdbc.update(sql);
  }

  @Override
  public MediaType produces() {
    return contentType;
  }

  @Override
  public void write(final OutputStream sink) throws IOException, DatasetException {
    writer.serialize(sink, sql, rowCount);
  }

  @Override
  public void cancel() throws DatasetException {
    try (final Invocation me = this) {
      jdbc.cancel();
    }
  }

  @Override
  public void close() throws DatasetException {
    jdbc.close();
  }

  interface ModCountWriter {
    void serialize(OutputStream output, String statement, int modCount) throws IOException;
  }

  static class XmlModCountWriter implements ModCountWriter {
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final String ASIO = "http://isc.univie.ac.at/2014/asio";

    @Override
    public void serialize(final OutputStream output, final String statement, final int modCount) throws IOException {
      XMLStreamWriter xml = null;
      try {
        xml = XML_OUTPUT_FACTORY.createXMLStreamWriter(output, Charsets.UTF_8.name());
        xml.setDefaultNamespace(ASIO);
        xml.writeStartDocument();
        xml.writeStartElement(ASIO, "sql");
        xml.writeDefaultNamespace(ASIO);
        xml.writeEmptyElement(ASIO, "head");
        xml.writeAttribute("statement", statement);
        xml.writeEmptyElement(ASIO, "update");
        xml.writeAttribute("affected", Integer.toString(modCount));
        xml.writeEndElement();
        xml.writeEndDocument();
      } catch (XMLStreamException e) {
        throw new IOException(e);
      } finally {
        Resources.close(xml);
      }
    }
  }

  static class CsvModCountWriter implements ModCountWriter {
    private static final String CSV_TEMPLATE = "statement,affected\r\n%s,%d";
    private static final Representation QUOTER = Representations.quotedString();

    @Override
    public void serialize(final OutputStream output, final String statement, final int modCount) throws IOException {
      final String quotedSql = QUOTER.apply(statement);
      final String csv =
          String.format(Locale.ENGLISH, CSV_TEMPLATE, quotedSql, modCount);
      output.write(csv.getBytes(Charsets.UTF_8));
    }
  }
}
