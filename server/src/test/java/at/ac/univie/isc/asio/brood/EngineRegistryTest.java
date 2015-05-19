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
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.CommandBuilder;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Identity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class EngineRegistryTest {
  @Rule
  public ExpectedException error = ExpectedException.none();
  private final EngineRegistry subject = new EngineRegistry();

  @Test
  public void should_fail_if_schema_not_found() throws Exception {
    error.expect(Id.NotFound.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_fail_if_language_not_supported() throws Exception {
    subject.onDeploy(new ContainerEvent.Deployed(StubContainer.create("default")));
    error.expect(Language.NotSupported.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_find_engine_identified_by_schema_and_language() throws Exception {
    final Engine expected = new StubEngine();
    final Container container = StubContainer.create("default").withEngine(expected);
    subject.onDeploy(new ContainerEvent.Deployed(container));
    final Engine selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(expected));
  }

  @Test
  public void should_forget_undeployed_schemas() throws Exception {
    final Container container = StubContainer.create("default").withEngine(new StubEngine());
    subject.onDeploy(new ContainerEvent.Deployed(container));
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    subject.onDrop(new ContainerEvent.Dropped(container));
    error.expect(Id.NotFound.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_replace_if_deployed_already_present() throws Exception {
    final Engine first = new StubEngine();
    final Engine second = new StubEngine();
    Engine selected;
    subject.onDeploy(new ContainerEvent.Deployed(StubContainer.create("default").withEngine(first)));
    selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(first));
    subject.onDeploy(new ContainerEvent.Deployed(StubContainer.create("default").withEngine(second)));
    selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(second));
  }

  private Command command(final Id id, final Language language) {
    return CommandBuilder.empty().language(language)
        .single(Command.KEY_SCHEMA, id.asString())
        .owner(Identity.undefined())
        .build();
  }

  private static class StubEngine implements Engine {
    @Override
    public Language language() {
      return Language.UNKNOWN;
    }

    @Override
    public Invocation prepare(final Command command) {
      throw new UnsupportedOperationException("fake");
    }

    @Override
    public void close() {

    }
  }
}
