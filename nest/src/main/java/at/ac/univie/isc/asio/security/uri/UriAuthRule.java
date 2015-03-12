package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;

import java.util.Map;

/**
 * A rule to extract authorization and redirect based on a parsed request URI.
 */
public interface UriAuthRule {
  /**
   * Whether this rule applies.
   * @param pathElements parsed URI elements.
   * @return true if this rule applies
   */
  boolean canHandle(PathElements pathElements);

  /**
   * Perform the actual processing. Extract the authorization information and determine the correct
   * redirection target if necessary.
   *
   * @param pathElements parsed URI elements.
   * @return redirection and authorization
   */
  FindAuthorization.AuthAndRedirect handle(PathElements pathElements);

  /**
   * Hold components parsed from an URI.
   */
  interface PathElements {
    /**
     * Find the component with given name or fail with an error.
     *
     * @param key name of required component
     * @return the matched value for the given key
     * @throws IllegalArgumentException if the given key was not matched
     */
    String require(String key) throws IllegalArgumentException;

    /**
     * Map based for testing.
     */
    static final class Mock implements PathElements {
      private final Map<String, String> elements;

      private Mock(final Map<String, String> elements) {
        this.elements = elements;
      }

      public static Mock from(final Map<String, String> elements) {
        return new Mock(elements);
      }

      @Override
      public String require(final String key) throws IllegalArgumentException {
        final String value = elements.get(key);
        if (value == null) {
          throw new IllegalArgumentException("no value for " + key);
        } else {
          return value;
        }
      }
    }
  }
}
