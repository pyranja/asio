package at.ac.univie.isc.asio.ogsadai;

import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetOperation;

public interface WorkflowComposer {

	Workflow createFrom(DatasetOperation operation, String streamId);
}
