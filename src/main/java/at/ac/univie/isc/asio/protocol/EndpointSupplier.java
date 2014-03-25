package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Language;

public interface EndpointSupplier {

  Endpoint get(final Language language);

}
