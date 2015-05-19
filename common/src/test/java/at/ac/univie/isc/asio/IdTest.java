/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class IdTest {
  @Rule
  public final ExpectedException error = ExpectedException.none();

  @Test
  public void should_reject_empty_id() throws Exception {
    error.expect(Id.IllegalIdentifier.class);
    Id.valueOf("");
  }

  @Test
  public void should_reject_null() throws Exception {
    error.expect(NullPointerException.class);
    Id.valueOf(null);
  }

  @Test
  public void should_reject_leading_hyphen() throws Exception {
    error.expect(Id.IllegalIdentifier.class);
    Id.valueOf("-test");
  }

  @Test
  public void should_reject_trailing_hyphen() throws Exception {
    error.expect(Id.IllegalIdentifier.class);
    Id.valueOf("test-");
  }

  @Test
  public void should_accept_simple_identifier() throws Exception {
    assertThat(Id.valueOf("test").asString(), equalTo("test"));
  }

  @Test
  public void should_convert_to_lower_case() throws Exception {
    assertThat(Id.valueOf("TeSt").asString(), equalTo("test"));
  }

  @Test
  public void should_accept_identifier_with_enclosed_hyphen() throws Exception {
    Id.valueOf("te-st");
  }

  @Test
  public void should_accept_identifier_with_leading_underscore() throws Exception {
    Id.valueOf("_test");
  }

  @Test
  public void should_accept_identifier_with_trailing_underscore() throws Exception {
    Id.valueOf("test_");
  }

  @Test
  public void should_accept_underscore_as_identifier() throws Exception {
    Id.valueOf("_");
  }

  @Test
  public void should_reject_special_characters() throws Exception {
    error.expect(Id.IllegalIdentifier.class);
    Id.valueOf("te/st");
  }

  @Test
  public void should_accept_numeric_identifier() throws Exception {
    assertThat(Id.valueOf("1234").asString(), equalTo("1234"));
  }
}
