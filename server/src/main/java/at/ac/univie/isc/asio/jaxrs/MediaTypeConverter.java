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
package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;

/**
 * Convert {@link javax.ws.rs.core.MediaType JAX-RS media types} into
 * {@link com.google.common.net.MediaType Guava media types} and vice versa.
 *
 * Note: Converting media types with duplicated parameters is not supported.
 */
@ThreadSafe
public final class MediaTypeConverter
    extends Converter<javax.ws.rs.core.MediaType, com.google.common.net.MediaType> {
  /** singleton */
  private static final MediaTypeConverter INSTANCE = new MediaTypeConverter();

  /**
   * @return a MediaType converter instance
   */
  public static MediaTypeConverter instance() {
    return INSTANCE;
  }

  @Override
  protected com.google.common.net.MediaType doForward(@Nonnull final javax.ws.rs.core.MediaType input) {
    return com.google.common.net.MediaType.create(input.getType(), input.getSubtype())
        .withParameters(Multimaps.forMap(input.getParameters()));
  }

  @Override
  protected javax.ws.rs.core.MediaType doBackward(@Nonnull final com.google.common.net.MediaType input) {
    final ImmutableMap.Builder<String, String> parameters = ImmutableMap.builder();
    for (Map.Entry<String, Collection<String>> entry : input.parameters().asMap().entrySet()) {
      assert entry.getValue().size() <= 1 : "cannot convert duplicate parameters in "+ input;
      parameters.put(entry.getKey(), entry.getValue().iterator().next());
    }
    return new javax.ws.rs.core.MediaType(input.type(), input.subtype(), parameters.build());
  }

  private MediaTypeConverter() {}
}
