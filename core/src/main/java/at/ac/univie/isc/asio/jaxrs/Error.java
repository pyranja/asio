package at.ac.univie.isc.asio.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO to capture exception information.
 */
@XmlRootElement
public class Error {
  private String message;
  private String cause;
  private String root;
  private String trace;

  public Error() {
  }

  public String getCause() {
    return cause;
  }

  public void setCause(final String cause) {
    this.cause = cause;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(final String root) {
    this.root = root;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getTrace() {
    return trace;
  }

  public void setTrace(final String trace) {
    this.trace = trace;
  }
}
