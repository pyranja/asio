package at.ac.univie.isc.asio.protocol;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import org.slf4j.MDC;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.MockResult;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;
import at.ac.univie.isc.asio.frontend.OperationRouter;
import at.ac.univie.isc.asio.frontend.VariantConverter;

// FIXME : replace with Mockito mock
public class MockOperationRouter implements OperationRouter {

  private static final VariantConverter CONVERT = new VariantConverter();

  private final AsyncProcessor processor;

  private DatasetOperation lastOperation;

  public MockOperationRouter(final AsyncProcessor processor) {
    super();
    this.processor = processor;
  }

  @Override
  public void accept(final OperationBuilder partial, final Language language,
      final Request request, final AsyncResponse response) {
    try {
      MDC.put("test", "is testing"); // FIXME avoid NPE in asyncprocessor
      assertThat(language, is(Language.TEST));
      final Variant selected =
          request.selectVariant(Arrays.asList(CONVERT.asVariant(MockFormat.APPLICABLE_MIME)));
      assertThat(selected, is(notNullValue()));
      lastOperation = partial.renderAs(MockFormat.ALWAYS_APPLICABLE);
      processor.handle(MockResult.successFuture(), response);
    } catch (final Throwable t) {
      response.resume(t); // resume immediately on error
    }
  }

  public DatasetOperation getLastOperation() {
    return lastOperation;
  }
}
