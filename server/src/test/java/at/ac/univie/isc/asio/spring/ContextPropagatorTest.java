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
package at.ac.univie.isc.asio.spring;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class ContextPropagatorTest {
  private final RequestAttributes attributes = Mockito.mock(RequestAttributes.class);
  private final ContextPropagator subject = new ContextPropagator(attributes, new Thread());

  @After
  public void tearDown() throws Exception {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void should_store_context_of_current_thread_on_creation() throws Exception {
    RequestContextHolder.setRequestAttributes(attributes);
    assertThat(ContextPropagator.capture().getStoredAttributes(), sameInstance(attributes));
  }

  @Test
  public void should_publish_stored_attributes_to_current_thread() throws Exception {
    subject.publish();
    assertThat(RequestContextHolder.currentRequestAttributes(), sameInstance(attributes));
  }

  @Test
  public void should_clear_attributes_from_current_thread() throws Exception {
    RequestContextHolder.setRequestAttributes(attributes);
    subject.close();
    assertThat(RequestContextHolder.getRequestAttributes(), nullValue());
  }

  @Test(expected = IllegalStateException.class)
  public void should_assert_clearing_only_own_attributes() throws Exception {
    RequestContextHolder.setRequestAttributes(Mockito.mock(RequestAttributes.class));
    subject.close();
  }
}
