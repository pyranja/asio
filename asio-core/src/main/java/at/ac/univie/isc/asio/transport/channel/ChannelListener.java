package at.ac.univie.isc.asio.transport.channel;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Subscribe to events on a {@link ByteChannel}. Subclasses should override
 * methods which correspond to the events they are interested in.
 * 
 * @author Chris Borckholder
 */
public abstract class ChannelListener {

	/**
	 * Invoked when the channel is {@link Channel#close() closed}.
	 */
	public void onClose() {};

	/**
	 * Invoked on a successful
	 * {@link ReadableByteChannel#read(java.nio.ByteBuffer) read} from the
	 * channel.
	 * 
	 * @param count
	 *            number of bytes read.
	 */
	public void onRead(final int count) {};

	/**
	 * Invoked on a successful
	 * {@link WritableByteChannel#write(java.nio.ByteBuffer) write} to the
	 * channel.
	 * 
	 * @param count
	 *            number of bytes written.
	 */
	public void onWrite(final int count) {};

	/**
	 * Invoked whenever an error occurs while interacting with the channel.
	 * 
	 * @param cause
	 *            of error
	 */
	public void onError(final IOException cause) {};
}
