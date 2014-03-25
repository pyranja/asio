package at.ac.univie.isc.asio.common;

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
