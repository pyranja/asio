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
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class FixedSelectionTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Engine sql = Mockito.mock(Engine.class);
  private final Engine sparql = Mockito.mock(Engine.class);

  private FixedSelection subject;

  @Before
  public void setupMockEngines() {
    when(sql.language()).thenReturn(Language.SQL);
    when(sparql.language()).thenReturn(Language.SPARQL);
    subject = FixedSelection.from(ImmutableSet.of(sql, sparql));
  }

  @Test
  public void should_yield_single_engine_that_supports_language_of_given_command() throws Exception {
    final Engine selected = subject.select(CommandBuilder.empty().language(Language.SQL).build());
    assertThat(selected, is(sql));
  }

  @Test
  public void should_fail_if_no_engine_supports_given_command() throws Exception {
    error.expect(Language.NotSupported.class);
    subject.select(CommandBuilder.empty().language(Language.UNKNOWN).build());
  }

  @Test
  public void should_fail_fast_if_constructed_with_multiple_engines_for_a_single_engine() throws Exception {
    final Engine duplicate = Mockito.mock(Engine.class);
    when(duplicate.language()).thenReturn(Language.SQL);
    error.expect(IllegalArgumentException.class);
    FixedSelection.from(ImmutableSet.of(sql, duplicate));
  }
}
