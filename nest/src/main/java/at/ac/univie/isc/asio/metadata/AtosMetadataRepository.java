package at.ac.univie.isc.asio.metadata;

import net.atos.AtosDataset;
import net.atos.AtosMessage;
import net.atos.AtosResourceMetadata;
import net.atos.AtosResourceMetadataList;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Connect to the atos vph-metadata repository to perform CRUD on dataset metadata.
 */
public final class AtosMetadataRepository {
  private final WebTarget endpoint;

  public AtosMetadataRepository(final WebTarget atosService) {
    endpoint = atosService;
  }

  /**
   * Find metadata on the dataset with given {@code globalID}.
   *
   * @param globalIdentifier identifier of the target dataset
   * @return metadata on target dataset if present
   */
  public Observable<AtosDataset> findOne(final String globalIdentifier) {
    return checkedRequest(new Callable<AtosDataset>() {
      @Override
      public AtosDataset call() throws Exception {
        final Response response = endpoint
            .path("/metadata/{identifier}")
            .resolveTemplate("identifier", globalIdentifier)
            .request(MediaType.APPLICATION_XML_TYPE)
            .get();
        switch (response.getStatus()) {
          case HttpStatus.SC_OK:
            return response.readEntity(AtosResourceMetadata.class).getDataset();
          case HttpStatus.SC_INTERNAL_SERVER_ERROR:
          case HttpStatus.SC_NOT_FOUND:
            return null;
          default:
            throw new WebApplicationException("unexpected http response code", response.getStatus());
        }
      }
    });
  }

  /**
   * Find metadata on the datasets with given {@code localID}.
   *
   * @param localIdentifier identifier of dataset
   * @return metadata on target dataset if present
   */
  public Observable<AtosDataset> findByLocalId(final String localIdentifier) {
    return checkedRequest(new Callable<List<AtosResourceMetadata>>() {
      @Override
      public List<AtosResourceMetadata> call() throws Exception {
        final AtosResourceMetadataList resources = endpoint
            .path("/metadata/facets/Dataset/localID")
            .queryParam("value", localIdentifier)
            .request(MediaType.APPLICATION_XML_TYPE)
            .get(AtosResourceMetadataList.class);
        return resources.getResourceMetadata();
      }
    }).flatMap(new Func1<List<AtosResourceMetadata>, Observable<AtosResourceMetadata>>() {
      @Override
      public Observable<AtosResourceMetadata> call(final List<AtosResourceMetadata> resourceList) {
        return Observable.from(resourceList);
      }
    }).map(new Func1<AtosResourceMetadata, AtosDataset>() {
      @Override
      public AtosDataset call(final AtosResourceMetadata resource) {
        return resource.getDataset();
      }
    }).filter(new Func1<AtosDataset, Boolean>() {
      @Override
      public Boolean call(final AtosDataset atosDataset) {
        return atosDataset != null;
      }
    });
  }

  /**
   * Create a new metadata resource or update an existing one, depending on the globalId value of
   * the given dataset. If the globalID is {@code null} a new resource is created, else the given
   * identifier is used to update an existing resource if possible.
   *
   * @param data new metadata
   * @return new state of metadata resource
   */
  public Observable<AtosDataset> save(final AtosDataset data) {
    final AtosResourceMetadata wrapper = new AtosResourceMetadata().withDataset(data);
    final String identifier = data.getGlobalID();
    final Observable<AtosMessage> execution = (identifier == null) ?
        performCreate(wrapper) :
        performUpdate(identifier, wrapper);
    return execution.map(new Func1<AtosMessage, String>() {
      @Override
      public String call(final AtosMessage message) {
        return message.getData().getGlobalId();
      }
    }).flatMap(new Func1<String, Observable<AtosDataset>>() {
      @Override
      public Observable<AtosDataset> call(final String s) {
        return findOne(s);
      }
    });
  }

  /**
   * Create dataset as a new resource with auto-generated id
   */
  private Observable<AtosMessage> performCreate(final AtosResourceMetadata data) {
    return checkedRequest(new Callable<AtosMessage>() {
      @Override
      public AtosMessage call() throws Exception {
        return endpoint
            .path("/metadata")
            .request(MediaType.APPLICATION_XML_TYPE)
            .post(Entity.entity(data, MediaType.APPLICATION_XML_TYPE), AtosMessage.class);
      }
    });
  }

  /**
   * Update an existing metadata resource
   */
  private Observable<AtosMessage> performUpdate(final String identifier, final AtosResourceMetadata data) {
    return checkedRequest(new Callable<AtosMessage>() {
      @Override
      public AtosMessage call() throws Exception {
        return endpoint
            .path("/metadata/{identifier}")
            .resolveTemplate("identifier", identifier)
            .request(MediaType.APPLICATION_XML_TYPE)
            .put(Entity.entity(data, MediaType.APPLICATION_XML_TYPE), AtosMessage.class);
      }
    });
  }

  /**
   * Delete the metadata resource with given {@code globalID}.
   *
   * @param globalIdentifier id of the target resource
   * @return the identifier of the deleted resource
   */
  public Observable<String> delete(final String globalIdentifier) {
    return checkedRequest(new Callable<String>() {
      @Override
      public String call() throws Exception {
        endpoint
            .path("/metadata/{identifier}")
            .resolveTemplate("identifier", globalIdentifier)
            .request(MediaType.APPLICATION_XML_TYPE)
            .delete(AtosMessage.class);
        return globalIdentifier;
      }
    });
  }

  /**
   * perform given interaction with repository and handle errors
   */
  private <TYPE> Observable<TYPE> checkedRequest(final Callable<TYPE> action) {
    return Observable.create(new ReactiveCallable<>(action));
  }

  /**
   * Turn the given {@code Callable} into an {@code Observable}, that may yield zero or a single
   * item.
   *
   * @param <TYPE> type of the single result
   */
  private class ReactiveCallable<TYPE> implements Observable.OnSubscribe<TYPE> {
    private final Callable<TYPE> action;

    public ReactiveCallable(final Callable<TYPE> action) {
      this.action = action;
    }

    @Override
    public void call(final Subscriber<? super TYPE> subscriber) {
      try {
        final TYPE result = action.call();
        if (result != null) {
          subscriber.onNext(result);
        }
        subscriber.onCompleted();
      } catch (Exception error) {
        subscriber.onError(wrapError(error));
      }
    }
  }

  private RuntimeException wrapError(final Exception original) {
    if (original instanceof RepositoryFailure || original instanceof MetadataNotFound) {
      return (RuntimeException) original;
    }
    return new RepositoryFailure(original.getMessage(), endpoint.getUri(), original);
  }

  @Override
  public String toString() {
    return "AtosMetadataRepository{" +
        "endpoint=" + endpoint.getUri() +
        '}';
  }
}
