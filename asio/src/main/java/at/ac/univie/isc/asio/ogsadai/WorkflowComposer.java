package at.ac.univie.isc.asio.ogsadai;

import java.io.OutputStream;

import uk.org.ogsadai.activity.workflow.Workflow;
import at.ac.univie.isc.asio.DatasetOperation;

import com.google.common.io.OutputSupplier;

/**
 * Transform {@link DatasetOperation}s into OGSADAI {@link Workflow}s.
 * 
 * @author Chris Borckholder
 */
public interface WorkflowComposer {

	/**
	 * Create a matching workflow for the given operation.
	 * 
	 * @param operation
	 *            to be executed
	 * @param sinkSupplier
	 *            provider of output streams for result delivery
	 * @return a matching workflow
	 */
	Workflow createFrom(DatasetOperation operation,
			OutputSupplier<OutputStream> sinkSupplier);
}
