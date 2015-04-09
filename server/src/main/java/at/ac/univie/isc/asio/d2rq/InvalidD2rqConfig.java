package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.InvalidUsage;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Thrown if a given d2rq mapping cannot be read.
 */
public final class InvalidD2rqConfig extends InvalidUsage {
  InvalidD2rqConfig(final RDFNode offender, final String reason) {
    super("<" + offender + "> is illegal - " + reason);
  }
}
