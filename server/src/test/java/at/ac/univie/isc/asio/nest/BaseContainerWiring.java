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

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.zaxxer.hikari.HikariConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
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
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HikariConfig baseHikariConfig(final Timeout timeout) {
      final HikariConfig base = new HikariConfig();
      base.setConnectionTimeout(timeout.getAs(TimeUnit.MILLISECONDS, 0));
      return base;
    }

    @Bean
    public D2rqConfigModel d2rq() {
      return D2rqConfigModel.wrap(ModelFactory.createDefaultModel());
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
