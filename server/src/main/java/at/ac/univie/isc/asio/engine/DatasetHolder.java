package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.spring.Holder;
import com.hp.hpl.jena.rdf.model.Model;
import rx.Observable;

/**
 * Proxy a {@link at.ac.univie.isc.asio.Dataset} by forwarding invocations to a delegate instance.
 * Suitable for creating a scoped proxy in spring.
 */
public final class DatasetHolder implements Dataset, Holder<Dataset> {
  private Dataset delegate;

  public void set(final Dataset delegate) {
    this.delegate = delegate;
  }

  @Override
  public String toString() {
    return "DatasetHolder{delegate=" + delegate + '}';
  }

  // === proxy implementation

  private Dataset delegate() {
    if (delegate == null) { throw new IllegalStateException("no delegate instance set"); }
    return delegate;
  }

  @Override
  public Id name() {
    return delegate().name();
  }

  @Override
  public Observable<SchemaDescriptor> metadata() {
    return delegate().metadata();
  }

  @Override
  public Observable<SqlSchema> definition() {
    return delegate().definition();
  }

  @Override
  public Observable<Model> mapping() {
    return delegate().mapping();
  }
}
