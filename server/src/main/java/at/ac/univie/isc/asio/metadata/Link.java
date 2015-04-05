package at.ac.univie.isc.asio.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Describe a relation from some entity to another. This represents a subset of the link definition
 * in RFC 5988 and is compatible with corresponding representation techniques.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5988#section-4">RFC 5988</a>
 */
@AutoValue
public abstract class Link {
  public static final String SELF = "self";

  /**
   * Create the special link with {@code 'self'} relation.
   *
   * @param href IRI to representation of containing resource
   * @return a self link
   */
  public static Link self(final String href) {
    return create(href, SELF, href);
  }

  /**
   * Create a link without title. Alias for {@code Link.create(href, relation, null)}.
   *
   * @param href IRI of target
   * @param relation the type of relationship
   * @return link with given href and rel, but without a title.
   */
  public static Link untitled(final String href, final String relation) {
    return create(href, relation, href);
  }

  /**
   * Create a link. {@code href} SHOULD be a valid IRI, but this is not enforced.
   *
   * @param href IRI of target
   * @param relation the type of relationship with target
   * @param title human readable description
   * @return link instance
   */
  @JsonCreator
  public static Link create(@JsonProperty("href") final String href,
                            @JsonProperty("rel") final String relation,
                            @JsonProperty("title") @Nullable final String title) {
    return new AutoValue_Link(href, relation, title);
  }

  Link() { /* prevent sub-classing */ };

  @JsonProperty("href")
  public abstract String getHref();

  @JsonProperty("rel")
  public abstract String getRelation();

  @JsonProperty("title")
  @Nullable
  public abstract String getTitle();
}
