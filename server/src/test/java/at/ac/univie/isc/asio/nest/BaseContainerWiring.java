package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * assertions on beans that must always be present in a container
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("wiring-test")
public abstract class BaseContainerWiring extends AbstractJUnit4SpringContextTests {

  @Configuration
  @Profile("wiring-test")
  static class BaseWiringConfig {
    @Bean
    public Mapping d2rqMapping() {
      final Mapping mapping = new Mapping();
      mapping.addDatabase(new Database(ResourceFactory.createResource()));
      return mapping;
    }

    @Bean
    public Model mappingModel() {
      return ModelFactory.createDefaultModel();
    }

    @Bean
    public Timeout globalTimeout() {
      return Timeout.from(100, TimeUnit.MILLISECONDS);
    }
  }

  @Test
  public void must_configure_sql_engine() throws Exception {
    final Map<String, Engine> engines = applicationContext.getBeansOfType(Engine.class);
    final boolean present = Iterables.any(engines.values(), new EnginePredicate(Language.SQL));
    assertThat("sql engine missing", present, equalTo(true));
  }

  @Test
  public void must_configure_sparql_engine() throws Exception {
    final Map<String, Engine> engines = applicationContext.getBeansOfType(Engine.class);
    final boolean present = Iterables.any(engines.values(), new EnginePredicate(Language.SPARQL));
    assertThat("sparql engine missing", present, equalTo(true));
  }

  @Test
  public void must_configure_dataset_definition() throws Exception {
    applicationContext.getBean(NestBluePrint.BEAN_DEFINITION_SOURCE, Observable.class);
  }

  @Test
  public void must_configure_dataset_descriptor() throws Exception {
    applicationContext.getBean(NestBluePrint.BEAN_DESCRIPTOR_SOURCE, Observable.class);
  }
}
