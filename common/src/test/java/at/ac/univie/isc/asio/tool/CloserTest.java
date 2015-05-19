/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.tool;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CloserTest {

  @Test
  public void should_call_given_closer_action() throws Exception {
    final AtomicBoolean called = new AtomicBoolean(false);
    Closer.quietly(new Object(), new Closer<Object>() {
      @Override
      public void close(final Object it) throws Exception {
        called.set(true);
      }
    });
    assertThat(called.get(), equalTo(true));
  }

  @Test
  public void should_suppress_exception_from_action() throws Exception {
    Closer.quietly(new Object(), new Closer<Object>() {
      @Override
      public void close(final Object it) throws Exception {
        throw new IllegalStateException("test");
      }
    });
  }

  @Test(expected = Error.class)
  public void should_not_swallow_Error() throws Exception {
    Closer.quietly(new Object(), new Closer<Object>() {
      @Override
      public void close(final Object it) throws Exception {
        throw new Error("test");
      }
    });
  }

  @Test
  public void should_ignore_null_input() throws Exception {
    Closer.quietly(null, new Closer<Object>() {
      @Override
      public void close(final Object it) throws Exception {
        fail("called action on null input");
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void should_fail_on_null_action() throws Exception {
    Closer.quietly(new Object(), null);
  }
}
