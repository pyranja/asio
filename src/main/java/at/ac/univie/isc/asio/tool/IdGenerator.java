package at.ac.univie.isc.asio.tool;

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
