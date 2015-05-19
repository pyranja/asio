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
package at.ac.univie.isc.asio.insight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * Inject the request/response correlation id into the response headers.
 */
@Component
@javax.ws.rs.ext.Provider
@Priority(Priorities.HEADER_DECORATOR)
public final class InjectCorrelationFilter implements ContainerResponseFilter {
  public static final String CORRELATION_HEADER = "Correlation";

  private final Provider<Correlation> correlation;

  @Autowired
  InjectCorrelationFilter(final Provider<Correlation> provider) {
    this.correlation = provider;
  }

  @Override
  public void filter(final ContainerRequestContext request,
                     final ContainerResponseContext response) throws IOException {
    response.getHeaders().putSingle(CORRELATION_HEADER, correlation.get());
  }
}
