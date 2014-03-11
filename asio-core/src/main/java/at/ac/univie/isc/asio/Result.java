package at.ac.univie.isc.asio;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.InputSupplier;
import com.google.common.net.MediaType;

/**
 * Represent the result of an {@link DatasetOperation operation}.
 * 
 * @author Chris Borckholder
 */
public interface Result extends InputSupplier<InputStream> {

  /**
   * @return InputStream containing the result data
   * @throws IOException if retrieving the stream fails
   */
  @Override
  InputStream getInput() throws IOException;

  /**
   * @return the format of the result data
   */
  MediaType mediaType();
}
