package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.transport.Transfer;

public interface Operator {

  void invoke(DatasetOperation operation, Transfer exchange, OperatorCallback callback);
}
