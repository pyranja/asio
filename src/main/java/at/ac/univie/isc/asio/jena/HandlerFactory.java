package at.ac.univie.isc.asio.jena;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.RDFWriterF;
import com.hp.hpl.jena.rdf.model.impl.RDFWriterFImpl;
import com.hp.hpl.jena.sparql.resultset.CSVOutput;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import org.openjena.riot.Lang;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

final class HandlerFactory<SERIALIZER> {

  public static JenaQueryHandler select(final int queryType, final List<MediaType> acceptable) {
    final HandlerFactory factory = FACTORIES.get(queryType);
    if (factory == null) {
      throw new JenaConnector.UnknownQueryType();
    } else {
      return factory.select(acceptable);
    }
  }

  private final Map<MediaType, SERIALIZER> mappings;
  private final Function<SERIALIZER, ? extends JenaQueryHandler> provider;

  HandlerFactory(final Map<MediaType, SERIALIZER> mappings, final Function<SERIALIZER, ? extends JenaQueryHandler> provider) {
    this.mappings = mappings;
    this.provider = provider;
  }

  public final JenaQueryHandler select(final List<MediaType> accepted) {
    for (MediaType candidate : accepted) {
      for (Map.Entry<MediaType, SERIALIZER> mapping : this.mappings.entrySet()) {
        if (mapping.getKey().isCompatible(candidate)) {
          return provider.apply(mapping.getValue());
        }
      }
    }
    throw new JenaConnector.NoSupportedFormat(accepted, mappings.keySet());
  }

  // ======================== HACK : add N-TRIPLE writer name alias to support Lang enum
  static {
    // FIXME : move to spring config initialization ?
    final RDFWriterF writerFactory = new RDFWriterFImpl();
    final RDFWriter nTripleWriter = writerFactory.getWriter("N-TRIPLE");
    RDFWriterFImpl.setBaseWriterClassName(
        Lang.NTRIPLES.getName(),
        nTripleWriter.getClass().getName());
  }

  // ======================== MIME -> HandlerParameter mappings
  // !! order matters - mimes higher up take precedence, e.g. when matching */* !!

  private static final Map<MediaType, Lang> GRAPH_FORMAT_MAPPING = ImmutableMap
      .<MediaType, Lang>builder()
      .put(MediaType.APPLICATION_XML_TYPE, Lang.RDFXML) // default
      .put(MediaType.TEXT_XML_TYPE, Lang.RDFXML)
      .put(MediaType.valueOf(Lang.RDFXML.getContentType()), Lang.RDFXML)
      .put(MediaType.APPLICATION_JSON_TYPE, Lang.RDFJSON)
      .put(MediaType.valueOf(Lang.RDFJSON.getContentType()), Lang.RDFJSON)
      .put(MediaType.valueOf(Lang.TURTLE.getContentType()), Lang.TURTLE)
      .put(MediaType.TEXT_PLAIN_TYPE, Lang.NTRIPLES)
      .build();

  public static final FormatterAndMediaType XML_RESULTS_FORMATTER =
      new FormatterAndMediaType(new XMLOutput(), MediaType.valueOf("application/sparql-results+xml"));
  public static final FormatterAndMediaType JSON_RESULTS_FORMATTER =
      new FormatterAndMediaType(new JSONOutput(), MediaType.valueOf("application/sparql-results+json"));
  public static final FormatterAndMediaType CSV_RESULTS_FORMATTER =
      new FormatterAndMediaType(new CSVOutput(), MediaType.valueOf("text/csv"));

  private static final Map<MediaType, FormatterAndMediaType> RESULT_FORMAT_MAPPING = ImmutableMap
      .<MediaType, FormatterAndMediaType>builder()
      .put(MediaType.APPLICATION_XML_TYPE, XML_RESULTS_FORMATTER) // default
      .put(MediaType.TEXT_XML_TYPE, XML_RESULTS_FORMATTER)
      .put(XML_RESULTS_FORMATTER.format, XML_RESULTS_FORMATTER)
      .put(MediaType.APPLICATION_JSON_TYPE, JSON_RESULTS_FORMATTER)
      .put(JSON_RESULTS_FORMATTER.format, JSON_RESULTS_FORMATTER)
      .put(CSV_RESULTS_FORMATTER.format, CSV_RESULTS_FORMATTER)
      .build();

  // ======================== Handler constructor functions

  private static final Function<Lang, ConstructHandler> CONSTRUCT_PROVIDER =
      new Function<Lang, ConstructHandler>() {
        @Nullable
        @Override
        public ConstructHandler apply(@Nullable final Lang input) {
          return new ConstructHandler(input);
        }
      };

  private static final Function<Lang, DescribeHandler> DESCRIBE_PROVIDER =
      new Function<Lang, DescribeHandler>() {
        @Nullable
        @Override
        public DescribeHandler apply(@Nullable final Lang input) {
          return new DescribeHandler(input);
        }
      };

  private static final Function<FormatterAndMediaType, SelectHandler> SELECT_PROVIDER =
      new Function<FormatterAndMediaType, SelectHandler>() {
        @Override
        public SelectHandler apply(final FormatterAndMediaType input) {
          return new SelectHandler(input.formatter, input.format);
        }
      };

  private static final Function<FormatterAndMediaType, AskHandler> ASK_PROVIDER =
      new Function<FormatterAndMediaType, AskHandler>() {
        @Override
        public AskHandler apply(final FormatterAndMediaType input) {
          return new AskHandler(input.formatter, input.format);
        }
      };

  private static final Map<Integer, HandlerFactory> FACTORIES = ImmutableMap
      .<Integer, HandlerFactory>builder()
      .put(Query.QueryTypeSelect, new HandlerFactory(RESULT_FORMAT_MAPPING, SELECT_PROVIDER))
      .put(Query.QueryTypeAsk, new HandlerFactory(RESULT_FORMAT_MAPPING, ASK_PROVIDER))
      .put(Query.QueryTypeConstruct, new HandlerFactory(GRAPH_FORMAT_MAPPING, CONSTRUCT_PROVIDER))
      .put(Query.QueryTypeDescribe, new HandlerFactory(GRAPH_FORMAT_MAPPING, DESCRIBE_PROVIDER))
      .build();

  /** struct */
  private static final class FormatterAndMediaType {
    public final OutputFormatter formatter;
    public final MediaType format;
    private FormatterAndMediaType(final OutputFormatter formatter, final MediaType format) {
      this.formatter = formatter;
      this.format = format;
    }
  }
}
