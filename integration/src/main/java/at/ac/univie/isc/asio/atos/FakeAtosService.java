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
package at.ac.univie.isc.asio.atos;

import at.ac.univie.isc.asio.Integration;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.io.ByteSource;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Stub http server that serves canned vph metadata.
 */
public final class FakeAtosService implements HttpHandler {
  /**
   * Add a root context to the given {@code HttpServer} that mimics the atos vph metadata repository
   * API for local id queries. It serves metadata for the local ids
   * {@link at.ac.univie.isc.asio.Integration#IDENTIFIER} and
   * {@link at.ac.univie.isc.asio.Integration#BASE_URI}.
   *
   * @return the http server
   * @param httpServer the http server that should host the metadata repository fake
   */
  public static HttpServer attachTo(final HttpServer httpServer) {
    return httpServer.with("/", new FakeAtosService());
  }

  private static final String LOCAL_ID_PATH = "/facets/Dataset/localID";
  private static final String LOCAL_ID_PARAMETER = "value";

  private final ByteSource notFoundResponse = Classpath.load("atos/atos-not_found.xml");
  private final ByteSource referenceResponse = Classpath.load("atos/atos-reference.xml");

  @Override
  public void handle(final HttpExchange exchange) throws IOException {
    final URI uri = exchange.getRequestURI();
    if (uri.getPath().endsWith(LOCAL_ID_PATH)) {
      final Map<String, String> parameters = HttpServer.parseParameters(exchange);
      final String id = parameters.get(LOCAL_ID_PARAMETER);
      if (id == null) {
        exchange.sendResponseHeaders(HttpStatus.SC_INTERNAL_SERVER_ERROR, 0);
      } else if (Integration.IDENTIFIER.equals(id) || Integration.BASE_URI.equals(id)) {
        send(exchange, referenceResponse);
      } else {
        send(exchange, notFoundResponse);
      }
    } else {
      exchange.sendResponseHeaders(HttpStatus.SC_NOT_FOUND, 0);
    }
  }

  private void send(final HttpExchange exchange, final ByteSource content) throws IOException {
    exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/xml");
    exchange.sendResponseHeaders(HttpStatus.SC_OK, content.size());
    content.copyTo(exchange.getResponseBody());
  }
}
