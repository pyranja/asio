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
package at.ac.univie.isc.asio.engine.sql;

import com.google.common.base.Predicate;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class CommandWhitelistTest {

  @DataPoints
  public static final String[] WHITESPACES = new String[] { " ", "  ", "\n", "", "\t", "\r", "\f" };
  @DataPoint
  public static final String WHITESPACED_WORD = " a-word ";
  @DataPoints
  public static final String[] WHITELIST = new String[] { "ALLOWED", "gagamat" };
  @DataPoints
  public static final String[] ILLEGAL = new String[] { "FORBIDDEN", "with whitespace", "illegal-character" };

  private final Predicate<String> subject = CommandWhitelist.allowOnly(Arrays.asList(WHITELIST));

  @Theory
  public void should_ignore_white_space_arround_command(final String prefix, final String command, final String suffix, final String tail) {
    assumeThat(prefix, equalToIgnoringWhiteSpace(""));
    assumeThat(command, not(equalToIgnoringWhiteSpace("")));
    final String ending = suffix + tail;
    assumeThat(ending, either(equalToIgnoringWhiteSpace("")).or(startsWith(" ")));
    final boolean isAllowed = Arrays.asList(WHITELIST).contains(command);
    assertThat(subject.apply(prefix + command + suffix + tail), equalTo(isAllowed));
  }

  @Theory
  public void should_reject_allowed_command_as_substring(final String command) {
    assertThat(subject.apply("surrounding" + command + "string"), equalTo(false));
  }
}
