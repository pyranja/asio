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
