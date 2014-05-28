package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.frontend.*;
import at.ac.univie.isc.asio.security.Anonymous;
import at.ac.univie.isc.asio.transport.ObservableStreamBodyWriter;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.Mockito;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ApplicationPath("/")
public class MockProtocolApplication extends Application {

  private final Endpoint endpoint;
  private final Engine acceptor;

  public MockProtocolApplication() {
    super();
    final AsyncProcessor processor =
        new ListeningAsyncProcessor(MoreExecutors.sameThreadExecutor(), new VariantConverter());
    final OperationFactory factory =
        new OperationFactory(RandomIdGenerator.withPrefix("integration"));
    final OperationParser parser = new OperationParser(factory);
    final ContentNegotiator negotiator =
        new FormatSelector(Collections.singleton(MockFormat.ALWAYS_APPLICABLE),
            new VariantConverter());
    acceptor = Mockito.mock(Engine.class);
    endpoint =
        new Endpoint(acceptor, parser, negotiator, new VariantConverter(), TimeoutSpec.from(2, TimeUnit.SECONDS)).authorize(Anonymous.INSTANCE,
            EnumSet.allOf(Action.class));
  }

  @Override
  public Set<Object> getSingletons() {
    return ImmutableSet.of(endpoint, new DatasetExceptionMapper(), new ObservableStreamBodyWriter());
  }

  public Engine getAcceptor() {
    return acceptor;
  }

  public void reset() {
    Mockito.reset(acceptor);
  }
}
