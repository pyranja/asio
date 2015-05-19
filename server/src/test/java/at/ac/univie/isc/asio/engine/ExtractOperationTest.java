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
import at.ac.univie.isc.asio.tool.ValueOrError;
import org.junit.Test;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ExtractOperationTest {
  private final ExtractOperation subject = ExtractOperation.expect(Language.valueOf("language"));
  private ValueOrError<String> extracted;

  @Test
  public void should_parse_valid_operation() throws Exception {
    extracted = subject.from(MediaType.valueOf("*/language-operation"));
    assertThat(extracted.get(), is("operation"));
  }

  @Test
  public void should_fail_on_null_mime() throws Exception {
    extracted = subject.from(null);
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  @Test
  public void should_fail_on_language_mismatch() throws Exception {
    extracted = subject.from(MediaType.valueOf("*/illegal-operation"));
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  @Test
  public void should_fail_on_malformed_mime() throws Exception {
    extracted = subject.from(MediaType.WILDCARD_TYPE);
    assertThat(extracted.error(), is(instanceOf(NotSupportedException.class)));
  }

  private final Pattern p = ExtractOperation.MEDIA_SUBTYPE_PATTERN;

  @Test
  public void should_find_language() throws Exception {
    final String[] parsed = apply("TEST-QUERY");
    assertThat(parsed[0], is("TEST"));
  }

  @Test
  public void should_identify_query() throws Exception {
    final String[] parsed = apply("TEST-QUERY");
    assertThat(parsed[1], is("QUERY"));
  }

  @Test
  public void should_identify_update() throws Exception {
    final String[] parsed = apply("TEST-UPDATE");
    assertThat(parsed[1], is("UPDATE"));
  }

  private String[] apply(final String input) {
    final Matcher match = p.matcher(input);
    assertThat(match.matches(), is(true));
    assertThat(match.groupCount(), is(2));
    return new String[] {match.group(1), match.group(2)};
  }
}
