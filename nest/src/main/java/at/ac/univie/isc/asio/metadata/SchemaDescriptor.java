package at.ac.univie.isc.asio.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Non-functional metadata on a deployed schema.
 */
@AutoValue
public abstract class SchemaDescriptor {

  /**
   * Create a builder, where only the identifier and label are set to the given value. The status is
   * not active, all other values are {@code null}.
   *
   * @param identifier global id of schema
   * @return builder
   */
  public static SchemaDescriptor.Builder empty(final String identifier) {
    return new Builder(identifier);
  }

  /**
   * Create a builder, that is initialized with generic default values.
   *
   * @param identifier global id of the schema
   * @return builder
   */
  public static SchemaDescriptor.Builder defaults(final String identifier) {
    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    return new Builder(identifier)
        .withActive(false)
        .withLabel(identifier).withCategory("general").withDescription("no description")
        .withAuthor("anonymous").withLicense("public domain")
        .withCreated(now).withUpdated(now)
        .withTags(Collections.<String>emptyList()).withLinks(Collections.<Link>emptyList());
  }

  /**
   * Factory method intended for serialization - use the builder to create instances manually.
   * This factory applies default values:
   * <ul>
   * <li>{@code ZonedDateTime.now(ZoneOffset.UTC)} for {@code created} and {@code updated}</li>
   * <li>{@code Collections.emptyList()} for {@code tags} and {@code links}</li>
   * </ul>
   * Other {@link javax.annotation.Nullable nullable} fields are left blank if they are not set.
   *
   * @return fully constructed descriptor
   */
  @JsonCreator
  static SchemaDescriptor create(
      @JsonProperty("identifier") final String identifier,
      @JsonProperty("active") final boolean active,
      @JsonProperty("label") @Nullable final String label,
      @JsonProperty("description") @Nullable final String description,
      @JsonProperty("author") @Nullable final String author,
      @JsonProperty("license") @Nullable final String license,
      @JsonProperty("category") @Nullable final String category,
      @JsonProperty("created") @Nullable final ZonedDateTime created,
      @JsonProperty("updated") @Nullable final ZonedDateTime updated,
      @JsonProperty("tags") @Nullable final List<String> tags,
      @JsonProperty("links") @Nullable final List<Link> links) {
    final String labelOrIdentifier = label == null ? identifier : label;
    final List<String> tagsCopy = tags == null ?
        ImmutableList.<String>of() : ImmutableList.copyOf(tags);
    final List<Link> linksCopy = links == null ?
        ImmutableList.<Link>of() : ImmutableList.copyOf(links);
    // normalize times to UTC if present, else use now as fallback
    final ZonedDateTime createdOrNow =
        created == null ? ZonedDateTime.now(ZoneOffset.UTC) : created.withZoneSameInstant(ZoneOffset.UTC);
    // use same instant as created for consistency
    final ZonedDateTime updatedOrNow = updated == null ? createdOrNow : updated.withZoneSameInstant(ZoneOffset.UTC);
    return new AutoValue_SchemaDescriptor(identifier, active, labelOrIdentifier, description, author, license, category, createdOrNow, updatedOrNow, tagsCopy, linksCopy);
  }

  SchemaDescriptor() { /* prevent sub-classing */ }

  /**
   * Globally unique identifier of this schema in the backing metadata repository.
   * 
   * @return the identifier
   */
  public abstract String getIdentifier();

  /**
   * Whether this schema is currently active. Generally this should be the case, if it is hosted in
   * an asio instance.
   * 
   * @return true if this schema is active
   */
  public abstract boolean isActive();

  /**
   * A short name for this schema. If present, this is also the name of the backing MySQL database. 
   * 
   * @return name of this schema
   */
  public abstract String getLabel();

  /**
   * A human readable, more detailed description of the contents and/or purpose of data contained
   * in this schema.
   */
  @Nullable
  public abstract String getDescription();

  /**
   * The creator of this schema.
   */
  @Nullable
  public abstract String getAuthor();

  /**
   * The licensing terms that apply to this schema. A missing value does <strong>NOT</strong> 
   * indicate that no license is applied.
   */
  @Nullable
  public abstract String getLicense();

  /**
   * A general, broad category into which this schema falls.
   */
  @Nullable
  public abstract String getCategory();

  /**
   * The UTC time and date of creation.
   */
  public abstract ZonedDateTime getCreated();

  /**
   * The UTC time and date of the last update.
   */
  public abstract ZonedDateTime getUpdated();

  /**
   * A collection of tags that describe this schema.
   */
  public abstract List<String> getTags();

  /**
   * A collection of links to related resources, e.g. additional information.
   */
  public abstract List<Link> getLinks();

  /**
   * Fluent builder for {@code SchemaDescriptor}.
   */
  public static class Builder {
    private final String identifier;
    private boolean active;
    private String label;
    private String description;
    private String author;
    private String license;
    private String category;
    private ZonedDateTime created;
    private ZonedDateTime updated;
    private List<String> tags;
    private List<Link> links;

    public Builder(final String identifier) {
      this.identifier = identifier;
      this.label = identifier;
    }

    public Builder withActive(final boolean active) {
      this.active = active;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withAuthor(final String author) {
      this.author = author;
      return this;
    }

    public Builder withLicense(final String license) {
      this.license = license;
      return this;
    }

    public Builder withCategory(final String category) {
      this.category = category;
      return this;
    }

    public Builder withCreated(final ZonedDateTime created) {
      this.created = created;
      return this;
    }

    public Builder withUpdated(final ZonedDateTime updated) {
      this.updated = updated;
      return this;
    }

    public Builder withTags(final List<String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder withLinks(final List<Link> links) {
      this.links = links;
      return this;
    }

    public SchemaDescriptor build() {
      return create(identifier, active, label, description, author, license, category, created, updated, tags, links);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "identifier='" + identifier + '\'' +
          ", active=" + active +
          ", label='" + label + '\'' +
          ", description='" + description + '\'' +
          ", author='" + author + '\'' +
          ", license='" + license + '\'' +
          ", category='" + category + '\'' +
          ", created=" + created +
          ", updated=" + updated +
          ", tags=" + tags +
          ", links=" + links +
          '}';
    }
  }
}
