package at.ac.univie.isc.asio.transport;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Represent an unidirectional pipe.
 * 
 * @author Chris Borckholder
 */
public interface Transfer {

  /**
   * @return the writable end of the pipe
   */
  WritableByteChannel sink();

  /**
   * @return the readable end of the pipe
   */
  ReadableByteChannel source();

  /**
   * Close {@link #sink()} and {@link #source()} and free all resources held by this pipe.
   */
  void release();
}
