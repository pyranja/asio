package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.ConfigStore;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.io.TransientFolder;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.io.ByteSource;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.nio.file.Files;
import java.util.Collection;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContainerResourceTest {
  private final Emitter events = Mockito.mock(Emitter.class);
  private final Catalog<Container> catalog = new Catalog<>(events, Timeout.undefined());
  private final ConfigStore store = Mockito.mock(ConfigStore.class);
  private Assembler assembler = Mockito.mock(Assembler.class);
  private ContainerResource subject = new ContainerResource(catalog, store, assembler);

  // === REST api

  @Test
  public void should_respond_with_OK_if_disposed_successfully() throws Exception {
    catalog.deploy(StubContainer.create("test"));
    final Response response = subject.deleteContainer(Id.valueOf("test"));
    assertThat(response, hasStatus(Response.Status.OK));
  }

  @Test
  public void should_respond_with_NOT_FOUND_if_target_not_found_on_dispose() throws Exception {
    final Response response = subject.deleteContainer(Id.valueOf("test"));
    assertThat(response, hasStatus(Response.Status.NOT_FOUND));
  }

  @Rule
  public final TransientFolder temp = TransientFolder.create();

  @Test
  public void should_respond_with_CREATED_if_deployed_successfully() throws Exception {
    when(assembler.assemble(Mockito.eq(Id.valueOf("test")), Mockito.any(ByteSource.class)))
        .thenReturn(StubContainer.create("test"));
    final Response response = subject.createD2rqContainer(Id.valueOf("test"),
        Files.createTempFile(temp.path(), "test", ".dat").toFile());
    assertThat(response, hasStatus(Response.Status.CREATED));
  }

  @Test
  public void should_respond_with_list_of_deployed_container_ids() throws Exception {
    catalog.deploy(StubContainer.create("test"));
    final Collection<Id> names = subject.listContainers();
    assertThat(names, contains(Id.valueOf("test")));
  }

  @Test
  public void should_respond_with_deployed_container_info() throws Exception {
    final Container container = StubContainer.create("test");
    catalog.deploy(container);
    final Response response = subject.findContainer(Id.valueOf("test"));
    assertThat(response, hasStatus(Response.Status.OK));
    assertThat(response.getEntity(), Matchers.<Object>equalTo(container));
  }

  @Test
  public void should_respond_with_NOT_FOUND_if_container_not_present() throws Exception {
    final Response response = subject.findContainer(Id.valueOf("test"));
    assertThat(response, hasStatus(Response.Status.NOT_FOUND));
  }

  // === close

  @Test
  public void should_clear_catalog_on_close() throws Exception {
    subject.close();
    assertThat("catalog was not cleared", catalog.isClosed(), equalTo(true));
  }

  @Test
  public void should_close_remaining_containers_from_catalog_on_close() throws Exception {
    final StubContainer container = StubContainer.create("test");
    catalog.deploy(container);
    subject.close();
    assertThat("remaining container was not closed", container.isClosed(), equalTo(true));
  }

  // === dispose

  @Test
  public void should_return_false_if_target_container_not_deployed_when_disposing() throws Exception {
    assertThat(subject.dispose(Id.valueOf("not-there")), equalTo(false));
  }

  @Test
  public void should_clear_config_even_if_no_container_was_present() throws Exception {
    subject.dispose(Id.valueOf("not-there"));
    verify(store).clear("not-there");
  }

  @Test
  public void should_return_true_if_target_container_was_present_on_dispose() throws Exception {
    catalog.deploy(StubContainer.create("test"));
    assertThat(subject.dispose(Id.valueOf("test")), equalTo(true));
  }

  @Test
  public void should_close_disposed_container() throws Exception {
    final StubContainer container = StubContainer.create("test");
    catalog.deploy(container);
    subject.dispose(Id.valueOf("test"));
    assertThat(container.isClosed(), equalTo(true));
  }

  @Test
  public void should_remove_stored_config_of_disposed_container() throws Exception {
    final StubContainer container = StubContainer.create("test");
    catalog.deploy(container);
    subject.dispose(Id.valueOf("test"));
    verify(store).clear("test");
  }

  // === deploy

  @Test
  public void should_drop_then_deploy_if_replacing() throws Exception {
    catalog.deploy(StubContainer.create("test"));
    Mockito.reset(events);
    subject.deploy(StubContainer.create("test"), ByteSource.empty());
    verify(events).emit(isA(ContainerEvent.Dropped.class));
    verify(events).emit(isA(ContainerEvent.Deployed.class));
  }

  @Test
  public void should_dispose_former_container_if_replacing() throws Exception {
    final StubContainer former = StubContainer.create("test");
    catalog.deploy(former);
    subject.deploy(StubContainer.create("test"), ByteSource.empty());
    assertThat(former.isClosed(), equalTo(true));
  }

  @Test
  public void should_clear_config_of_schema() throws Exception {
    subject.deploy(StubContainer.create("test"), ByteSource.empty());
    verify(store).clear("test");
  }

  @Test
  public void should_save_config_of_new_container() throws Exception {
    final ByteSource raw = ByteSource.wrap(Payload.randomWithLength(1024));
    subject.deploy(StubContainer.create("test"), raw);
    verify(store).save("test", "config", raw);
  }

  @Test
  public void should_active_new_container() throws Exception {
    final StubContainer deployed = StubContainer.create("test");
    subject.deploy(deployed, ByteSource.empty());
    assertThat(deployed.isRunning(), equalTo(true));
  }

  @Test
  public void should_deploy_container_from_assembler() throws Exception {
    final StubContainer created = StubContainer.create("created");
    Mockito.reset(events);
    final ArgumentCaptor<ContainerEvent> captor = ArgumentCaptor.forClass(ContainerEvent.class);
    subject.deploy(created, ByteSource.empty());
    verify(events).emit(captor.capture());
    final ContainerEvent event = captor.getValue();
    assertThat(event, instanceOf(ContainerEvent.Deployed.class));
    assertThat(event.getName(), equalTo(Id.valueOf("created")));
    assertThat(event.getContainer(), Matchers.<Container>sameInstance(created));
  }
}
