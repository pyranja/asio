package at.ac.univie.isc.asio.protocol;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mockito.Mockito;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.coordination.OperationAcceptor;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.ContentNegotiator;
import at.ac.univie.isc.asio.frontend.DatasetExceptionMapper;
import at.ac.univie.isc.asio.frontend.FormatSelector;
import at.ac.univie.isc.asio.frontend.OperationFactory;
import at.ac.univie.isc.asio.frontend.VariantConverter;
import at.ac.univie.isc.asio.security.Anonymous;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

@ApplicationPath("/")
public class MockProtocolApplication extends Application {

  private final Endpoint endpoint;
  private final OperationAcceptor acceptor;

  public MockProtocolApplication() {
    super();
    final AsyncProcessor processor =
        new AsyncProcessor(MoreExecutors.sameThreadExecutor(), new VariantConverter());
    final OperationFactory factory =
        new OperationFactory(RandomIdGenerator.withPrefix("integration"));
    final OperationParser parser = new OperationParser(factory);
    final ContentNegotiator negotiator =
        new FormatSelector(Collections.singleton(MockFormat.ALWAYS_APPLICABLE),
            new VariantConverter());
    acceptor = Mockito.mock(OperationAcceptor.class);
    endpoint =
        new Endpoint(parser, negotiator, acceptor, processor).authorize(Anonymous.INSTANCE,
            EnumSet.allOf(Action.class));
  }

  @Override
  public Set<Object> getSingletons() {
    return ImmutableSet.of(endpoint, new DatasetExceptionMapper());
  }

  public OperationAcceptor getAcceptor() {
    return acceptor;
  }

  public void reset() {
    Mockito.reset(acceptor);
  }
}
