/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.sql;

import com.google.common.collect.AbstractIterator;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Turns a ResultSet into an Iterator using a given {@link at.ac.univie.isc.asio.sql.ResultSetIterator.RowConverter} to process each row of the
 * ResultSet into an object.
 * 
 * @author Chris Borckholder
 * @param <T> type of converted rows
 */
public final class ResultSetIterator<T> extends AbstractIterator<T> implements AutoCloseable {

  /**
   * Create an Iterator over the given {@link java.sql.ResultSet}.
   * 
   * @param resultSet will be iterated
   * @param converter converts each row of resultSet to an object
   * @return the iterator
   */
  public static <T> ResultSetIterator<T> from(final ResultSet resultSet,
      final RowConverter<? extends T> converter) {
    return new ResultSetIterator<T>(resultSet, converter);
  }

  /**
   * Convert the given {@link java.sql.ResultSet} to an {@link Iterable} over the conversion results of the
   * given {@link at.ac.univie.isc.asio.sql.ResultSetIterator.RowConverter}. <strong>Caution:</strong>This iterable can only be traversed once!
   * 
   * @return an Iterable suitable for for-each constructs
   */
  public static <T> Iterable<T> asIterable(final ResultSet resultSet,
      final RowConverter<? extends T> converter) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new ResultSetIterator<T>(resultSet, converter);
      }
    };
  }

  /**
   * Defines a conversion function for individual ResultSet rows.
   * 
   * @author Chris Borckholder
   * @param <ROW> type of conversion results
   */
  // @FunctionalInterface - java8
  public static interface RowConverter<ROW> {
    /**
     * Convert the given ResultSet to an object of type ROW. Should not modify or advance the given
     * ResultSet. Implementations should <strong>never</strong> produce null as conversion result.
     * 
     * @param resultSet set to current row
     * @return result of conversion (non-null)
     * @throws java.sql.SQLException occurring while interacting with the ResultSet
     */
    @Nonnull
    ROW process(@Nonnull ResultSet resultSet) throws SQLException;
  }

  /**
   * Thrown if any (Sql)Exception occurs while iterating the ResultSet.
   * 
   * @author Chris Borckholder
   */
  public static class ResultSetIterationException extends IllegalStateException {
    private static final long serialVersionUID = 7447190500968931002L;

    public ResultSetIterationException(final Throwable cause) {
      super(cause);
    }
  }

  private final ResultSet rs;
  private final RowConverter<? extends T> converter;

  private ResultSetIterator(final ResultSet resultSet, final RowConverter<? extends T> converter) {
    super();
    this.rs = checkNotNull(resultSet, "cannot iterate null result set");
    this.converter = checkNotNull(converter, "cannot use null function as converter");
  }

  @Override
  protected T computeNext() {
    try {
      if (rs.next()) {
        return converter.process(rs);
      } else {
        return endOfData();
      }
    } catch (final SQLException e) {
      throw new ResultSetIterationException(e);
    }
  }

  /**
   * Closes the {@link java.sql.ResultSet} that was/is iterated.
   */
  @Override
  public void close() throws SQLException {
    rs.close();
  }

}
