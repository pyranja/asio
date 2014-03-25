package at.ac.univie.isc.asio.jena;

import java.io.OutputStream;

import com.google.common.base.Function;
import com.hp.hpl.jena.query.QueryExecution;

interface QueryModeHandler<RESULT> extends Function<QueryExecution, RESULT> {

  @Override
  RESULT apply(QueryExecution execution);

  void serialize(OutputStream sink, RESULT data);
}
