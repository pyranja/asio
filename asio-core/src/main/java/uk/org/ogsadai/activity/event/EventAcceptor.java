package uk.org.ogsadai.activity.event;

import static uk.org.ogsadai.resource.request.RequestExecutionStatus.COMPLETED;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.ERROR;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.TERMINATED;

import java.util.concurrent.atomic.AtomicInteger;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;

/**
 * Inspect request events (status changes and errors) to decide whether to
 * notify the given {@link CompletionCallback} and/or stop tracking.
 * 
 * @author Chris Borckholder
 */
public class EventAcceptor {

   // error states
   private static final int NO_ERRORS = 1; // no errors encountered
   private static final int HAS_PUBLISHED_ERRORS = 2; // has handled errors
   private static final int ENDED_WITH_ERROR = 4; // received ERROR state

   private final CompletionCallback delegate;
   private final AtomicInteger errorState;

   EventAcceptor(final CompletionCallback delegate) {
      this.delegate = delegate;
      errorState = new AtomicInteger(NO_ERRORS);
   }

   /**
    * Inspect the new state after a change and notify if required.
    * 
    * @param status
    *           of request
    * @return true if the request should not be tracked anymore
    */
   public boolean handleStateAndStop(final RequestExecutionStatus status) {
      boolean stopTracking = false;
      if (status.hasFinished()) {
         if (status == COMPLETED) {
            delegate.complete();
            stopTracking = true;
         } else if (status == TERMINATED) {
            delegate.fail(new RequestTerminatedException());
            stopTracking = true;
         } else if (status == ERROR) {
            stopTracking = !errorState.compareAndSet(NO_ERRORS, ENDED_WITH_ERROR);
         } else {
            delegate
                  .fail(new RequestProcessingException(new IllegalStateException("unknown error")));
            throw new AssertionError("received unexpected terminal request status " + status);
         }
      }
      return stopTracking;
   }

   /**
    * Notify the callback of the request error.
    * 
    * @param cause
    *           of request failure
    * @return true if the request should not be tracked anymore
    */
   public boolean handleErrorAndStop(final DAIException cause) {
      final int formerState = errorState.getAndSet(HAS_PUBLISHED_ERRORS);
      delegate.fail(cause);
      return formerState == ENDED_WITH_ERROR;
   }

   /**
    * @return the {@link CompletionCallback} this acceptor delegates to.
    */
   public CompletionCallback getDelegate() {
      return delegate;
   }
}
