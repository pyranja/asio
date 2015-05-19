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
package at.ac.univie.isc.asio.restassured;

import at.ac.univie.isc.asio.junit.CompositeReport;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.web.HttpExchangeReport;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.ByteStreams;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

/**
 * Intercept rest-assured exchanges and format them as Interaction.Report
 */
public final class ReportingFilter implements Filter, Interactions.Report {
  private final CompositeReport reports = CompositeReport.create();

  public static ReportingFilter create() {
    return new ReportingFilter();
  }

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    reports.appendTo(sink);
    return sink;
  }

  @Override
  public String toString() {
    return "rest-assured";
  }

  @Override
  public Response filter(final FilterableRequestSpecification requestSpec, final FilterableResponseSpecification responseSpec, final FilterContext ctx) {
    // register http client interceptors
    final HttpClient client = requestSpec.getHttpClient();
    assert client instanceof AbstractHttpClient : "unexpected http client type";
    final HttpExchangeReport current = HttpExchangeReport.named("rest-assured");
    reports.attach(current);
    final ReportingInterceptor interceptor = new ReportingInterceptor(current);
    ((AbstractHttpClient) client).addRequestInterceptor(interceptor);
    ((AbstractHttpClient) client).addResponseInterceptor(interceptor);
    // proceed with execution
    return ctx.next(requestSpec, responseSpec);
  }

  private final class ReportingInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor {
    private final HttpExchangeReport report;

    public ReportingInterceptor(final HttpExchangeReport report) {
      this.report = report;
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      final RequestLine requestLine = request.getRequestLine();
      report
          .captureRequest(requestLine.getMethod(), URI.create(requestLine.getUri()), asMap(request.getAllHeaders()));
      if (request instanceof HttpEntityEnclosingRequest) {
        final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
        if (entity != null) {
          final BufferedHttpEntity cached = new BufferedHttpEntity(entity);
          ((HttpEntityEnclosingRequest) request).setEntity(cached);
          report.withRequestBody(ByteStreams.toByteArray(cached.getContent()));
        }
      }
    }

    @Override
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
      report
          .captureResponse(response.getStatusLine().getStatusCode(), asMap(response.getAllHeaders()));
      final HttpEntity entity = response.getEntity();
      if (entity != null) {
        final BufferedHttpEntity cached = new BufferedHttpEntity(entity);
        response.setEntity(cached);
        report.withResponseBody(ByteStreams.toByteArray(cached.getContent()));
      }
    }

    private ImmutableMap<String, Collection<String>> asMap(final Header[] rawHeaders) {
      final ImmutableMultimap.Builder<String, String> headers = ImmutableMultimap.builder();
      for (final org.apache.http.Header each : rawHeaders) {
        headers.put(each.getName(), each.getValue());
      }
      return headers.build().asMap();
    }
  }
}
