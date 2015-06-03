/*
 * #%L
 * asio cli
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
package at.ac.univie.isc.asio.tool;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class JvmOptionsTest {

  public static class Default {

    private final Collection<String> args = JvmOptions.create().collect();

    @Test
    public void should_force_server_vm() throws Exception {
      assertThat(args, hasItem("-server"));
    }

    @Test
    public void should_not_set_ipstack_preferences() throws Exception {
      assertThat(args, everyItem(not(containsString("java.net.prefer"))));
    }

    @Test
    public void should_not_modify_heap_settings() throws Exception {
      assertThat(args, everyItem(not(containsString("-Xms"))));
      assertThat(args, everyItem(not(containsString("-Xmx"))));
    }

    @Test
    public void should_enable_heap_diagnostics() throws Exception {
      assertThat(args, hasItems(
          equalTo("-XX:+HeapDumpOnOutOfMemoryError"),
          startsWith("-XX:HeapDumpPath="))
      );
    }

    @Test
    public void should_enable_gc_diagnostics() throws Exception {
      assertThat(args, hasItems(
          equalTo("-XX:+PrintGCDateStamps"),
          equalTo("-verbose:gc"),
          equalTo("-XX:+PrintGCDetails"),
          startsWith("-Xloggc:"),
          equalTo("-XX:+UseGCLogFileRotation"),
          equalTo("-XX:NumberOfGCLogFiles=4"),
          equalTo("-XX:GCLogFileSize=10M")
      ));
    }

    @Test
    public void should_set_dns_cache_ttl() throws Exception {
      assertThat(args, hasItem(startsWith("-Dsun.net.inetaddr.ttl=")));
    }
  }

  @Test
  public void should_prefer_IPv4_if_enabled() throws Exception {
    assertThat(JvmOptions.create().forceIPv4().collect(),
        hasItems("-Djava.net.preferIPv4Stack=true", "-Djava.net.preferIPv6Addresses=false"));
  }

  @Test
  public void should_set_heap_size_if_size_hint_given() throws Exception {
    assertThat(JvmOptions.create().relativeHeapSize(0.5).collect(),
        hasItems(startsWith("-Xms"), startsWith("-Xmx")));
  }
}
