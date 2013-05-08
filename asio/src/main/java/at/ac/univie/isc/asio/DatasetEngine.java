package at.ac.univie.isc.asio;

import java.io.InputStream;

import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ListenableFuture;

public interface DatasetEngine {

	ListenableFuture<InputSupplier<InputStream>> submit(String query);
}
