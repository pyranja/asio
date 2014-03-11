package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

public class MockOperations {

  public static final String TEST_ID = "test-id";

  public static DatasetOperation query(final String query, final SerializationFormat format) {
    return new DatasetOperation(TEST_ID, Action.QUERY, query, format);
  }

  public static DatasetOperation schema(final SerializationFormat format) {
    return new DatasetOperation(TEST_ID, Action.SCHEMA, null, format);
  }

  public static DatasetOperation update(final String update, final SerializationFormat format) {
    return new DatasetOperation(TEST_ID, Action.UPDATE, update, format);
  }

  private MockOperations() {}
}
