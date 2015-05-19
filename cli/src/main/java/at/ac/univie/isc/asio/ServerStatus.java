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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.TypedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ServerStatus extends TypedValue<String> {
  public static final ServerStatus UP = ServerStatus.valueOf("UP");
  public static final ServerStatus DOWN = ServerStatus.valueOf("DOWN");

  @JsonCreator
  public static ServerStatus valueOf(@JsonProperty("status") final String val) {
    if (val == null) {
      return DOWN;
    } else {
      return new ServerStatus(val);
    }
  }

  private ServerStatus(@Nonnull final String val) {
    super(val);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toUpperCase(Locale.ENGLISH).trim();
  }
}
