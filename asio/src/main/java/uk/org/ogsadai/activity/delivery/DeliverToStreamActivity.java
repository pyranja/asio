// Copyright (c) The University of Edinburgh, 2007.
//
// LICENCE-START
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// LICENCE-END

package uk.org.ogsadai.activity.delivery;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.MatchedIterativeActivity;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.activity.io.ActivityInput;
import uk.org.ogsadai.activity.io.InputStreamActivityInput;
import uk.org.ogsadai.activity.io.TypedActivityInput;
import uk.org.ogsadai.common.msgs.DAILogger;
import uk.org.ogsadai.exception.ErrorID;

import com.google.common.io.ByteStreams;
import com.google.common.io.OutputSupplier;

/**
 * An activity that delivers data to an in-process {@link OutputStream}. That
 * stream must be given as second input {@link #INPUT_SUPPLIER}.
 * <p>
 * Inputs:
 * </p>
 * <ul>
 * <li><code>input</code>. Type: One of OGSA-DAI list of <code>char[]</code>,
 * OGSA-DAI list of <code>byte[]</code>, {@link java.sql.Clob} or
 * {@link java.sql.Blob}. The data that will be streamed to the retrieved
 * {@link OutputStrem}. If the input contains character data rather than byte
 * data then the characters are converted to bytes using UTF-8 encoding.</li>
 * <li><code>supplier</code>. Type: {@link com.google.common.io.OutputSupplier}.
 * Capable of providing an output stream at least once. This activity will
 * invoke {@link OutputSupplier#getOutput() getOutput()} on the supplier exactly
 * once.</li>
 * </ul>
 * <p>
 * Outputs: none.
 * </p>
 * <p>
 * Activity input/output ordering:
 * </p>
 * <ul>
 * <li>
 * The <code>input</code>,<code>supplier</code> inputs are read and then the
 * data is streamed to the retrieved OutputStream.</li>
 * </ul>
 * <p>
 * Activity contracts: none.
 * </p>
 * <p>
 * Target data resource: none.
 * </p>
 * <p>
 * Behavior:
 * </p>
 * <ul>
 * <li>
 * The activity attempts to retrieve an {@link OutputStream stream} from the
 * given {@link OuputSupplier supplier} and writes the received input data to
 * it. After the input data is exhausted or an error occurred, both the
 * InputStream and OutputStream will be closed.</li>
 * <li>
 * An <code>ActivityProcessingException</code> will be thrown if the given
 * supplier cannot provide a stream or an I/O error is encountered.</li>
 * </ul>
 * 
 * @author Chris Borckholder
 */
public class DeliverToStreamActivity extends MatchedIterativeActivity {

	/** Logger object for logging in this class. */
	private static final DAILogger LOG = DAILogger
			.getLogger(DeliverToStreamActivity.class);

	/** Activity input name - stream id */
	public static final String INPUT_SUPPLIER = "supplier";

	/** Activity input name - Data. */
	public static final String INPUT_DATA = "input";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ActivityInput[] getIterationInputs() {
		return new ActivityInput[] { new InputStreamActivityInput(INPUT_DATA),
				new TypedActivityInput(INPUT_SUPPLIER, OutputSupplier.class) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void preprocess() throws ActivityUserException,
			ActivityProcessingException, ActivityTerminatedException {
		// no pre-processing required
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected void processIteration(final Object[] iterationData)
			throws ActivityProcessingException, ActivityTerminatedException,
			ActivityUserException {
		// check input
		final InputStream data = (InputStream) failIfInputNull(
				iterationData[0], INPUT_DATA);
		@SuppressWarnings("unchecked")
		final OutputSupplier<OutputStream> supplier = (OutputSupplier<OutputStream>) failIfInputNull(
				iterationData[1], INPUT_SUPPLIER);
		if (LOG.isDebugEnabled()) {
			LOG.debug("delivering activity data to stream");
		}
		// process
		try {
			try (final OutputStream sink = supplier.getOutput();) {
				ByteStreams.copy(data, sink);
			} catch (final IOException e) {
				LOG.debug("IO error on streaming activity results - "
						+ e.getLocalizedMessage());
				LOG.error(e, true);
				throw new ActivityIOException(e);
			}
		} finally {
			this.close(data);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("finished delivering activity data to stream");
		}
	}

	/* close a stream without failing but log errors that occur */
	private void close(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (final IOException e) {
				LOG.debug("IO error on closing the activity results - "
						+ e.getLocalizedMessage());
				LOG.error(e, true);
			}
		} else {
			LOG.warn(new NullPointerException(
					"IO error on closing the activity results - stream is null"));
		}
	}

	/**
	 * Check the given reference for non-nullness.
	 * 
	 * @param reference
	 *            to be checked
	 * @param name
	 *            of the input that is checked
	 * @return the reference if it is not null
	 * @throws ActivityUserException
	 *             if the reference is null
	 */
	private <T> T failIfInputNull(final T reference, final String name)
			throws ActivityUserException {
		if (reference == null) {
			throw new ActivityUserException(
					ErrorID.INVALID_INPUT_VALUE_EXCEPTION, new Object[] { name,
							reference });
		}
		return reference;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void postprocess() throws ActivityUserException,
			ActivityProcessingException, ActivityTerminatedException {
		// no post-processing required
	}
}
