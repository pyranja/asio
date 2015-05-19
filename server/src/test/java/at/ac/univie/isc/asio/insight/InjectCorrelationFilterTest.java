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

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class InjectCorrelationFilterTest {

  @Test
  public void should_set_correlation_id_as_response_header() throws Exception {
    final InjectCorrelationFilter subject = new InjectCorrelationFilter(new Provider<Correlation>() {
      @Override
      public Correlation get() {
        return Correlation.valueOf("test");
      }
    });
    final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
    final ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
    final ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
    given(responseContext.getHeaders()).willReturn(headers);
    subject.filter(requestContext, responseContext);
    final Matcher<Iterable<?>> expected = Matchers.<Object>contains(Correlation.valueOf("test"));
    assertThat(headers, hasEntry(equalTo("Correlation"), expected));
  }
}
