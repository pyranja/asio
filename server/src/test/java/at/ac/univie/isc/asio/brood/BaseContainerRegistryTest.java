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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class BaseContainerRegistryTest {
  @Rule
  public final ExpectedException error = ExpectedException.none();

  private final BaseContainerRegistry subject = new BaseContainerRegistry() { /* empty */ };

  @Test
  public void should_fail_if_requested_schema_not_present() throws Exception {
    error.expect(Id.NotFound.class);
    error.expectMessage(containsString("not-there"));
    subject.find(Id.valueOf("not-there"));
  }

  @Test
  public void should_find_deployed_schema() throws Exception {
    final Container expected = StubContainer.create("test");
    subject.onDeploy(new ContainerEvent.Deployed(expected));
    assertThat(subject.find(Id.valueOf("test")), sameInstance(expected));
  }

  @Test
  public void should_not_find_schema_after_dropping_it() throws Exception {
    final Container expected = StubContainer.create("test");
    subject.onDeploy(new ContainerEvent.Deployed(expected));
    subject.onDrop(new ContainerEvent.Dropped(expected));
    error.expect(Id.NotFound.class);
    subject.find(Id.valueOf("test"));
  }
}
