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
