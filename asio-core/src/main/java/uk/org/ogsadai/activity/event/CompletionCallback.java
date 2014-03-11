package uk.org.ogsadai.activity.event;

/**
 * Callback to track the successful or erroneous completion of a request.
 * 
 * @author Chris Borckholder
 */
public interface CompletionCallback {

  /**
   * request completed successfully.
   */
  void complete();

  /**
   * request failed due to the given cause.
   * 
   * @param cause of failure
   */
  void fail(Exception cause);
}
