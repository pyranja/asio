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
package at.ac.univie.isc.asio.metadata;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.Collections;

/**
 * Builder{identifier='defaults-descriptor-id', active=false, label='defaults-descriptor-id', description='no description', author='anonymous', license='public domain', category='general', created=2015-03-05T19:05:29.896Z, updated=2015-03-05T19:05:29.896Z, tags=[], links=[]} [group 2, item 1]
 * Builder{identifier='defaults-descriptor-id', active=false, label='defaults-descriptor-id', description='no description', author='anonymous', license='public domain', category='general', created=2015-03-05T19:05:29.896Z, updated=2015-03-05T19:05:29.896Z, tags=[], links=[]} [group 2, item 2]
 */
public class SchemaDescriptorTest {
  @Test
  public void ensure_sane_class() throws Exception {
    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    final ZonedDateTime notNow = now.plusDays(1);
    final ZonedDateTime nonUtcNow = now.withZoneSameInstant(ZoneOffset.ofHours(5));
    new EqualsTester()
        .addEqualityGroup(SchemaDescriptor.empty("empty-descriptor-id").build())
            // ensure defaults() sets fixed values
        .addEqualityGroup(
            SchemaDescriptor.defaults("defaults-descriptor-id")
                .withCreated(now).withUpdated(now)
                .build()
            , SchemaDescriptor.empty("defaults-descriptor-id")
                .withActive(false)
                .withLabel("defaults-descriptor-id")
                .withDescription("no description").withCategory("general")
                .withAuthor("anonymous").withLicense("public domain")
                .withCreated(now).withUpdated(now)
                .withTags(Collections.<String>emptyList()).withLinks(Collections.<Link>emptyList())
                .build()
            , SchemaDescriptor.create("defaults-descriptor-id", false, "defaults-descriptor-id",
                "no description", "anonymous", "public domain", "general", now, now,
                Collections.<String>emptyList(), Collections.<Link>emptyList())
        )
            // different times
        .addEqualityGroup(
            SchemaDescriptor.defaults("defaults-descriptor-id").withCreated(notNow).build())
        .addEqualityGroup(
            SchemaDescriptor.defaults("defaults-descriptor-id").withUpdated(notNow).build())
            // factory method defaults and normalization
        .addEqualityGroup(
            SchemaDescriptor.create("test-id", true, null, null, null, null, null, nonUtcNow, nonUtcNow, null, null)
            , SchemaDescriptor.empty("test-id")
                .withActive(true).withLabel("test-id")
                .withCreated(now).withUpdated(now)
                .withTags(Collections.<String>emptyList())
                .withLinks(Collections.<Link>emptyList())
                .build()
        )
        .testEquals();
  }
}
