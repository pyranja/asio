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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.MatchedIterativeActivity;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.activity.io.ActivityInput;
import uk.org.ogsadai.activity.io.InputStreamActivityInput;
import uk.org.ogsadai.activity.io.TypedActivityInput;
import uk.org.ogsadai.common.ID;
import uk.org.ogsadai.common.msgs.DAILogger;
import uk.org.ogsadai.context.OGSADAIContext;
import uk.org.ogsadai.exception.ErrorID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;

/**
 * An activity that delivers data to an in-process {@link OutputStream}. This
 * activity requires an additional component, the {@link StreamExchanger}, to be
 * setup and available under the id {@link #STREAM_EXCHANGER} from the
 * {@link OGSADAIContext}. The OutputStream to be used must be available from
 * the set StreamExchanger under the id given as input {@link #INPUT_STREAM_ID}
 * to this activity.
 * <p>
 * Inputs:
 * </p>
 * <ul>
 * <li>
 * <code>streamId</code>. Type: {@link java.lang.String}. Id of the required
 * stream in the StreamExchanger.</li>
 * <code>input</code>. Type: One of OGSA-DAI list of <code>char[]</code>,
 * OGSA-DAI list of <code>byte[]</code>, {@link java.sql.Clob} or
 * {@link java.sql.Blob}. The data that will be streamed to the retrieved
 * {@link OutputStrem}. If the input contains character data rather than byte
 * data then the characters are converted to bytes using UTF-8 encoding.</li>
 * </ul>
 * <p>
 * Outputs: none.
 * </p>
 * <p>
 * Activity input/output ordering:
 * </p>
 * <ul>
 * <li>
 * The <code>id</code>, <code>data</code> inputs are read and then the data is
 * streamed to the retrieved OutputStream.</li>
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
 * The activity attempts to retrieve the stream associated with the given id
 * from the {@link StreamExchanger} and writes the received input data to that
 * OutputStream. After the input data is exhausted or an error occured, both the
 * InputStream and OutputStream will be closed.</li>
 * <li>
 * An <code>ActivityProcessingException</code> will be thrown if the given id
 * has no associated stream or an I/O error is encountered.</li>
 * </ul>
 * 
 * @author Chris Borckholder
 */
public class DeliverToStreamActivity extends MatchedIterativeActivity {

   /** Logger object for logging in this class. */
   private static final DAILogger LOG = DAILogger.getLogger(DeliverToStreamActivity.class);

   /** Activity input name - stream id */
   public static final String INPUT_STREAM_ID = "streamId";

   /** Activity input name - Data. */
   public static final String INPUT_DATA = "input";

   /** id of stream exchanger in the {@link OGSADAIContext} */
   public static final ID STREAM_EXCHANGER = new ID("at.ac.univie.isc.STREAM_EXCHANGER");

   // provides task id -> stream mappings
   private final StreamExchanger streamProvider;

   /**
    * Retrieves the {@link VceOgsadaiConnector} on construction.
    */
   public DeliverToStreamActivity() {
      super();
      streamProvider = (StreamExchanger) OGSADAIContext.getInstance().get(STREAM_EXCHANGER);
   }

   @VisibleForTesting
   DeliverToStreamActivity(final StreamExchanger streamProvider) {
      super();
      this.streamProvider = streamProvider;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ActivityInput[] getIterationInputs() {
      return new ActivityInput[] { new TypedActivityInput(INPUT_STREAM_ID, String.class),
            new InputStreamActivityInput(INPUT_DATA) };
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void preprocess() throws ActivityUserException, ActivityProcessingException,
         ActivityTerminatedException {
      // no pre-processing required
   }

   /**
    * {@inheritDoc}
    * 
    */
   @Override
   protected void processIteration(final Object[] iterationData)
         throws ActivityProcessingException, ActivityTerminatedException, ActivityUserException {
      // check input
      final String streamId = (String) checkNotNull(iterationData[0],
            "no stream id given for delivery: is null");
      final InputStream data = (InputStream) checkNotNull(iterationData[1],
            "no data given for delivery to #%s: is null", streamId);
      if (LOG.isDebugEnabled()) {
         LOG.debug("Delivering activity data to stream #" + streamId);
      }
      // process
      final Optional<OutputStream> providedStream = streamProvider.take(streamId);
      try {
         if (providedStream.isPresent()) {
            try (final OutputStream sink = providedStream.get();) {
               ByteStreams.copy(data, sink);
            } catch (final IOException e) {
               LOG.debug("IO error on streaming activity results - " + e.getLocalizedMessage());
               LOG.error(e, true);
               throw new ActivityIOException(e);
            }
         } else {
            final NoSuchElementException cause = new NoSuchElementException("no stream with id #"
                  + streamId + " available");
            LOG.error(cause, true);
            throw new ActivityUserException(ErrorID.INVALID_INPUT_VALUE_EXCEPTION, new Object[] {
                  INPUT_STREAM_ID, streamId }, cause);
         }
      } finally {
         this.close(data);
      }
      if (LOG.isDebugEnabled()) {
         LOG.debug("Finished delivering activity data to #" + streamId);
      }
   }

   /* close a stream without failing but log errors that occur */
   private void close(final Closeable stream) {
      if (stream != null) {
         try {
            stream.close();
         } catch (final IOException e) {
            LOG.debug("IO error on closing the activity results - " + e.getLocalizedMessage());
            LOG.error(e, true);
         }
      } else {
         LOG.warn(new NullPointerException(
               "IO error on closing the activity results - stream is null"));
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void postprocess() throws ActivityUserException, ActivityProcessingException,
         ActivityTerminatedException {
      // no post-processing required
   }
}
