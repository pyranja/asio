package at.ac.univie.isc.asio.metadata;

import org.springframework.hateoas.ResourceSupport;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe a dataset.
 */
public final class DatasetDescription extends ResourceSupport {
  /**
   * Create with minimal required attributes.
   * @return valid dataset descriptor with minimal content.
   */
  public static DatasetDescription create(final String identifier, final boolean isActive) {
    final DatasetDescription description = new DatasetDescription();
    description.setGlobalIdentifier(identifier);
    description.setLocalIdentifier(identifier);
    description.setLabel(identifier);
    description.setActive(isActive);
    description.setType("Dataset");
    return description;
  }

  DatasetDescription() {}

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

  public String getLocalIdentifier() {
    return localIdentifier;
  }

  public void setLocalIdentifier(final String localIdentifier) {
    this.localIdentifier = localIdentifier;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(final boolean active) {
    this.active = active;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(final String category) {
    this.category = category;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(final String author) {
    this.author = author;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(final String license) {
    this.license = license;
  }

  public ZonedDateTime getCreated() {
    return created;
  }

  public void setCreated(final ZonedDateTime created) {
    this.created = created;
  }

  public ZonedDateTime getUpdated() {
    return updated;
  }

  public void setUpdated(final ZonedDateTime updated) {
    this.updated = updated;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(final List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "DatasetDescriptor{localIdentifier='" + localIdentifier + "', type='" + type + "'}";
  }
}
