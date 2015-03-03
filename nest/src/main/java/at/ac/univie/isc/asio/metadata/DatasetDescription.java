package at.ac.univie.isc.asio.metadata;

import org.springframework.hateoas.ResourceSupport;
import org.threeten.bp.ZonedDateTime;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Describe a dataset.
 */
public final class DatasetDescription extends ResourceSupport {
  /**
   * Create with minimal required attributes.
   * @return valid dataset descriptor with minimal content.
   */
  public static DatasetDescription create(final String identifier, final boolean isActive) {
    requireNonNull(identifier);
    final DatasetDescription description = new DatasetDescription();
    description.setGlobalIdentifier(identifier);
    description.setLocalIdentifier(identifier);
    description.setLabel(identifier);
    description.setActive(isActive);
    description.setType("Dataset");
    return description;
  }

  public static DatasetDescription empty() {
    return new DatasetDescription();
  }

  DatasetDescription() {}

  /**
   * For class sanity tester.
   */
  DatasetDescription(final String globalIdentifier, final String localIdentifier, final String type, final boolean active, final String label, final String description, final String category, final String author, final String license, final ZonedDateTime created, final ZonedDateTime updated, final List<String> tags) {
    this.globalIdentifier = globalIdentifier;
    this.localIdentifier = localIdentifier;
    this.type = type;
    this.active = active;
    this.label = label;
    this.description = description;
    this.category = category;
    this.author = author;
    this.license = license;
    this.created = created;
    this.updated = updated;
    this.tags = tags;
  }

  // system properties

  private String globalIdentifier;

  private String localIdentifier;

  private String type;

  private boolean active;

  // human readable properties

  private String label;

  private String description;

  private String category;

  private String author;

  private String license;

  private ZonedDateTime created;

  private ZonedDateTime updated;

  private List<String> tags = new ArrayList<>();

  public String getGlobalIdentifier() {
    return globalIdentifier;
  }

  public void setGlobalIdentifier(final String globalIdentifier) {
    this.globalIdentifier = globalIdentifier;
  }

  @Nonnull
  public DatasetDescription withGlobalIdentifier(final String globalIdentifier) {
    this.globalIdentifier = globalIdentifier;
    return this;
  }

  public String getLocalIdentifier() {
    return localIdentifier;
  }

  public void setLocalIdentifier(final String localIdentifier) {
    this.localIdentifier = localIdentifier;
  }

  @Nonnull
  public DatasetDescription withLocalIdentifier(final String localIdentifier) {
    this.localIdentifier = localIdentifier;
    return this;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  @Nonnull
  public DatasetDescription withType(final String type) {
    this.type = type;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(final boolean active) {
    this.active = active;
  }

  @Nonnull
  public DatasetDescription withActive(final boolean active) {
    this.active = active;
    return this;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  @Nonnull
  public DatasetDescription withLabel(final String label) {
    this.label = label;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Nonnull
  public DatasetDescription withDescription(final String description) {
    this.description = description;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(final String category) {
    this.category = category;
  }

  @Nonnull
  public DatasetDescription withCategory(final String category) {
    this.category = category;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(final String author) {
    this.author = author;
  }

  @Nonnull
  public DatasetDescription withAuthor(final String author) {
    this.author = author;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(final String license) {
    this.license = license;
  }

  @Nonnull
  public DatasetDescription withLicense(final String license) {
    this.license = license;
    return this;
  }

  public ZonedDateTime getCreated() {
    return created;
  }

  public void setCreated(final ZonedDateTime created) {
    this.created = created;
  }

  @Nonnull
  public DatasetDescription withCreated(final ZonedDateTime created) {
    this.created = created;
    return this;
  }

  public ZonedDateTime getUpdated() {
    return updated;
  }

  public void setUpdated(final ZonedDateTime updated) {
    this.updated = updated;
  }

  @Nonnull
  public DatasetDescription withUpdated(final ZonedDateTime updated) {
    this.updated = updated;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(final List<String> tags) {
    this.tags = tags;
  }

  @Nonnull
  public DatasetDescription withTags(final String... tags) {
    this.tags.addAll(Arrays.asList(tags));
    return this;
  }

  @Override
  public String toString() {
    return "DatasetDescription{" +
        "globalIdentifier='" + globalIdentifier + '\'' +
        ", localIdentifier='" + localIdentifier + '\'' +
        ", type='" + type + '\'' +
        ", active=" + active +
        ", label='" + label + '\'' +
        ", description='" + description + '\'' +
        ", category='" + category + '\'' +
        ", author='" + author + '\'' +
        ", license='" + license + '\'' +
        ", created=" + created +
        ", updated=" + updated +
        ", tags=" + tags +
        ", links=" + getLinks() + '\'' +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    final DatasetDescription that = (DatasetDescription) o;

    if (active != that.active)
      return false;
    if (author != null ? !author.equals(that.author) : that.author != null)
      return false;
    if (category != null ? !category.equals(that.category) : that.category != null)
      return false;
    if (created != null ? !created.equals(that.created) : that.created != null)
      return false;
    if (description != null ? !description.equals(that.description) : that.description != null)
      return false;
    if (globalIdentifier != null ?
        !globalIdentifier.equals(that.globalIdentifier) :
        that.globalIdentifier != null)
      return false;
    if (label != null ? !label.equals(that.label) : that.label != null)
      return false;
    if (license != null ? !license.equals(that.license) : that.license != null)
      return false;
    if (localIdentifier != null ?
        !localIdentifier.equals(that.localIdentifier) :
        that.localIdentifier != null)
      return false;
    if (tags != null ? !tags.equals(that.tags) : that.tags != null)
      return false;
    if (type != null ? !type.equals(that.type) : that.type != null)
      return false;
    if (updated != null ? !updated.equals(that.updated) : that.updated != null)
      return false;
    if (getLinks() != null ? !getLinks().equals(that.getLinks()) : that.getLinks() != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (globalIdentifier != null ? globalIdentifier.hashCode() : 0);
    result = 31 * result + (localIdentifier != null ? localIdentifier.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (active ? 1 : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (category != null ? category.hashCode() : 0);
    result = 31 * result + (author != null ? author.hashCode() : 0);
    result = 31 * result + (license != null ? license.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (updated != null ? updated.hashCode() : 0);
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    result = 31 * result + (getLinks() != null ? getLinks().hashCode() : 0);
    return result;
  }
}
