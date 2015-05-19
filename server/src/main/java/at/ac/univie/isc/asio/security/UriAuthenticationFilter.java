/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.security;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticate requests, which use uri based authentication. One path element of the request uri
 * holds the login name and is extracted by this filter.
 */
public final class UriAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  /**
   * Expected request uri format. The principal is the second element of the path.
   */
  private static final Pattern URI_TEMPLATE = Pattern.compile("^/[^/]+/(?<principal>[^/?#]+).*$");

  /**
   * Create the filter with default settings.
   */
  public static UriAuthenticationFilter create() {
    final UrlPathHelper pathHelper = new UrlPathHelper();
    pathHelper.setRemoveSemicolonContent(true);
    pathHelper.setUrlDecode(true);
    return new UriAuthenticationFilter(pathHelper);
  }

  private final UrlPathHelper paths;

  private UriAuthenticationFilter(final UrlPathHelper paths) {
    logger.info("using uri template: " + URI_TEMPLATE);
    this.paths = paths;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    final String path = paths.getPathWithinApplication(request);

    if (logger.isDebugEnabled()) {
      logger.debug("inspecting request uri <" + path + "> for embedded principal");
    }

    final Matcher matcher = URI_TEMPLATE.matcher(path);
    return matcher.matches()
        ? matcher.group("principal")
        : null; // null means no authentication found
  }

  @Override
  protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
    return "N/A";
  }
}
