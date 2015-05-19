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
package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.spring.SpringContextFactory;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import java.net.URI;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * tests the package-private helper methods, as mocking the raw config data is complex
 */
public class D2rqNestAssemblerTest {
  @Rule
  public final ExpectedException error = ExpectedException.none();

  private final SpringContextFactory factory =
      new SpringContextFactory(new StaticApplicationContext());

  private final D2rqNestAssembler subject =
      new D2rqNestAssembler(factory, Collections.<Configurer>emptyList(), Collections.<OnClose>emptyList());

  // === parse configuration model

  @Test
  public void should_parse_minimal_d2rq_model_into_bean_holder() throws Exception {
    // test with minimal accepted config model
    final Model model = ModelFactory.createDefaultModel();
    model.createResource("urn:test:server", D2RConfig.Server);
    model.createResource("", D2RQ.Database).addProperty(D2RQ.jdbcDSN, "jdbc:test://localhost/");
    final NestConfig parsed = subject.parse(model);
    assertThat(parsed.getDataset().getIdentifier(), equalTo(URI.create("urn:test:server")));
    assertThat(parsed.getJdbc().getUrl(), equalTo("jdbc:test://localhost/"));
    assertThat(parsed.getD2rq(), not(nullValue()));
  }

  // === post process configuration

  @Test
  public void should_apply_all_configurers() throws Exception {
    final Configurer first = Mockito.mock(Configurer.class);
    final Configurer second = Mockito.mock(Configurer.class);
    final NestConfig config = NestConfig.empty();
    config.getDataset().setName(Id.valueOf("test"));
    subject.postProcess(config, Lists.newArrayList(first, second));
    verify(first).apply(Mockito.any(NestConfig.class));
    verify(second).apply(Mockito.any(NestConfig.class));
  }

  @Test
  public void should_fail_fast_if_dataset_name_not_set() throws Exception {
    error.expect(AssertionError.class);
    subject.postProcess(NestConfig.empty(), Collections.<Configurer>emptyList());
  }

  // === injection

  private final DefaultListableBeanFactory beanFactory =
      Mockito.mock(DefaultListableBeanFactory.class);
  private final AnnotationConfigApplicationContext context =
      Mockito.spy(new AnnotationConfigApplicationContext(beanFactory));
  private final NestConfig holder = NestConfig.empty();

  @Test
  public void should_build_from_nest_blue_print() throws Exception {
    subject.inject(context, NestConfig.empty());
    verify(context).register(NestBluePrint.class);
  }

  @Test
  public void should_inject_config_beans() throws Exception {
    subject.inject(context, holder);
    verify(beanFactory).registerSingleton("dataset", holder.getDataset());
    verify(beanFactory).registerSingleton("jdbc", holder.getJdbc());
  }

  @Test
  public void should_inject_d2rq_config() throws Exception {
    subject.inject(context, holder);
    verify(beanFactory).registerSingleton("d2rq", holder.getD2rq());
  }

  // === optional listener wiring

  @Test
  public void should_create_singleton_assembler_if_no_optional_listeners_present() throws Exception {
    final StaticApplicationContext parent = new StaticApplicationContext();
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().addActiveProfile("brood");
    context.getBeanFactory().registerSingleton("factory", new SpringContextFactory(parent));
    context.register(D2rqNestAssembler.class);
    context.refresh();
    final D2rqNestAssembler actual = context.getBean(D2rqNestAssembler.class);
    assertThat(actual, notNullValue());
  }
}
