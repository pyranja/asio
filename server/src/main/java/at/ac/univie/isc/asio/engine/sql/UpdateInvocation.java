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

import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.tool.Closer;
import com.google.common.base.Charsets;

import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

final class UpdateInvocation extends SqlInvocation {
  interface ModCountWriter {
    void serialize(OutputStream output, String statement, int modCount) throws IOException;
  }

  private final ModCountWriter writer;

  private int rowCount = -1;

  public UpdateInvocation(final JdbcExecution jdbc, final String sql, final ModCountWriter writer,
                          final MediaType contentType) {
    super(jdbc, sql, contentType, Permission.INVOKE_UPDATE);
    this.writer = writer;
  }

  @Override
  public void execute() {
    rowCount = jdbc.update(sql);
  }

  @Override
  public void write(final OutputStream sink) throws IOException {
    writer.serialize(sink, sql, rowCount);
  }

  @Override
  public void cancel() {
    try (final Invocation me = this) {
      jdbc.cancel();
    }
  }

  @Override
  public void close() {
    jdbc.close();
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
        Closer.quietly(xml, Closer.xmlStreamWriter());
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
