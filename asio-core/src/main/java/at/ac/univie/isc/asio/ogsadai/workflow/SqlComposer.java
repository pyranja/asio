package at.ac.univie.isc.asio.ogsadai.workflow;

import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.deliverToStream;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.dynamicSerializer;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.extractSchema;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.metadataToXml;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.sqlQuery;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.sqlUpdate;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.tupleToCsv;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeActivities.tupleToWebRowSetCharArrays;
import static at.ac.univie.isc.asio.ogsadai.workflow.PipeBuilder.pipe;

import java.io.OutputStream;

import uk.org.ogsadai.activity.transform.BlockTransformer;
import uk.org.ogsadai.activity.workflow.ActivityPipelineWorkflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.ogsadai.OgsadaiFormats;
import at.ac.univie.isc.asio.ogsadai.WorkflowComposer;

import com.google.common.io.OutputSupplier;

/**
 * Create SQL workflows for {@link DatasetOperation operations}.
 * 
 * @author Chris Borckholder
 */
public class SqlComposer implements WorkflowComposer {

	private final ResourceID resource;

	public SqlComposer(final ResourceID resource) {
		this.resource = resource;
	}

	@Override
	public ActivityPipelineWorkflow createFrom(
			final DatasetOperation operation,
			final OutputSupplier<OutputStream> supplier) {
		final Action action = operation.action();
		PipeBuilder pipe;
		switch (action) {
			case QUERY:
				pipe = makeQuery(operation.format(), operation.commandOrFail());
				break;
			case SCHEMA:
				assert !operation.command().isPresent() : "command present in "
						+ operation;
				pipe = makeSchema(operation.format());
				break;
			case UPDATE:
				pipe = makeUpdate(operation.format(), operation.commandOrFail());
				break;
			default:
				throw new UnsupportedOperationException("not implemented "
						+ action);
		}
		return pipe.finish(deliverToStream(supplier));
	}

	private PipeBuilder makeUpdate(final SerializationFormat format,
			final String update) {
		final PipeBuilder pipe = pipe(sqlUpdate(resource, update));
		BlockTransformer converter = null;
		if (format == OgsadaiFormats.XML) {
			converter = new XmlUpdateCountTransformer(update);
		} else if (format == OgsadaiFormats.PLAIN) {
			converter = new PlainUpdateCountTransformer(update);
		} else {
			throw unexpected(format);
		}
		pipe.into(dynamicSerializer(converter));
		return pipe;
	}

	private PipeBuilder makeSchema(final SerializationFormat format) {
		final PipeBuilder pipe = pipe(extractSchema(resource));
		if (format == OgsadaiFormats.XML) {
			pipe.into(metadataToXml());
		} else {
			throw unexpected(format);
		}
		return pipe;
	}

	private PipeBuilder makeQuery(final SerializationFormat format,
			final String query) {
		final PipeBuilder pipe = pipe(sqlQuery(resource, query));
		if (format == OgsadaiFormats.XML) {
			pipe.into(tupleToWebRowSetCharArrays());
		} else if (format == OgsadaiFormats.CSV) {
			pipe.into(tupleToCsv());
		} else {
			throw unexpected(format);
		}
		return pipe;
	}

	private AssertionError unexpected(final SerializationFormat illegal) {
		return new AssertionError("unsupported format " + illegal
				+ " for SQL workflows");
	}
}
