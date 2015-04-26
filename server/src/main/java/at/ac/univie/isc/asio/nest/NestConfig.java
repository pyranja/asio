package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.database.Jdbc;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@AutoValue
abstract class NestConfig {
  /**
   * Create config holder with default settings only.
   */
  static NestConfig empty() {
    return create(new Dataset(), new Jdbc(), D2rqConfigModel.wrap(ModelFactory.createDefaultModel()));
  }

  /**
   * Create holder with given config beans.
   */
  static NestConfig create(final Dataset dataset, final Jdbc jdbc, final D2rqConfigModel d2rq) {
    return new AutoValue_NestConfig(dataset, jdbc, d2rq);
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
   * The d2rq configuration.
   */
  @JsonIgnore
  public abstract D2rqConfigModel getD2rq();
}
