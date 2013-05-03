package at.ac.univie.isc.asio.ogsadai;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import uk.org.ogsadai.activity.pipeline.ActivityDescriptor;

/**
 * Provide activity wrappers that can act as Producer or Consumer or both.
 * 
 * @author Chris Borckholder
 */
public final class PipeElements {

   private PipeElements() {/* static factory class */}

   /**
    * Create a wrapper that holds a tuple producing activity
    * 
    * @param activity
    *           wrapped activity
    * @param output
    *           name of output pipe
    * @return producer
    */
   public static Producer producer(final ActivityDescriptor activity, final String output) {
      checkNotNull(emptyToNull(output), "output name must be given");
      return new ActivityWrapper(activity, output, null);
   }

   /**
    * Create a wrapper that holds a tuple consuming activity
    * 
    * @param activity
    *           wrapped activity
    * @param input
    *           name of input pipe
    * @return consumer
    */
   public static Consumer consumer(final ActivityDescriptor activity, final String input) {
      checkNotNull(emptyToNull(input), "input name must be given");
      return new ActivityWrapper(activity, null, input);
   }

   /**
    * Create a wrapper that holds an activity that can produce and consume
    * tuples
    * 
    * @param activity
    *           wrapped activity
    * @param input
    *           name of input pipe
    * @param output
    *           name of output pipe
    * @return producer and consumer
    */
   public static ProducerAndConsumer both(final ActivityDescriptor activity, final String input,
         final String output) {
      checkNotNull(emptyToNull(input), "input name must be given");
      checkNotNull(emptyToNull(output), "output name must be given");
      return new ActivityWrapper(activity, output, input);
   }

   /**
    * Represent an activity that consumes tuples.
    * 
    * @author Chris Borckholder
    */
   public interface Consumer {

      /**
       * @return the wrapped activity
       */
      ActivityDescriptor activity();

      /**
       * @return the name of the activity's tuple input
       */
      String input();
   }

   /**
    * Represent an activity that produces tuples.
    * 
    * @author Chris Borckholder
    */
   public interface Producer {

      /**
       * @return the wrapped activity
       */
      ActivityDescriptor activity();

      /**
       * @return the name of the activity's tuple output
       */
      String output();
   }

   /**
    * Represent an activity that can act as Producer and Consumer
    * 
    * @author Chris Borckholder
    */
   public interface ProducerAndConsumer extends Producer, Consumer {
      // no new definitions
   }

   private static class ActivityWrapper implements ProducerAndConsumer {

      private final ActivityDescriptor activity;
      private final String outName;
      private final String inName;

      ActivityWrapper(final ActivityDescriptor activity, final String outName, final String inName) {
         super();
         this.activity = checkNotNull(activity, "wrapped activity may not be null");
         this.outName = outName;
         this.inName = inName;
      }

      @Override
      public ActivityDescriptor activity() {
         return activity;
      }

      @Override
      public String input() {
         if (inName == null) { throw new UnsupportedOperationException("not a consumer"); }
         return inName;
      }

      @Override
      public String output() {
         if (outName == null) { throw new UnsupportedOperationException("not a producer"); }
         return outName;
      }
   }
}
