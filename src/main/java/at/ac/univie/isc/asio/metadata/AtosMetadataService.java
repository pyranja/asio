package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.common.Resources;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fetch metadata informations from a remote repository.
 *
 * @author Chris Borckholder
 */
public class AtosMetadataService {

  // WP4 meta data services XXX retrieve from config ?
  private static final String QUERY_PARAM = "value";
  private static final URI QUERY_PATH = URI.create("metadata/facets/dataset/localID");

  private final URI repository;
  private final JAXBContext jaxbContext;

  public AtosMetadataService(final URI repository) {
    this.repository = checkNotNull(repository, "illegal repository URL");
    try {
      jaxbContext = JAXBContext.newInstance(RepositoryResponse.class);
    } catch (final JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  public DatasetMetadata fetchMetadataForId(final String id) {
    final WebClient client = prepareClient(repository, id);
    Response response = null;
    try {
      response = client.get();
      if (notSuccessful(response)) {
        throw failure(client, describe(response), null);
      } else {
        return extractFrom(response, id);
      }
    } catch (final ClientException | IOException | JAXBException cause) {
      throw failure(client, "internal error", cause);
    } catch (final NoSuchElementException cause) {
      throw notFound(client, id, cause);
    } catch (final IllegalArgumentException cause) {
      throw failure(client, "multiple metadata entries found", cause);
    } finally {
      Resources.close(response);
    }
  }

  // TODO inject client
  private WebClient prepareClient(final URI baseURI, final String id) {
    final WebClient client = WebClient.create(baseURI);
    client.path(QUERY_PATH).query(QUERY_PARAM, id).accept(MediaType.APPLICATION_XML);
    return client;
  }

  private boolean notSuccessful(final Response response) {
    return response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL;
  }

  private String describe(final Response response) {
    return String.format("$s : %s", response.getStatus(), response.getStatusInfo());
  }

  private DatasetMetadata extractFrom(final Response response, final String id) throws IOException, JAXBException {
    try (final InputStream body = (InputStream) response.getEntity()) {
      final List<DatasetMetadata> received = parse(body);
      return findSingleMatching(received, id);
    }
  }

  private List<DatasetMetadata> parse(final InputStream body) throws JAXBException {
    final Unmarshaller xml = jaxbContext.createUnmarshaller();
    final RepositoryResponse results =
        xml.unmarshal(new StreamSource(body), RepositoryResponse.class).getValue();
    return results.getDatasets();
  }

  private DatasetMetadata findSingleMatching(final List<DatasetMetadata> received, final String localId) {
    Iterable<DatasetMetadata> matches =
        Iterables.filter(received, new Predicate<DatasetMetadata>() {
          @Override
          public boolean apply(@Nullable final DatasetMetadata input) {
            return Objects.equals(input.getLocalID(), localId);
          }
        });
    return Iterables.getOnlyElement(matches);
  }

  private RepositoryFailure failure(final WebClient client, final String reason, final Exception e) {
    final String message =
        String.format(Locale.ENGLISH, "metadata request to <%s> failed (%s)",
            client.getCurrentURI(), reason);
    return new RepositoryFailure(message, e);
  }

  private MetadataNotFound notFound(final WebClient client, final String id, final NoSuchElementException cause) {
    final String message =
        String.format(Locale.ENGLISH, "metadata for <%s> not found at <%s>", id, client.getCurrentURI());
    return new MetadataNotFound(message, cause);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this)
        .add("repository", repository)
        .toString();
  }
}
