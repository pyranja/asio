package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.database.Jdbc;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import de.fuberlin.wiwiss.d2rq.map.Mapping;

@AutoValue
abstract class NestConfig {
  /**
   * Create config holder with default settings only.
   */
  static NestConfig empty() {
    return create(new Dataset(), new Jdbc(), new Mapping(), ModelFactory.createDefaultModel());
  }

  /**
   * Create holder with given config beans.
   */
  static NestConfig create(final Dataset dataset, final Jdbc jdbc, final Mapping mapping, final Model model) {
    return new AutoValue_NestConfig(dataset, jdbc, mapping, model);
  }

  /**
   * General settings of the dataset.
   */
  @JsonUnwrapped
  public abstract Dataset getDataset();

  /**
   * Describe the connection to the backing database.
   */
  public abstract Jdbc getJdbc();

  /**
   * Mapping rules for d2rq.
   */
  @JsonIgnore
  public abstract Mapping getMapping();

  /**
   * Definition of d2rq mapping.
   */
  @JsonIgnore
  public abstract Model getMappingModel();
}
