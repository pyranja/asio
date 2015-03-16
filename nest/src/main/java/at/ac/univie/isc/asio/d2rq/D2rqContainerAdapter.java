package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.container.ConfigStore;
import at.ac.univie.isc.asio.container.ContainerAdapter;
import at.ac.univie.isc.asio.container.ContainerSettings;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import org.d2rq.lang.D2RQMappingVisitor;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.openjena.riot.Lang;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Read a raw d2rq mapping model and decompose it into {@code ContainerSettings} and
 * a context-less {@code mapping}. The created mapping will only retain the d2rq mapping directives
 * from the source, but not contextual information like the jdbc connection settings.
 */
@NotThreadSafe
public final class D2rqContainerAdapter implements ContainerAdapter {
  /**
   * Adapt the given d2rq configuration model.
   *
   * @param configuration raw d2rq configuration
   * @return adapter using given configuration model
   */
  public static D2rqContainerAdapter from(@Nonnull final Model configuration) {
    return new D2rqContainerAdapter(D2rqSpec.wrap(configuration));
  }

  /**
   * Parse the given raw mapping and adapt it.
   *
   * @param raw binary representation of RDF formatted d2rq mapping
   * @return adapter using given d2rq configuration
   */
  public static D2rqContainerAdapter load(@Nonnull final ByteSource raw) {
    final Model parsed = LoadD2rqModel.inferBaseUri().parse(raw);
    return new D2rqContainerAdapter(D2rqSpec.wrap(parsed));
  }

  private final D2rqSpec spec;

  private D2rqContainerAdapter(@Nonnull final D2rqSpec spec) {
    this.spec = spec;
  }

  @Override
  public ContainerSettings translate(@Nonnull final Schema schema, @Nonnull final ConfigStore store) {
    final ContainerSettings settings = ContainerSettings.of(schema);
    final URI mapping = store.save(schema.name(), "mapping.ttl", cleanedMapping());
    populate(settings, mapping);
    return settings;
  }

  /**
   * read all settings from the d2rq config
   */
  private void populate(final ContainerSettings settings, final URI mappingLocation) {
    settings.setIdentifier(spec.getBaseResourceIri());
    if (spec.getSparqlTimeout().isDefined()) {
      settings.setTimeout(spec.getSparqlTimeout().getAs(TimeUnit.MILLISECONDS, -1L));
    }
    final ContainerSettings.Sparql sparql = new ContainerSettings.Sparql();
    sparql.setD2rBaseUri(URI.create(spec.getBaseResourceIri()));
    sparql.setD2rMappingLocation(mappingLocation);
    settings.setSparql(sparql);
    final ContainerSettings.Datasource datasource =
        FindJdbcConfig.create().parse(spec.getMapping());
    settings.setDatasource(datasource);
  }

  /**
   * dump the mapping as RDF data, omit the jdbc settings
   */
  private ByteSource cleanedMapping() {
    final Model cleanModel = spec.getContextFreeModel();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    cleanModel.write(sink, Lang.TURTLE.getName(), spec.getBaseResourceIri());
    return ByteSource.wrap(sink.toByteArray());
  }

  /**
   * Extract jdbc settings from a d2rq mapping. Expect a single jdbc configuration block.
   */
  static final class FindJdbcConfig extends D2RQMappingVisitor.Default {
    ContainerSettings.Datasource datasource;

    private FindJdbcConfig() {
    }

    static FindJdbcConfig create() {
      return new FindJdbcConfig();
    }

    ContainerSettings.Datasource parse(final Mapping mapping) {
      mapping.accept(this);
      if (datasource == null) {
        throw new IllegalArgumentException("illegal d2rq mapping : found no jdbc configuration");
      }
      return datasource;
    }

    @Override
    public void visit(final Database database) {
      if (datasource != null) {
        throw new IllegalArgumentException("illegal d2rq mapping : found multiple database configuration blocks");
      }
      this.datasource = new ContainerSettings.Datasource();
      this.datasource.setJdbcUrl(database.getJdbcURL());
      this.datasource.setPassword(database.getPassword());
      this.datasource.setUsername(database.getUsername());
    }
  }
}
