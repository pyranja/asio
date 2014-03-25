package at.ac.univie.isc.asio.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import at.ac.univie.isc.asio.transport.Transfer;

public final class ByteArrayTransfer implements Transfer {
  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  @Override
  public WritableByteChannel sink() {
    return Channels.newChannel(baos);
  }

  @Override
  public ReadableByteChannel source() {
    return Channels.newChannel(new ByteArrayInputStream(baos.toByteArray()));
  }

  @Override
  public void release() {
    /* no op */
  }

  public byte[] buffer() {
    return baos.toByteArray();
  }
}
