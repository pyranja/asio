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
import at.ac.univie.isc.asio.insight.Emitter;
import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(Enclosed.class)
public class CatalogTest {

  public static class PublicApi {
    @Rule
    public ExpectedException error = ExpectedException.none();

    private Catalog subject = new Catalog(Mockito.mock(Emitter.class));

    @Test
    public void should_yield_absent_if_deploying_new_container() throws Exception {
      final Optional<Container> former = subject.deploy(StubContainer.create("test"));
      assertThat(former.isPresent(), equalTo(false));
    }

    @Test
    public void should_yield_former_if_replacing() throws Exception {
      final Container replaced = StubContainer.create("test");
      subject.deploy(replaced);
      final Container replacement = StubContainer.create("test");
      final Optional<Container> former = subject.deploy(replacement);
      assertThat(former.get(), equalTo(replaced));
    }

    @Test
    public void should_store_deployed_container() throws Exception {
      final StubContainer deployed = StubContainer.create("test");
      subject.deploy(deployed);
      assertThat(subject.internal().values(), hasItem(deployed));
    }

    @Test
    public void should_yield_absent_if_dropped_missing() throws Exception {
      final Optional<Container> dropped = subject.drop(Id.valueOf("name"));
      assertThat(dropped.isPresent(), equalTo(false));
    }

    @Test
    public void should_yield_dropped() throws Exception {
      final Container container = StubContainer.create("name");
      subject.deploy(container);
      final Optional<Container> dropped = subject.drop(Id.valueOf("name"));
      assertThat(dropped.get(), equalTo(container));
    }

    @Test
    public void should_remove_dropped_from_internal_storage() throws Exception {
      final StubContainer dropped = StubContainer.create("test");
      subject.deploy(dropped);
      subject.drop(Id.valueOf("test"));
      assertThat(subject.internal().values(), not(hasItem(dropped)));
    }

    @Test
    public void should_yield_remaining_on_clear() throws Exception {
      final Container container = StubContainer.create("test");
      subject.deploy(container);
      final Set<Container> remaining = subject.clear();
      assertThat(remaining, contains(container));
    }

    @Test
    public void should_remove_all_from_internal_map_on_clear() throws Exception {
      subject.deploy(StubContainer.create("test"));
      subject.deploy(StubContainer.create("another"));
      subject.clear();
      assertThat(subject.internal().values(), empty());
    }
  }


  public static class Eventing {
    private final Emitter events = Mockito.mock(Emitter.class);
    private Catalog subject = new Catalog(events);

    @Test
    public void emit_event_on_deploying_schema() throws Exception {
      subject.deploy(StubContainer.create("name"));
      verify(events).emit(Matchers.isA(ContainerEvent.Deployed.class));
    }

    @Test
    public void emit_event_on_dropping_schema() throws Exception {
      subject.deploy(StubContainer.create("name"));
      subject.drop(Id.valueOf("name"));
      final InOrder ordered = inOrder(events);
      ordered.verify(events).emit(Matchers.isA(ContainerEvent.Deployed.class));
      ordered.verify(events).emit(Matchers.isA(ContainerEvent.Dropped.class));
    }

    @Test
    public void emit_drop_and_deploy_when_replacing() throws Exception {
      subject.deploy(StubContainer.create("first"));
      subject.deploy(StubContainer.create("first"));
      final InOrder ordered = inOrder(events);
      ordered.verify(events).emit(Matchers.isA(ContainerEvent.Deployed.class));
      ordered.verify(events).emit(Matchers.isA(ContainerEvent.Dropped.class));
      ordered.verify(events).emit(Matchers.isA(ContainerEvent.Deployed.class));
    }

    @Test
    public void no_event_if_schema_not_present_on_drop() throws Exception {
      subject.drop(Id.valueOf("name"));
      verifyZeroInteractions(events);
    }

    @Test
    public void emit_drop_for_all_remaining_on_close() throws Exception {
      subject.deploy(StubContainer.create("one"));
      subject.deploy(StubContainer.create("two"));
      subject.clear();
      final InOrder ordered = inOrder(events);
      ordered.verify(events, times(2)).emit(Matchers.isA(ContainerEvent.Deployed.class));
      ordered.verify(events, times(2)).emit(Matchers.isA(ContainerEvent.Dropped.class));
    }
  }
}
