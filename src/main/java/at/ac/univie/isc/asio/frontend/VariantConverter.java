package at.ac.univie.isc.asio.frontend;

import java.nio.charset.Charset;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import com.google.common.base.Optional;

/**
 * Convert guava {@link com.google.common.net.MediaType media types} to corresponding
 * {@link Variant JAXRS variants}.
 * 
 * @author Chris Borckholder
 */
public class VariantConverter {

  public VariantConverter() {
    super();
  }

  /**
   * Create a new variant matching the given media type as close as possible.
   * 
   * @param given guava media type
   * @return matching JAXRS variant
   */
  public Variant asVariant(final com.google.common.net.MediaType given) {
    final MediaType type = asContentType(given);
    final Locale language = Locale.ENGLISH;
    final String encoding = null; // omitted
    return new Variant(type, language, encoding);
  }

  /**
   * Create a new JAXRS Mediatype matching the given guava type as close as possible.
   * 
   * @param given guava media type
   * @return matching JAXRS media type
   */
  public MediaType asContentType(final com.google.common.net.MediaType given) {
    // FIXME ignores charsets - should assume UTF-8 ?
    return new MediaType(given.type(), given.subtype());
    // return new MediaType(given.type(), given.subtype(), encodingOrNullFor(given.charset()));
  }

  private String encodingOrNullFor(final Optional<Charset> charset) {
    if (charset.isPresent()) {
      return charset.get().toString();
    } else {
      return null;
    }
  }
}