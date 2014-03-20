package at.ac.univie.isc.asio;

import java.security.Principal;

import javax.annotation.Nullable;

import at.ac.univie.isc.asio.security.Anonymous;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;

/**
 * Represent an operation on a dataset, including the command to be executed and the required output
 * format.
 * 
 * @author Chris Borckholder
 */
public class DatasetOperation {

  /**
   * The different possible types of DatasetOperations.
   * 
   * @author Chris Borckholder
   */
  public static enum Action {
    QUERY, SCHEMA, UPDATE, BATCH;
  }

  /**
   * Marker interface for result output media types.
   * 
   * @author Chris Borckholder
   */
  public static interface SerializationFormat {
    /**
     * @return the common internet media type of this format
     */
    MediaType asMediaType();

    /**
     * @param action of operation
     * @return true if this format is supported for the given operation action
     */
    boolean applicableOn(Action action);

    /** indicates that no valid format is set on the operation */
    public static final SerializationFormat NONE = new SerializationFormat() {
      @Override
      public MediaType asMediaType() {
        throw new AssertionError("NONE format accessed");
      }

      @Override
      public boolean applicableOn(final Action action) {
        throw new AssertionError("NONE format accessed");
      }
    };
  }

  private final String id;
  private final Action action;
  private final Optional<String> command;
  private final SerializationFormat format;
  private final Principal owner;

  public DatasetOperation(final String id, final Action action, @Nullable final String command,
      final SerializationFormat format) {
    super();
    this.id = id;
    this.action = action;
    this.command = Optional.fromNullable(command);
    this.format = format;
    owner = Anonymous.INSTANCE;
  }

  private DatasetOperation(final DatasetOperation prototype, final Principal owner) {
    super();
    id = prototype.id;
    action = prototype.action;
    command = Optional.fromNullable(prototype.command.orNull());
    format = prototype.format;
    this.owner = owner;
  }

  public DatasetOperation withOwner(final Principal owner) {
    return new DatasetOperation(this, owner);
  }

  /**
   * @return the unique id of this operation instance.
   */
  public String id() {
    return id;
  }

  /**
   * The action of operation described by this instance.
   * 
   * @return one of the possible {@link Action dataset actions}
   */
  public Action action() {
    return action;
  }

  /**
   * The command to be executed by this operation if one is given.
   * 
   * @return optional holding the command if one is set
   */
  public Optional<String> command() {
    return command;
  }

  /**
   * If a command is required, this getter will fail fast if it is not present with a
   * {@link DatasetUsageException} holding details of this operation.
   * 
   * @return the command if it is present
   * @throws DatasetUsageException if the command is not present
   */
  public String commandOrFail() throws DatasetUsageException {
    if (command.isPresent()) {
      return command.get();
    } else {
      throw new DatasetUsageException("required command missing");
    }
  }

  /**
   * The output format in which the results of this operation must be rendered.
   * 
   * @return the desired format
   */
  public SerializationFormat format() {
    return format;
  }

  /**
   * The subject, that requested this operation.
   * 
   * @return
   */
  public Principal owner() {
    return owner;
  }

  @Override
  public String toString() {
    return String.format("[action=%s, command=\"%s\", format=%s]", action, command.or("none"),
        format);
  }
}