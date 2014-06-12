package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.tool.VariantConverter;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

public class FormatMatcher {
  private static final VariantConverter CONVERTER = new VariantConverter();

  private final Set<DatasetOperation.SerializationFormat> allSupportedFormats;

  public FormatMatcher(final Set<DatasetOperation.SerializationFormat> allSupportedFormats) {
    this.allSupportedFormats = allSupportedFormats;
  }

  public FormatAndMediaType match(List<MediaType> allAcceptedTypes) {
    for (final MediaType accepted : allAcceptedTypes) {
      for (final DatasetOperation.SerializationFormat format : allSupportedFormats) {
        final MediaType supported = CONVERTER.asContentType(format.asMediaType());
        if (accepted.isCompatible(supported)) {
          return new FormatAndMediaType(format, supported);
        }
      }
    }
    // FIXME : replace with specific subclass
    throw new DatasetUsageException("no matching format supported");
  }

  public static class FormatAndMediaType {
    public final DatasetOperation.SerializationFormat format;
    public final MediaType type;

    public FormatAndMediaType(final DatasetOperation.SerializationFormat format, final MediaType type) {
      this.format = format;
      this.type = type;
    }
  }
}
