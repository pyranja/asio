package at.ac.univie.isc.asio.frontend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import javax.ws.rs.core.Variant;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.net.MediaType;

public class VariantConverterTest {

  private VariantConverter subject;

  @Before
  public void setUp() {
    subject = new VariantConverter();
  }

  @Test
  public void variant_has_correct_type() throws Exception {
    final Variant converted = subject.asVariant(MediaType.create("test", "type"));
    assertEquals("test", converted.getMediaType().getType());
    assertEquals("type", converted.getMediaType().getSubtype());
  }

  @Test
  public void variant_has_default_language() throws Exception {
    final Variant converted = subject.asVariant(MediaType.ANY_TYPE);
    assertEquals(Locale.ENGLISH, converted.getLanguage());
  }

  @Test
  public void variant_has_no_encoding() throws Exception {
    final Variant converted =
        subject.asVariant(MediaType.create("*", "*").withCharset(Charsets.ISO_8859_1));
    assertNull(converted.getEncoding());
  }

  @Test
  public void variant_has_no_encoding_if_none_given() throws Exception {
    final Variant converted = subject.asVariant(MediaType.ANY_TYPE);
    assertNull(converted.getEncoding());
  }

  @Test
  public void content_type_is_equivalent() throws Exception {
    final javax.ws.rs.core.MediaType converted =
        subject.asContentType(MediaType.create("test", "type"));
    assertEquals("test", converted.getType());
    assertEquals("type", converted.getSubtype());
  }

  @Ignore("resolve exact matching issue")
  @Test
  public void encoding_is_equivalent() throws Exception {
    final javax.ws.rs.core.MediaType converted =
        subject.asContentType(MediaType.create("*", "*").withCharset(Charsets.ISO_8859_1));
    assertEquals(Charsets.ISO_8859_1.name(), converted.getParameters().get("charset"));
  }
}
