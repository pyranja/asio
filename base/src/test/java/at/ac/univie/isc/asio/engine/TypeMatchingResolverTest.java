package at.ac.univie.isc.asio.engine;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TypeMatchingResolverTest {

  public static final Supplier<Object> DUMMY_SUPPLIER = Suppliers.ofInstance(new Object());
  public static final List<MediaType> ANY = Arrays.asList(MediaType.WILDCARD_TYPE);

  @Rule
  public ExpectedException error = ExpectedException.none();

  private TypeMatchingResolver<Object> subject;
  private TypeMatchingResolver.Selection<Object> selection;

  private MediaType _(final String type) {
    return MediaType.valueOf(type);
  }

  private Iterable<MediaType> all(final String... types) {
    return Iterables.transform(Arrays.asList(types), new Function<String, MediaType>() {
      @Nullable
      @Override
      public MediaType apply(@Nullable final String input) {
        return MediaType.valueOf(input);
      }
    });
  }

  @Test
  public void uses_registered_supplier() throws Exception {
    final Supplier<Object> supplier = mock(Supplier.class);
    when(supplier.get()).thenReturn(new Object());
    subject = TypeMatchingResolver.builder()
        .register(MediaType.WILDCARD_TYPE, supplier).make();
    subject.select(ANY);
    verify(supplier).get();
  }

  @Test
  public void exact_match() throws Exception {
    final MediaType expected = MediaType.APPLICATION_JSON_TYPE;
    subject = TypeMatchingResolver.builder()
        .register(expected, DUMMY_SUPPLIER).make();
    selection = subject.select(all("application/json"));
    assertThat(selection.type(), is(expected));
  }

  @Test
  public void type_wildcard_match() throws Exception {
    final MediaType expected = MediaType.APPLICATION_JSON_TYPE;
    subject = TypeMatchingResolver.builder()
        .register(expected, DUMMY_SUPPLIER).make();
    selection = subject.select(all("application/*"));
    assertThat(selection.type(), is(expected));
  }

  @Test
  public void subtype_wildcard_match() throws Exception {
    final MediaType expected = MediaType.APPLICATION_JSON_TYPE;
    subject = TypeMatchingResolver.builder()
        .register(expected, DUMMY_SUPPLIER).make();
    selection = subject.select(all("*/json"));
    assertThat(selection.type(), is(expected));
  }

  @Test
  public void full_wildcard_match() throws Exception {
    final MediaType expected = MediaType.APPLICATION_JSON_TYPE;
    subject = TypeMatchingResolver.builder()
        .register(expected, DUMMY_SUPPLIER).make();
    selection = subject.select(all("*/*"));
    assertThat(selection.type(), is(expected));
  }

  @Test
  public void registration_order_determines_selection_order() throws Exception {
    subject = TypeMatchingResolver.builder()
        .register(_("type/first"), DUMMY_SUPPLIER)
        .register(_("type/second"), DUMMY_SUPPLIER)
        .make();
    selection = subject.select(ANY);
    assertThat(selection.type(), is(_("type/first")));
  }

  @Test
  public void acceptable_order_determines_selection_order() throws Exception {
    subject = TypeMatchingResolver.builder()
        .register(_("type/first"), DUMMY_SUPPLIER)
        .register(_("type/second"), DUMMY_SUPPLIER)
        .make();
    selection = subject.select(all("type/second", "type/first"));
    assertThat(selection.type(), is(_("type/second")));
  }

  @Test
  public void skip_unmatched_accepted() throws Exception {
    subject = TypeMatchingResolver.builder().register(_("type/expected"), DUMMY_SUPPLIER).make();
    selection = subject.select(all("type/some", "type/other", "type/expected"));
    assertThat(selection.type(), is(_("type/expected")));
  }

  @Test
  public void yield_canonical_type_on_matching_alias() throws Exception {
    subject = TypeMatchingResolver.builder()
      .register(_("type/canonical"), DUMMY_SUPPLIER)
      .alias(_("type/alias"))
      .make();
    selection = subject.select(all("type/alias"));
    assertThat(selection.type(), is(_("type/canonical")));
  }

  @Test
  public void no_supplier_registered() throws Exception {
    subject = TypeMatchingResolver.builder().make();
    error.expect(TypeMatchingResolver.NoMatchingFormat.class);
    subject.select(Arrays.asList(MediaType.WILDCARD_TYPE));
  }
}
