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
package at.ac.univie.isc.asio.engine.sql;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValuePresenterTest {

  private Representation representation;
  private ValuePresenter subject;

  @Before
  public void setUp() throws Exception {
    representation = Mockito.mock(Representation.class);
    when(representation.apply(any())).thenReturn("test");
  }

  @Test
  public void should_use_registered_function_for_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Long.class).build();
    subject.format(1L, Long.class);
    verify(representation).apply(1L);
  }

  @Test
  public void should_yield_registered_functions_return_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Long.class).build();
    final String actual = subject.format(1L, Long.class);
    assertThat(actual, is("test"));
  }

  @Test
  public void should_use_void_representation_for_null_value() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Void.class).build();
    subject.format(null, Object.class);
    verify(representation).apply(null);
  }

  @Test
  public void fall_back_to_default_representation() throws Exception {
    subject = ValuePresenter.withDefault(representation).build();
    final String formatted = subject.format("fallback", Object.class);
    assertThat(formatted, is("test"));
  }

  @Test(expected = ValuePresenter.NoRepresentationFound.class)
  public void fail_if_no_representation_found() throws Exception {
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).build();
    subject.format(new Object(), Object.class);
  }

  @Test(expected = AssertionError.class)
  public void fail_if_representation_yields_null() throws Exception {
    when(representation.apply(any())).thenReturn(null);
    subject = ValuePresenter.withDefault(ValuePresenter.FAIL).register(representation, Object.class).build();
    subject.format(new Object(), Object.class);
  }
}
