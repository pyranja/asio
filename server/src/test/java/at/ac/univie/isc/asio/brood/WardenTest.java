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

import at.ac.univie.isc.asio.ConfigStore;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.flock.FlockAssembler;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.nest.D2rqNestAssembler;
import at.ac.univie.isc.asio.tool.StatefulMonitor;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(HierarchicalContextRunner.class)
public class WardenTest {
  public static final Runnable NOOP = new Runnable() {
    @Override
    public void run() {
      ;
    }
  };

  @Rule
  public final ExpectedException error = ExpectedException.none();

  private final ConfigStore store = Mockito.mock(ConfigStore.class);
  private final D2rqNestAssembler d2rq = Mockito.mock(D2rqNestAssembler.class);
  private final FlockAssembler json = Mockito.mock(FlockAssembler.class);
  private final Catalog catalog = Mockito.mock(Catalog.class);

  private final Warden subject = new Warden(catalog, d2rq, json, store, Timeout.undefined());

  @Test
  public void should_have_correct_lifecycle_settings() throws Exception {
    assertThat(subject.isAutoStartup(), equalTo(true));
    assertThat(subject.getPhase(), equalTo(Integer.MAX_VALUE));
  }

  public class WhenNotRunning {

    @Test
    public void should_report_correct_state() throws Exception {
      assertThat(subject.isRunning(), equalTo(false));
    }

    @Test
    public void should_reject_deploy() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.deployFromD2rqMapping(Id.valueOf("test"), ByteSource.empty());
      subject.deployFromJson(Id.valueOf("test"), ByteSource.empty());
      verifyZeroInteractions();
    }

    @Test
    public void should_reject_dispose() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.dispose(Id.valueOf("test"));
      verifyZeroInteractions();
    }

    @Test
    public void should_reject_stop() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.stop(NOOP);
    }

    public class Start {
      private final Map<String, ByteSource> d2rqConfigItems = ImmutableMap.of(
          "first", ByteSource.empty()
          , "second", ByteSource.empty()
      );

      private final Map<String, ByteSource> jsonConfigItems = ImmutableMap.of(
          "single-json", ByteSource.empty()
      );

      @Before
      public void mocks() throws Exception {
        given(store.findAllWithIdentifier(Warden.D2RQ_SUFFIX)).willReturn(d2rqConfigItems);
        given(store.findAllWithIdentifier(Warden.JSON_SUFFIX)).willReturn(jsonConfigItems);
        given(d2rq.assemble(any(Id.class), any(ByteSource.class)))
            .willReturn(StubContainer.create("test"));
        given(json.assemble(any(Id.class), any(ByteSource.class)))
            .willReturn(StubContainer.create("test"));
        given(catalog.deploy(any(Container.class))).willReturn(Optional.<Container>absent());
      }

      @After
      public void ensure_no_config_deleted() {
        verify(store, never()).clear(anyString());
      }

      @Test
      public void should_be_running_after_start() throws Exception {
        subject.start();
        assertThat("not running after #start()", subject.isRunning(), equalTo(true));
      }

      @Test
      public void should_assemble_each_found_d2rq_config_item() throws Exception {
        subject.start();
        verify(d2rq).assemble(Id.valueOf("second"), ByteSource.empty());
        verify(d2rq).assemble(Id.valueOf("first"), ByteSource.empty());
      }

      @Test
      public void should_assemble_json_config_items() throws Exception {
        subject.start();
        verify(json).assemble(Id.valueOf("single-json"), ByteSource.empty());
      }

      @Test
      public void should_activate_assembled_container() throws Exception {
        final StubContainer first = StubContainer.create("first");
        final StubContainer second = StubContainer.create("second");
        given(d2rq.assemble(any(Id.class), any(ByteSource.class))).willReturn(first, second);
        subject.start();
        assertThat(first.isActivated(), equalTo(true));
        assertThat(second.isActivated(), equalTo(true));
      }

      @Test
      public void should_deploy_each_assembled_container() throws Exception {
        final StubContainer expected = StubContainer.create("dummy");
        given(d2rq.assemble(any(Id.class), any(ByteSource.class))).willReturn(expected);
        given(json.assemble(any(Id.class), any(ByteSource.class))).willReturn(expected);
        subject.start();
        verify(catalog, times(3)).deploy(expected);
      }

      @Test
      public void should_clean_up_replaced_containers() throws Exception {
        final StubContainer former = StubContainer.create("former");
        given(catalog.deploy(any(Container.class)))
            .willReturn(Optional.<Container>of(former), Optional.<Container>absent());
        subject.start();
        assertThat(former.isClosed(), equalTo(true));
      }

      @Test
      public void should_not_store_config_of_found_items_again() throws Exception {
        subject.start();
        verify(store, never()).save(anyString(), anyString(), any(ByteSource.class));
      }

      @Test
      public void should_continue_if_one_deployment_fails() throws Exception {
        given(d2rq.assemble(any(Id.class), any(ByteSource.class)))
            .willThrow(new RuntimeException())
            .willReturn(StubContainer.create("test"));
        subject.start();
        verify(catalog, times(2)).deploy(any(Container.class));
      }
    }
  }

  public class WhenRunning {

    @Before
    public void setUp() throws Exception {
      subject.start();
    }

    @Test
    public void should_report_correct_state() throws Exception {
      assertThat(subject.isRunning(), equalTo(true));
    }

    @Test
    public void should_reject_start() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.start();
    }

    public class Deploy {
      @Before
      public void mockAssembler() {
        given(d2rq.assemble(eq(Id.valueOf("test")), any(ByteSource.class)))
            .willReturn(StubContainer.create("test"));
        given(json.assemble(eq(Id.valueOf("test")), any(ByteSource.class)))
            .willReturn(StubContainer.create("test"));
        given(catalog.deploy(any(Container.class))).willReturn(Optional.<Container>absent());
        given(catalog.drop(any(Id.class))).willReturn(Optional.<Container>absent());
      }

      @Test
      public void should_deploy_assembled_container() throws Exception {
        final StubContainer created = StubContainer.create("created");
        given(d2rq.assemble(Id.valueOf("test"), ByteSource.empty())).willReturn(created);
        subject.deployFromD2rqMapping(Id.valueOf("test"), ByteSource.empty());
        verify(catalog).deploy(created);
      }

      @Test
      public void should_save_config_of_new_container() throws Exception {
        final ByteSource raw = ByteSource.wrap(Payload.randomWithLength(1024));
        subject.deployFromD2rqMapping(Id.valueOf("test"), raw);
        verify(store).save("test", Warden.D2RQ_SUFFIX, raw);
      }

      @Test
      public void should_activate_new_container() throws Exception {
        final StubContainer deployed = StubContainer.create("test");
        given(d2rq.assemble(Id.valueOf("test"), ByteSource.empty())).willReturn(deployed);
        subject.deployFromD2rqMapping(Id.valueOf("test"), ByteSource.empty());
        assertThat(deployed.isActivated(), equalTo(true));
      }

      @Test
      public void should_dispose_former_container() throws Exception {
        final StubContainer it = StubContainer.create("test");
        given(catalog.drop(Id.valueOf("test"))).willReturn(Optional.<Container>of(it));
        subject.deployFromD2rqMapping(Id.valueOf("test"), ByteSource.empty());
        assertThat("former container was not closed", it.isClosed(), equalTo(true));
      }

      @Test
      public void should_clear_config_of_former_container() throws Exception {
        given(catalog.drop(Id.valueOf("test")))
            .willReturn(Optional.<Container>of(StubContainer.create("test")));
        subject.deployFromD2rqMapping(Id.valueOf("test"), ByteSource.empty());
        verify(store).clear("test");
      }
    }

    public class Dispose {
      @Test
      public void should_return_false_if_target_container_is_not_deployed() throws Exception {
        given(catalog.drop(Id.valueOf("not-there"))).willReturn(Optional.<Container>absent());
        assertThat(subject.dispose(Id.valueOf("not-there")), equalTo(false));
      }

      @Test
      public void should_clear_config_even_if_no_container_was_present() throws Exception {
        given(catalog.drop(Id.valueOf("not-there"))).willReturn(Optional.<Container>absent());
        subject.dispose(Id.valueOf("not-there"));
        verify(store).clear("not-there");
      }

      @Test
      public void should_return_true_if_target_container_was_present() throws Exception {
        given(catalog.drop(Id.valueOf("test")))
            .willReturn(Optional.<Container>of(StubContainer.create("test")));
        catalog.deploy(StubContainer.create("test"));
        assertThat(subject.dispose(Id.valueOf("test")), equalTo(true));
      }

      @Test
      public void should_close_disposed_container() throws Exception {
        final StubContainer it = StubContainer.create("test");
        given(catalog.drop(Id.valueOf("test"))).willReturn(Optional.<Container>of(it));
        subject.dispose(Id.valueOf("test"));
        assertThat(it.isClosed(), equalTo(true));
      }

      @Test
      public void should_remove_stored_config_of_disposed_container() throws Exception {
        given(catalog.drop(Id.valueOf("test")))
            .willReturn(Optional.<Container>of(StubContainer.create("test")));
        subject.dispose(Id.valueOf("test"));
        verify(store).clear("test");
      }
    }

    public class Stop {
      @Test
      public void should_run_callback() throws Exception {
        final Runnable callback = Mockito.mock(Runnable.class);
        subject.stop(callback);
        verify(callback).run();
      }

      @Test
      public void should_not_be_running_after_stop() throws Exception {
        subject.stop(NOOP);
        assertThat(subject.isRunning(), equalTo(false));
      }

      @Test
      public void should_clear_catalog() throws Exception {
        subject.stop(NOOP);
        verify(catalog).clear();
      }

      @Test
      public void should_close_remaining_containers_from_catalog() throws Exception {
        final StubContainer it = StubContainer.create("test");
        given(catalog.clear()).willReturn(Collections.<Container>singleton(it));
        subject.stop(NOOP);
        assertThat("remaining container was not closed", it.isClosed(), equalTo(true));
      }
    }
  }
}
