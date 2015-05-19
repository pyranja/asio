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

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.Credentials;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Eagerly add basic authentication headers to any intercepted request.
 * <strong>Note:</strong> Using preemptive basic auth reveals the credentials to
 * <strong>ANY</strong> targeted server!
 */
class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
  private final String header;

  public PreemptiveAuthInterceptor(final Credentials credentials) {
    header = encodeAsBasicAuthorizationHeader(credentials);
  }

  @Override
  public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
    request.addHeader(HttpHeaders.AUTHORIZATION, header);
  }

  private String encodeAsBasicAuthorizationHeader(final Credentials credentials) {
    final String usernameAndPassword =
        credentials.getUserPrincipal().getName() + ":" + credentials.getPassword();
    final String header = BaseEncoding.base64().encode(Payload.encodeUtf8(usernameAndPassword));
    return "Basic " + header;
  }
}
