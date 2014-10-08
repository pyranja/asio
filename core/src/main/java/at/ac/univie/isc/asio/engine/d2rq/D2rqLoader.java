package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.config.DatasourceSpec;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import org.d2rq.CompiledMapping;
import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.CompiledD2RQMapping;
import org.d2rq.lang.D2RQCompiler;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;
import org.d2rq.validation.Report;
import org.openjena.atlas.AtlasException;
import org.openjena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.MalformedInputException;
import java.util.Locale;

/**
 * Provide functionality of D2Rs {@code SystemLoader}, omitting the server specific code and
 * dependencies.
 */
public class D2rqLoader {
  private static final Logger log = LoggerFactory.getLogger(D2rqLoader.class);

  /**
   * inlined from org.d2rq.ConfigLoader
   */
  public static String toAbsolutePath(final String path) {
    String fileName = path;
    // Permit backslashes when using the file: URI scheme under Windows
    // This is not required for the latter File.toURL() call
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      fileName = fileName.replaceAll("\\\\", "/");
    }
    try {
      // Check if it's an absolute URI already - but don't confuse Windows drive letters with URI schemes
      if (fileName.matches("[a-zA-Z0-9]{2,}:.*") && new URI(fileName).isAbsolute()) {
        return fileName;
      }
      return new File(fileName).getAbsoluteFile().toURI().normalize()
          .toString();
    } catch (URISyntaxException ex) {
      throw new D2RQException(ex);
    }
  }

  private final CompiledMapping mapping;
  private final ServerSpec serverSpec;

  public D2rqLoader(final String mappingPath) {
    // order is important
    this.serverSpec = loadServerSpec(mappingPath);
    this.mapping = loadMapping(mappingPath);
  }

  private ServerSpec loadServerSpec(final String path) {
    final Model configModel = readRdfFile(path, null);
    return new ServerSpec(configModel);
  }

  // FIXME : support R2RML syntax
  private CompiledMapping loadMapping(final String path) {
    final Model mappingModel = readRdfFile(path, serverSpec.baseURI());
    final Mapping d2rqMapping = new D2RQReader(mappingModel, serverSpec.baseURI()).getMapping();
    d2rqMapping.configuration().setUseAllOptimizations(true);
    final D2RQCompiler compiler = new D2RQCompiler(d2rqMapping);
    final Report compilationReport = new Report();
    compiler.setReport(compilationReport);
    final CompiledD2RQMapping mapping = compiler.getResult();
    if (log.isDebugEnabled() || compilationReport.hasError()) {
      log.info("D2RQ mapping compilation report : {}", compilationReport.getMessages());
    }
    return mapping;
  }

  /**
   * Create a new D2RQ-based jena model from the configured mapping.
   *
   * @return a new and independent model instance.
   */
  public Model createModel() {
    final GraphD2RQ graph = new GraphD2RQ(mapping);
    return ModelFactory.createModelForGraph(graph);
  }

  /**
   * @return base URI as defined in the D2R config
   */
  public String baseUri() {
    return serverSpec.baseURI();
  }

  public ServerSpec serverSpec() {
    return serverSpec;
  }

  public DatasourceSpec datasourceSpec() {
    // XXX will not work if multiple database bindings are defined in d2r mapping .ttl
    final SQLConnection connection = Iterables.getOnlyElement(mapping.getSQLConnections());
    return DatasourceSpec
        .connectTo(connection.getJdbcURL())
        .with(connection.getJdbcDriverClass())
        .authenticateAs(connection.getUsername(), connection.getPassword());
  }

  /**
   * Read an RDF file into a jena model, defaulting to TURTLE syntax, if file extension is unknown.
   * @param path of mapping file
   * @param baseURI of RDF resource URIs
   * @return parsed RDF model
   */
  private static Model readRdfFile(final String path, final String baseURI) {
    final Model mappingModel;
    final String languageHint =
        FileUtils.guessLang(path, "unknown").equals("unknown")
            ? "TURTLE"
            : null; // null lets jena auto detect format
    try {
        mappingModel = FileManager.get().loadModel(path, baseURI, languageHint);
    } catch (JenaException | AtlasException cause) {
      throw parseError(cause, path);
    }
    return mappingModel;
  }

  /**
   * Wrap a parse error as D2RQException, with special handling for some known errors.
   * @param error that occurred
   * @param path of invalid mapping file
   * @return nothing, always throws
   * @throws org.d2rq.D2RQException always
   */
  private static D2RQException parseError(final Exception error, final String path) {
    String description = error.getMessage();
    if (error.getCause() != null) {
      final Throwable inner = error.getCause();
      if (inner instanceof RiotException) {
        description = error.getCause().getMessage();
      } else if (inner instanceof MalformedInputException) {
        final int inputLength = ((MalformedInputException) inner).getInputLength();
        description = "illegal utf-8 encoding found at byte " +  inputLength;
      }
    }
    final String message = String.format(Locale.ENGLISH, "Error parsing <%s> : %s", path, description);
    throw new D2RQException(message, error, D2RQException.MAPPING_TURTLE_SYNTAX);
  }
}
