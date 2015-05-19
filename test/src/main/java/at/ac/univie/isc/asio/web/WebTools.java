/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.web;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helpers for working with URIs.
 */
public final class WebTools {

  /**
   * Ensure the given URI ends with a {@code '/'}, i.e. it points to the root of a directory,
   * not to a document. If no path is defined, it is set to the root path ({@code '/'}).
   *
   * @return URI where the path ends with '/'
   */
  public static URI ensureDirectoryPath(final URI it) {
    final String path = it.getPath();
    if (isDocumentPath(path)) {
      return cloneWithPath(it, path + "/");
    }
    return it;
  }

  private static boolean isDocumentPath(final String path) {
    return path != null && !path.endsWith("/");
  }

  private static URI cloneWithPath(final URI it, final String path) {
    try {
      return new URI(it.getScheme(), it.getUserInfo(), it.getHost(), it.getPort(), path, it.getQuery(), it.getFragment());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

}
