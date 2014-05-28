package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;

public interface OperatorCallback {

  public static enum Phase {
    /** command was successfully executed on the dataset */
    EXECUTION,
    /** results of execution were serialized in the required format */
    SERIALIZATION,
    /** serialized results were transported to the recipient */
    DELIVERY;
  }

  void completed(Phase completed);

  void fail(DatasetException cause);
}
