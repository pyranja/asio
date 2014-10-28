package at.ac.unvie.isc.asio.web;

/**
 * The five HTTP status code families. Ported from {@code javax.ws.rs.core.Response.Status.Family}.
 */
public enum HttpCode {
  INFORMATIONAL(100),
  SUCCESSFUL(200),
  REDIRECTION(300),
  CLIENT_ERROR(400),
  SERVER_ERROR(500),
  OTHER(600);

  private final int minimum;
  private final int maximum;

  private HttpCode(final int base) {
    minimum = base;
    maximum = base + 100;
  }

  /**
   * @param code to be checked
   * @return true if the given status code falls into this status family.
   */
  public boolean includes(final int code) {
    return minimum <= code && code < maximum;
  }

  /**
   * @param code actual HTTP status code
   * @return family of the given code, e.g. {@link #CLIENT_ERROR} for {@code 404}.
   */
  public static HttpCode valueOf(final int code) {
    switch (code / 100) {
      case 1:
        return HttpCode.INFORMATIONAL;
      case 2:
        return HttpCode.SUCCESSFUL;
      case 3:
        return HttpCode.REDIRECTION;
      case 4:
        return HttpCode.CLIENT_ERROR;
      case 5:
        return HttpCode.SERVER_ERROR;
      default:
        return HttpCode.OTHER;
    }
  }
}
