package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import com.google.common.base.Supplier;
import com.hp.hpl.jena.n3.N3TurtleJenaWriter;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.sparql.resultset.CSVOutput;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import com.hp.hpl.jena.xmloutput.impl.Basic;
import org.openjena.riot.system.JenaWriterRdfJson;

import javax.ws.rs.core.MediaType;
import java.util.List;

final class HandlerFactory {
  // result set formats
  public static final MediaType RESULTS_XML_TYPE =
      MediaType.valueOf("application/sparql-results+xml");
  public static final MediaType RESULTS_JSON_TYPE =
      MediaType.valueOf("application/sparql-results+json");
  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");
  // graph formats
  public static final MediaType GRAPH_XML_TYPE = MediaType.valueOf("application/rdf+xml");
  public static final MediaType GRAPH_JSON_TYPE = MediaType.valueOf("application/rdf+json");
  public static final MediaType GRAPH_TURTLE_TYPE = MediaType.valueOf("text/turtle");

  private final TypeMatchingResolver<OutputFormatter> resultFormatterRegistry;
  private final TypeMatchingResolver<RDFWriter> graphFormatterRegistry;

  public HandlerFactory() {
    resultFormatterRegistry = TypeMatchingResolver.<OutputFormatter>builder()
        .register(RESULTS_XML_TYPE, new Supplier<OutputFormatter>() {
          @Override
          public OutputFormatter get() {
            return new XMLOutput();
          }
        })
        .alias(MediaType.APPLICATION_XML_TYPE)
        .register(RESULTS_JSON_TYPE, new Supplier<OutputFormatter>() {
          @Override
          public OutputFormatter get() {
            return new JSONOutput();
          }
        })
        .alias(MediaType.APPLICATION_JSON_TYPE)
        .register(CSV_TYPE, new Supplier<OutputFormatter>() {
          @Override
          public OutputFormatter get() {
            return new CSVOutput();
          }
        })
        .make();
    graphFormatterRegistry = TypeMatchingResolver.<RDFWriter>builder()
        .register(GRAPH_XML_TYPE, new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new Basic();
          }
        })
        .alias(MediaType.APPLICATION_XML_TYPE)
        .register(GRAPH_JSON_TYPE, new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new JenaWriterRdfJson();
          }
        })
        .alias(MediaType.APPLICATION_JSON_TYPE)
        .register(GRAPH_TURTLE_TYPE, new Supplier<RDFWriter>() {
          @Override
          public RDFWriter get() {
            return new N3TurtleJenaWriter();
          }
        })
        .alias(MediaType.TEXT_PLAIN_TYPE)
        .make();
  }

  public JenaQueryHandler select(final int queryType, final List<MediaType> acceptable) {
    switch (queryType) {
      case Query.QueryTypeAsk:
        return handleAsk(acceptable);
      case Query.QueryTypeSelect:
        return handleSelect(acceptable);
      case Query.QueryTypeConstruct:
        return handleConstruct(acceptable);
      case Query.QueryTypeDescribe:
        return handleDescribe(acceptable);
      default:
        throw new JenaEngine.UnknownQueryType();
    }
  }

  private AskHandler handleAsk(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<OutputFormatter> selection =
        resultFormatterRegistry.select(acceptable);
    return new AskHandler(selection.value(), selection.type());
  }

  private JenaQueryHandler handleSelect(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<OutputFormatter> selection =
        resultFormatterRegistry.select(acceptable);
    return new SelectHandler(selection.value(), selection.type());
  }

  private JenaQueryHandler handleDescribe(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<RDFWriter> selection =
        graphFormatterRegistry.select(acceptable);
    return new DescribeHandler(selection.value(), selection.type());
  }

  private JenaQueryHandler handleConstruct(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<RDFWriter> selection =
        graphFormatterRegistry.select(acceptable);
    return new ConstructHandler(selection.value(), selection.type());
  }
}
