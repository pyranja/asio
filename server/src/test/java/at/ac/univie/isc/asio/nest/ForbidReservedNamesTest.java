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

import at.ac.univie.isc.asio.Id;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public final class ForbidReservedNamesTest {

  @Test
  public void should_keep_config_as_is_if_not_reserved() throws Exception {
    final ForbidReservedNames subject =
        new ForbidReservedNames(Collections.<Id>emptyList());
    final NestConfig input = NestConfig.empty();
    input.getDataset().setName(Id.valueOf("test"));
    assertThat(subject.apply(input), equalTo(input));
  }

  @Test(expected = ForbidReservedNames.IllegalContainerName.class)
  public void should_reject_config_with_reserved_name() throws Exception {
    final ForbidReservedNames subject =
        new ForbidReservedNames(Collections.singletonList(Id.valueOf("reserved")));
    final NestConfig input = NestConfig.empty();
    input.getDataset().setName(Id.valueOf("reserved"));
    subject.apply(input);
  }
}
