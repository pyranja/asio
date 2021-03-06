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
package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.Scope;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.Locale;
import java.util.Set;

/**
 * Supports tunneling of accept-header through query parameters.
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public final class ContentNegotiationOverrideFilter implements ContainerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(ContentNegotiationOverrideFilter.class);

  public static final String DEPRECATED_PARAMETER_NAME = "x-asio-accept";
  public static final String DEFAULT_PARAMETER_NAME = "_accept";

  private final Set<String> parameterNames;

  public ContentNegotiationOverrideFilter(final String parameterName) {
    parameterNames = ImmutableSet.of(DEPRECATED_PARAMETER_NAME, parameterName);
    log.info(Scope.SYSTEM.marker(), "initialize accept tunnel with parameters {}", parameterNames);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    final UriInfo uri = requestContext.getUriInfo();
    final MultivaluedMap<String, String> queryParameters = uri.getQueryParameters();
    for (final String overrideParameter : parameterNames) {
      overrideIfPresent(overrideParameter, requestContext, queryParameters);
    }
  }

  private void overrideIfPresent(final String parameterName, final ContainerRequestContext requestContext, final MultivaluedMap<String, String> params) {
    if (params.containsKey(parameterName)) {
      final String accept = params.getFirst(parameterName);
      log.debug(Scope.REQUEST.marker(), "override header {Accept} with '{}'", accept);
      requestContext.getHeaders().putSingle(HttpHeaders.ACCEPT, accept);
      requestContext.getHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, Locale.ENGLISH.getLanguage());
    }
  }
}
