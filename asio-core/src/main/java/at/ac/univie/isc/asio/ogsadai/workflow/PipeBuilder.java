package at.ac.univie.isc.asio.ogsadai.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.pipeline.SimpleActivityPipeline;
import uk.org.ogsadai.activity.workflow.ActivityPipelineWorkflow;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.Consumer;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.Producer;
import at.ac.univie.isc.asio.ogsadai.workflow.PipeElements.ProducerAndConsumer;

/**
 * Create activity pipeline workflows in a fluent way.
 * 
 * @author Chris Borckholder
 */
public final class PipeBuilder {

  /**
   * Start the creation of a pipeline workflow.
   * 
   * @param start first activity producing tuples
   * @return builder
   */
  public static PipeBuilder pipe(final Producer start) {
    return new PipeBuilder(start);
  }

  private final ActivityPipeline pipe;
  private Producer last;

  PipeBuilder(final Producer start) {
    checkNotNull(start, "given activity is null");
    pipe = new SimpleActivityPipeline();
    last = start;
  }

  /**
   * Adds the given activity that produces and consumes tuples to the workflow and connects its
   * input to the current last producer's output.
   * 
   * @param next tuple producing and consuming activity
   * @return builder
   */
  public PipeBuilder into(final ProducerAndConsumer next) {
    checkNotNull(next, "given activity is null");
    pipe.connect(last.activity(), last.output(), next.activity(), next.input());
    last = next;
    return this;
  }

  /**
   * Ends the workflow creation with the given tuple consuming activity.
   * 
   * @param end tuple consumer
   * @return created workflow
   */
  public ActivityPipelineWorkflow finish(final Consumer end) {
    checkNotNull(end, "given activity is null");
    pipe.connect(last.activity(), last.output(), end.activity(), end.input());
    return new ActivityPipelineWorkflow(pipe);
  }
}
