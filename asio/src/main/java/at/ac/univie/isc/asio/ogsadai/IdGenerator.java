package at.ac.univie.isc.asio.ogsadai;


/**
 * Generate string identifier.
 * 
 * @author Chris Borckholder
 */
public interface IdGenerator {

	/**
	 * @return a new identifier
	 */
	String next();
}
