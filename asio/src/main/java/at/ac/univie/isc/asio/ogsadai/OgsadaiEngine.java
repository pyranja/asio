package at.ac.univie.isc.asio.ogsadai;

import static at.ac.univie.isc.asio.ogsadai.PipeActivities.deliverToStream;
import static at.ac.univie.isc.asio.ogsadai.PipeActivities.sqlQuery;
import static at.ac.univie.isc.asio.ogsadai.PipeActivities.tupleToWebRowSetCharArrays;
import static at.ac.univie.isc.asio.ogsadai.PipeBuilder.pipe;
import static com.google.common.base.Strings.emptyToNull;

import java.io.IOException;
import java.io.OutputStream;

import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.transport.FileResult;
import at.ac.univie.isc.asio.transport.FileResultRepository;

public class OgsadaiEngine implements DatasetEngine {

	private final OgsadaiAdapter ogsadai;
	private final FileResultRepository results;
	private final ResourceID resource;

	OgsadaiEngine(final OgsadaiAdapter ogsadai,
			final FileResultRepository results, final ResourceID resource) {
		super();
		this.ogsadai = ogsadai;
		this.results = results;
		this.resource = resource;
	}

	@Override
	public FileResult submit(final String query) {
		validateQuery(query);
		final FileResult handler = results.newResult();
		try (OutputStream resultSink = handler.getOutput();) {
			final String streamId = ogsadai.register(resultSink);
			final Workflow workflow = createWorkflow(query, streamId);
			ogsadai.executeSynchronous(workflow);
		} catch (final IOException e) {
			throw new DatasetTransportException(e);
		}
		return handler;
	}

	private Workflow createWorkflow(final String query, final String streamId) {
		return pipe(sqlQuery(resource, query)).into(
				tupleToWebRowSetCharArrays()).finish(deliverToStream(streamId));
	}

	private void validateQuery(final String query) {
		if (emptyToNull(query) == null) {
			throw new DatasetUsageException("invalid query \"" + query + "\"");
		}
	}
}
