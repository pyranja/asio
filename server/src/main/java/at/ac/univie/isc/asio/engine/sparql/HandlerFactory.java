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
import java.util.Arrays;
import java.util.List;

public final class HandlerFactory {
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

  /**
   * Choose and create an invocation for the given query and (single) accepted result format.
   * @param query the sparql query
   * @param format of serialized results
   * @return an appropriate invocation
   */
  public SparqlInvocation<?> select(final Query query, final String format) {
    return select(query.getQueryType(), Arrays.asList(MediaType.valueOf(format)));
  }

  public SparqlInvocation select(final int queryType, final List<MediaType> acceptable) {
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

  private AskInvocation handleAsk(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<OutputFormatter> selection =
        resultFormatterRegistry.select(acceptable);
    return new AskInvocation(selection.value(), selection.type());
  }

  private SparqlInvocation handleSelect(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<OutputFormatter> selection =
        resultFormatterRegistry.select(acceptable);
    return new SelectInvocation(selection.value(), selection.type());
  }

  private SparqlInvocation handleDescribe(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<RDFWriter> selection =
        graphFormatterRegistry.select(acceptable);
    return new DescribeInvocation(selection.value(), selection.type());
  }

  private SparqlInvocation handleConstruct(final List<MediaType> acceptable) {
    final TypeMatchingResolver.Selection<RDFWriter> selection =
        graphFormatterRegistry.select(acceptable);
    return new ConstructInvocation(selection.value(), selection.type());
  }
}
