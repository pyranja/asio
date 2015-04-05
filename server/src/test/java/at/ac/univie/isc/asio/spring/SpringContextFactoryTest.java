package at.ac.univie.isc.asio.spring;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SpringContextFactoryTest {
  private final StaticApplicationContext root = new StaticApplicationContext();
  private final SpringContextFactory subject = new SpringContextFactory(root);

  @Test
  public void should_create_dormant_context() throws Exception {
    assertThat(subject.named("test").isActive(), equalTo(false));
  }

  @Test
  public void should_use_current_context_as_parent() throws Exception {
    assertThat(subject.named("test").getParent(), Matchers.<ApplicationContext>sameInstance(root));
  }

  @Test
  public void should_assign_unique_ids() throws Exception {
    final ConfigurableApplicationContext first = subject.named("test");
    final ConfigurableApplicationContext second = subject.named("test");
    assertThat(first.getId(), not(equalTo(second.getId())));
  }

  @Test
  public void should_use_given_label_as_display_name() throws Exception {
    assertThat(subject.named("label").getDisplayName(), equalTo("label"));
  }

  @Test
  public void should_include_label_in_id() throws Exception {
    assertThat(subject.named("label").getId(), containsString("label"));
  }

  @Test
  public void should_include_parent_id_in_id() throws Exception {
    root.setId("parent-id");
    assertThat(subject.named("test").getId(), containsString("parent-id"));
  }
}
