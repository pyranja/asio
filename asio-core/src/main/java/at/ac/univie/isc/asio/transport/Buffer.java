package at.ac.univie.isc.asio.transport;

import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

/**
 * Marker interface for buffers that are capable of providing
 * {@link OutputStream}s to write content and {@link InputStream}s to read the
 * buffer's content.
 * 
 * @author Chris Borckholder
 */
public interface Buffer extends InputSupplier<InputStream>,
		OutputSupplier<OutputStream> {}
