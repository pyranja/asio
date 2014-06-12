package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.tool.Resources;

/**
 * A transfer utilizing the JDK built in {@link Pipe}. Releasing will close both ends of the used
 * pipe.
 * 
 * @author Chris Borckholder
 */
public class JdkPipeTransfer implements Transfer {

  private final Pipe inner;

  /**
   * May fail if the creation of the {@link Pipe inner pipe} fails.
   */
  public JdkPipeTransfer() {
    try {
      inner = Pipe.open();
    } catch (final IOException e) {
      throw new DatasetTransportException(e);
    }
  }

  @Override
  public WritableByteChannel sink() {
    return inner.sink();
  }

  @Override
  public ReadableByteChannel source() {
    return inner.source();
  }

  @Override
  public void release() {
    Resources.close(inner.sink());
    Resources.close(inner.source());
  }

}
