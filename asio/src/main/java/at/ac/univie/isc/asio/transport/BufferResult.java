package at.ac.univie.isc.asio.transport;

import java.io.IOException;
import java.io.InputStream;

import at.ac.univie.isc.asio.Result;

import com.google.common.io.InputSupplier;
import com.google.common.net.MediaType;

/**
 * Simple {@link Result} capturer.
 * 
 * @author Chris Borckholder
 */
public class BufferResult implements Result {

	private final MediaType mime;
	private final InputSupplier<InputStream> buffer;

	BufferResult(final InputSupplier<InputStream> buffer, final MediaType mime) {
		super();
		this.mime = mime;
		this.buffer = buffer;
	}

	@Override
	public InputStream getInput() throws IOException {
		return buffer.getInput();
	}

	@Override
	public MediaType mediaType() {
		return mime;
	}

}
