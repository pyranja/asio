/*
 * #%L
 * asio integration
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

import at.ac.univie.isc.asio.web.WebTools;
import com.jayway.restassured.builder.RequestSpecBuilder;

import java.net.URI;

/**
 * Authorize requests by injecting the role into the request uri.
 */
final class UriAuthMechanism extends AuthMechanism {
  @Override
  public boolean isAuthorizing() {
    return true;
  }

  @Override
  public URI configureUri(final URI uri, final String role) {
    return uri.resolve(WebTools.ensureDirectoryPath(URI.create(role)));
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String ignored) {
    return spec;
  }
}
