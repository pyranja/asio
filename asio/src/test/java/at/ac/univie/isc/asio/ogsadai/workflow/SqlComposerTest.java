package at.ac.univie.isc.asio.ogsadai.workflow;

import static at.ac.univie.isc.asio.ogsadai.OgsadaiFormats.CSV;
import static at.ac.univie.isc.asio.ogsadai.OgsadaiFormats.XML;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.org.ogsadai.activity.ActivityName;
import uk.org.ogsadai.activity.pipeline.ActivityDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.workflow.ActivityPipelineWorkflow;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.MockOperations;

import com.google.common.io.OutputSupplier;

public class SqlComposerTest {

	private static final ResourceID RESOURCE = new ResourceID("test");

	private SqlComposer subject;
	private Set<ActivityDescriptor> activities;

	@Before
	public void setUp() {
		subject = new SqlComposer(RESOURCE);
	}

	@After
	public void tearDown() {
		activities.clear();
	}

	@Test
	public void query_workflow_has_sql_query_activity() throws Exception {
		final DatasetOperation op = MockOperations.query("query", XML);
		createWorkflowFor(op);
		final ActivityDescriptor sql = getByType(PipeActivities.SQL_QUERY_ACTIVITY);
		assertNotNull("sql query activity missing in workflow", sql);
		RESOURCE.equals(sql.getTargetResource());
	}

	@Test
	public void workflow_has_delivery() throws Exception {
		final DatasetOperation op = MockOperations.query("query", CSV);
		createWorkflowFor(op);
		assertTrue(isPresent(PipeActivities.STREAM_DELIVERY));
	}

	@Test
	public void query_workflow_has_xml_transformation() throws Exception {
		final DatasetOperation op = MockOperations.query("query", XML);
		createWorkflowFor(op);
		assertTrue(isPresent(PipeActivities.TUPLE_WEBROWSET_TRANSFORMER_ACTIVITY));
	}

	@Test
	public void query_workflow_has_csv_transformer() throws Exception {
		final DatasetOperation op = MockOperations.query("query", CSV);
		createWorkflowFor(op);
		assertTrue(isPresent(PipeActivities.TUPLE_CSV_TRANSFORMER_ACTIVITY));
	}

	@Test
	public void schema_workflow_has_extract_schema() throws Exception {
		final DatasetOperation op = MockOperations.schema(XML);
		createWorkflowFor(op);
		final ActivityDescriptor sql = getByType(PipeActivities.SQL_SCHEMA_ACTIVITY);
		assertNotNull("sql schema activity missing in workflow", sql);
		RESOURCE.equals(sql.getTargetResource());
	}

	@Test
	public void schema_workflow_has_xml_transformer() throws Exception {
		final DatasetOperation op = MockOperations.schema(XML);
		createWorkflowFor(op);
		assertTrue(isPresent(PipeActivities.TABLEMETADATA_XML_TRANSFORMER_ACTIVITY));
	}

	@Test
	public void update_workflow_has_update_activity() throws Exception {
		final DatasetOperation op = MockOperations.update("update", XML);
		createWorkflowFor(op);
		final ActivityDescriptor sql = getByType(PipeActivities.SQL_UPDATE_ACTIVITY);
		assertNotNull("sql update activity missing in workflow", sql);
		RESOURCE.equals(sql.getTargetResource());
	}

	@Test
	public void update_workflow_has_transformer() throws Exception {
		final DatasetOperation op = MockOperations.update("update", XML);
		createWorkflowFor(op);
		assertTrue(isPresent(PipeActivities.DYNAMIC_TRANSFORMER_ACTIVITY));
	}

	@SuppressWarnings("unchecked")
	private void createWorkflowFor(final DatasetOperation op) {
		final OutputSupplier<OutputStream> mockSupplier = new OutputSupplier<OutputStream>() {
			@Override
			public OutputStream getOutput() throws IOException {
				return null;
			}
		};
		final ActivityPipelineWorkflow wf = subject
				.createFrom(op, mockSupplier);
		final ActivityPipeline pipe = wf.getActivityPipeline();
		activities = pipe.getActivities();
	}

	private boolean isPresent(final ActivityName type) {
		return getByType(type) != null;
	}

	private ActivityDescriptor getByType(final ActivityName type) {
		for (final ActivityDescriptor each : activities) {
			if (type.equals(each.getActivityName())) {
				return each;
			}
		}
		return null;
	}
}