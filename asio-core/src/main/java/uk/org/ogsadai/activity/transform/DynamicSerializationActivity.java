package uk.org.ogsadai.activity.transform;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;

import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityTerminatedException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.MatchedIterativeActivity;
import uk.org.ogsadai.activity.io.ActivityIOException;
import uk.org.ogsadai.activity.io.ActivityInput;
import uk.org.ogsadai.activity.io.BlockWriter;
import uk.org.ogsadai.activity.io.ControlBlock;
import uk.org.ogsadai.activity.io.PipeClosedException;
import uk.org.ogsadai.activity.io.PipeIOException;
import uk.org.ogsadai.activity.io.PipeTerminatedException;
import uk.org.ogsadai.activity.io.TypedActivityInput;
import uk.org.ogsadai.common.msgs.DAILogger;

/**
 * An activity that converts received {@link Object blocks} to a binary format, using a
 * {@link BlockTransformer} given as input. The result is output as an OGSA-DAI list of
 * <code>byte[]</code> of fixed size.
 * <p>
 * Activity inputs:
 * </p>
 * <ul>
 * <li>
 * <code>data</code> Type: Any object that is compatible to the given <code>transformer</code>. The
 * block that should be serialized.</li>
 * <li>
 * <code>transformer</code> Type: {@link BlockTransformer}. The transformer to be used.</li>
 * </ul>
 * <p>
 * Activity outputs:
 * </p>
 * <ul>
 * <li>
 * <code>result</code>. Type: OGSA-DAI list of <code>byte[]</code>. The serialized block.</li>
 * </ul>
 * <p>
 * Configuration parameters: none.
 * </p>
 * <p>
 * Activity input/output ordering: none.
 * </p>
 * <p>
 * Activity contracts: none.
 * </p>
 * <p>
 * Target data resource: none.
 * </p>
 * <p>
 * Behaviour:
 * </p>
 * <ul>
 * <li>
 * The input block is read, transformed using the read transformer and emitted as list of byte[].</li>
 * </ul>
 * 
 * @author Chris Borckholder
 */
public class DynamicSerializationActivity extends MatchedIterativeActivity {

  // OGSA-DAI logger
  private static final DAILogger LOG = DAILogger.getLogger(DynamicSerializationActivity.class);

  /** block to be serialized */
  public static final String INPUT_DATA = "data";
  /** serialization strategy */
  public static final String INPUT_TRANSFORMER = "transformer";
  /** stream of serialized data */
  public static final String OUTPUT = "result";

  // XXX make configurable ?
  private static final int BLOCK_SIZE = 8192; // magic JAVA buffer size

  private BlockWriter output;

  @Override
  protected ActivityInput[] getIterationInputs() {
    return new ActivityInput[] {new TypedActivityInput(INPUT_DATA, Object.class),
        new TypedActivityInput(INPUT_TRANSFORMER, BlockTransformer.class)};
  }

  @Override
  protected void preprocess() throws ActivityUserException, ActivityProcessingException,
      ActivityTerminatedException {
    validateOutput(OUTPUT);
    output = getOutput();
  }

  @Override
  protected void processIteration(final Object[] iterationData) throws ActivityProcessingException,
      ActivityTerminatedException, ActivityUserException {
    final Object block = checkNotNull(iterationData[0], "missing serialization input");
    final BlockTransformer transformer =
        (BlockTransformer) checkNotNull(iterationData[1], "missing transformer");
    if (LOG.isDebugEnabled()) {
      LOG.debug("Serializing " + block + " using " + transformer);
    }
    try {
      output.write(ControlBlock.LIST_BEGIN);
      try (final OutputStream adapted = new BlockWriterStreamAdapter(output, BLOCK_SIZE)) {
        transformer.writeObject(adapted, block);
      }
      output.write(ControlBlock.LIST_END);
    } catch (final IOException e) {
      output.closeForWritingDueToError();
      throw new ActivityIOException(e);
    } catch (final PipeClosedException e) {
      iterativeStageComplete();
    } catch (final PipeIOException e) {
      throw new ActivityProcessingException(e);
    } catch (final PipeTerminatedException e) {
      throw new ActivityTerminatedException();
    } finally {
      output.closeForWriting();
    }
  }

  @Override
  protected void postprocess() throws ActivityUserException, ActivityProcessingException,
      ActivityTerminatedException {
    // none
  }

}
