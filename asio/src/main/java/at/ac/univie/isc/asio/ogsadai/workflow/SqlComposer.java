package at.ac.univie.isc.asio.ogsadai.workflow;

import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.deliverToStream;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.extractSchema;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.metadataToXml;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.sqlQuery;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.tupleToCsv;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.tupleToWebRowSetCharArrays;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeBuilder.pipe;
import uk.org.ogsadai.activity.workflow.ActivityPipelineWorkflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.ogsadai.OgsadaiFormats;
import at.ac.univie.isc.asio.ogsadai.WorkflowComposer;

public class SqlComposer implements WorkflowComposer {

	private final ResourceID resource;

	public SqlComposer(final ResourceID resource) {
		this.resource = resource;
	}

	@Override
	public ActivityPipelineWorkflow createFrom(final DatasetOperation operation) {
		final Action action = operation.action();
		PipeBuilder pipe;
		switch (action) {
			case QUERY:
				pipe = makeQuery(operation);
				break;
			case SCHEMA:
				pipe = makeSchema(operation);
				break;
			default:
				throw new UnsupportedOperationException("not implemented "
						+ action);
		}
		return pipe.finish(deliverToStream(operation.id()));
	}

	private PipeBuilder makeSchema(final DatasetOperation op) {
		assert !op.command().isPresent() : "command present in " + op;
		final PipeBuilder pipe = pipe(extractSchema(resource));
		final SerializationFormat format = op.format();
		if (format == OgsadaiFormats.XML) {
			pipe.into(metadataToXml());
		} else {
			throw new AssertionError("unsupported format " + format
					+ " for ogsadai schema extraction");
		}
		return pipe;
	}

	private PipeBuilder makeQuery(final DatasetOperation op) {
		final String query = op.commandOrFail();
		final PipeBuilder pipe = pipe(sqlQuery(resource, query));
		final SerializationFormat format = op.format();
		if (format == OgsadaiFormats.XML) {
			pipe.into(tupleToWebRowSetCharArrays());
		} else if (format == OgsadaiFormats.CSV) {
			pipe.into(tupleToCsv());
		} else {
			throw new AssertionError("unsupported format " + format
					+ " for ogsadai sql query ");
		}
		return pipe;
	}
}
