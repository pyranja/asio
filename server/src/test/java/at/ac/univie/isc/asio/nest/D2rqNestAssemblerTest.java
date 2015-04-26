package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.spring.SpringContextFactory;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import org.hamcrest.Matchers;
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
    assertThat(parsed.getMapping(), not(nullValue()));
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
  public void should_inject_d2rq_mapping() throws Exception {
    subject.inject(context, holder);
    verify(beanFactory).registerSingleton("mapping", holder.getMapping());
  }

  @Test
  public void should_inject_mapping_model_holder() throws Exception {
    subject.inject(context, holder);
    verify(beanFactory).registerSingleton("mappingModel", holder.getMappingModel());
  }

  // === clean mapping model

  private final Model input = ModelFactory.createDefaultModel();

  @Test
  public void should_provide_a_copy_of_the_input_model() throws Exception {
    final Model held = subject.cleanMappingModel(input);
    assertThat(held, not(sameInstance(input)));
    final Resource anon = input.createResource();
    assertThat(held.containsResource(anon), equalTo(false));
  }

  @Test
  public void should_keep_top_level_server_resources() throws Exception {
    final Resource server = input.createResource(D2RConfig.Server);
    final Model held = subject.cleanMappingModel(input);
    assertThat(held.containsResource(server), equalTo(true));
  }

  @Test
  public void should_hide_database_properties() throws Exception {
    final Resource original = input.createResource(D2RQ.Database);
    original.addProperty(D2RQ.jdbcDSN, "jdbc:mysql:///");
    original.addProperty(D2RQ.username, "username");
    original.addProperty(D2RQ.password, "password");
    final Model held = subject.cleanMappingModel(input);
    final Resource copied =
        Iterators.getOnlyElement(held.listResourcesWithProperty(RDF.type, D2RQ.Database));
    assertThat(Iterators.size(copied.listProperties()), equalTo(1));
    final Statement typeProperty = Iterators.getOnlyElement(copied.listProperties());
    assertThat(typeProperty.getPredicate(), equalTo(RDF.type));
    assertThat(typeProperty.getObject(), Matchers.<RDFNode>equalTo(D2RQ.Database));
    assertThat(copied, not(equalTo(original)));
  }

  @Test
  public void should_keep_prefixes_in_model_creator_method() throws Exception {
    input.setNsPrefix("my", "urn:test:my-prefix");
    final Model model = subject.cleanMappingModel(input);
    assertThat(model.getNsPrefixURI("my"), equalTo("urn:test:my-prefix"));
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
