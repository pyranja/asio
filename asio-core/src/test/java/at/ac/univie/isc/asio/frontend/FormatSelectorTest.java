package at.ac.univie.isc.asio.frontend;

import static at.ac.univie.isc.asio.MockFormat.ALWAYS_APPLICABLE;
import static at.ac.univie.isc.asio.MockFormat.NEVER_APPLICABLE;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.collect.ImmutableSet;

@RunWith(MockitoJUnitRunner.class)
public class FormatSelectorTest {

  private ContentNegotiator subject;
  @Mock
  private Request request;
  private Set<SerializationFormat> formats;
  private final VariantConverter converter = new VariantConverter();

  @Before
  public void setUp() {
    formats = ImmutableSet.of(ALWAYS_APPLICABLE, NEVER_APPLICABLE);
    subject = new FormatSelector(formats, converter);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void passes_applicable_format_variants_to_request_selector() throws Exception {
    when(request.selectVariant(anyList())).thenReturn(new Variant(null, "en", null));
    subject.negotiate(request, Action.QUERY);
    verify(request).selectVariant(anyList());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void selects_the_applicable_format() throws Exception {
    final Variant valid = converter.asVariant(ALWAYS_APPLICABLE.asMediaType());
    when(request.selectVariant((List<Variant>) argThat(hasItem(valid)))).thenReturn(valid);
    final SerializationFormat selected = subject.negotiate(request, Action.QUERY);
    assertThat(selected, is(ALWAYS_APPLICABLE));
  }

  @Test(expected = WebApplicationException.class)
  public void fails_if_no_format_applicable() throws Exception {
    formats = singleton(NEVER_APPLICABLE);
    subject = new FormatSelector(formats, converter);
    subject.negotiate(request, Action.QUERY);
  }

  @SuppressWarnings("unchecked")
  @Test(expected = WebApplicationException.class)
  public void fails_if_request_selector_returns_null() throws Exception {
    when(request.selectVariant(anyList())).thenReturn(null);
    subject.negotiate(request, Action.QUERY);
  }
}
