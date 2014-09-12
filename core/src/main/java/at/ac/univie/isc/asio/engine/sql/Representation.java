package at.ac.univie.isc.asio.engine.sql;

import com.google.common.base.Function;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
interface Representation extends Function<Object, String> {

  /**
   * Create a canonical representation of the given input.
   *
   * @param input value to format
   * @return canonical representation of {@code input}
   */
  @Override
  String apply(Object input);
}
