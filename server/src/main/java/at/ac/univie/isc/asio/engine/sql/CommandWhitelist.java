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

import at.ac.univie.isc.asio.Scope;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Restrict executable sql queries to a fixed set of white listed commands.
 */
public final class CommandWhitelist implements Predicate<String> {
  private static final Logger log = getLogger(CommandWhitelist.class);

  /**
   * Create a filter that allows all given commands.
   */
  public static Predicate<String> allowOnly(final Iterable<String> allowed) {
    return new CommandWhitelist(allowed);
  }

  /**
   * Create a noop filter, that allows any command.
   */
  public static Predicate<String> any() {
    return Predicates.alwaysTrue();
  }

  private static final Pattern SQL_COMMAND =
      Pattern.compile("^\\s*(?<command>\\w+)(?:\\s+.*|$)", Pattern.DOTALL);

  private final Set<String> whitelist;

  private CommandWhitelist(final Iterable<String> allowed) {
    whitelist = new HashSet<>();
    for (String each : allowed) {
      whitelist.add(each.toUpperCase(Locale.ENGLISH));
    }
    if (whitelist.isEmpty()) {
      log.warn(Scope.SYSTEM.marker(), "no allowed sql commands configured - will reject all sql operations");
    }
  }

  @Override
  public boolean apply(final String input) {
    final Matcher matcher = SQL_COMMAND.matcher(input);
    if (matcher.matches()) {
      final String found = matcher.group("command");
      return whitelist.contains(found.toUpperCase(Locale.ENGLISH));
    }
    return false;
  }
}
