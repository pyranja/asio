package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.jaxrs.Mime;
import com.google.common.base.Supplier;
import com.hp.hpl.jena.n3.N3TurtleJenaWriter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.xmloutput.impl.Basic;
import org.openjena.riot.system.JenaWriterRdfJson;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

/**
 * Write jena models as http response entities.
 */
@Provider
@Produces(MediaType.WILDCARD)
@Consumes(MediaType.WILDCARD)
public final class JenaModelWriter implements MessageBodyWriter<Model> {
  private final TypeMatchingResolver<RDFWriter> serializer;

  public JenaModelWriter() {
    serializer = TypeMatchingResolver.<RDFWriter>builder()
        .register(Mime.GRAPH_XML.type(), new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new Basic();
          }
        })
        .alias(MediaType.APPLICATION_XML_TYPE)
        .register(Mime.GRAPH_JSON.type(), new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new JenaWriterRdfJson();
          }
        })
        .alias(MediaType.APPLICATION_JSON_TYPE)
        .register(Mime.GRAPH_TURTLE.type(), new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new N3TurtleJenaWriter();
          }
        })
        .alias(MediaType.TEXT_PLAIN_TYPE)
        .make();
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
                             final Annotation[] annotations, final MediaType mediaType) {
    return Model.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final Model model,
                      final Class<?> type, final Type genericType,
                      final Annotation[] annotations, final MediaType mediaType,
                      final MultivaluedMap<String, Object> headers, final OutputStream entity)
      throws IOException, WebApplicationException {
    final TypeMatchingResolver.Selection<RDFWriter> selection =
        serializer.select(Collections.singleton(mediaType));
    headers.putSingle(HttpHeaders.CONTENT_TYPE, selection.type());
    selection.value().write(model, entity, null);
  }

  /**
   * deprecated in JAX-RS 2.0
   */
  @Override
  public long getSize(final Model model,
                      final Class<?> type, final Type genericType, final Annotation[] annotations,
                      final MediaType mediaType) {
    return -1;
  }
}
