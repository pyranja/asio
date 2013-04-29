package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.transport.FileResult;

public interface DatasetEngine {

	FileResult submit(String query);
}
