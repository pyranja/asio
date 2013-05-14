package at.ac.univie.isc.asio.ogsadai;

/**
 * Generate string identifiers.
 * 
 * @author Chris Borckholder
 */
public interface IdGenerator {

	/**
	 * @return a new identifier
	 */
	String next();
}
