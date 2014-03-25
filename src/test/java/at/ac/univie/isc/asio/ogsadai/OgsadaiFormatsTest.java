package at.ac.univie.isc.asio.ogsadai;

import static at.ac.univie.isc.asio.DatasetOperation.Action.QUERY;
import static at.ac.univie.isc.asio.DatasetOperation.Action.SCHEMA;
import static at.ac.univie.isc.asio.ogsadai.OgsadaiFormats.CSV;
import static at.ac.univie.isc.asio.ogsadai.OgsadaiFormats.XML;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * regression warner for OgsadaiFormats
 * 
 * @author Chris Borckholder
 */
public class OgsadaiFormatsTest {

  @Test
  public void query_formats() throws Exception {
    assertTrue(XML.applicableOn(QUERY));
    assertTrue(CSV.applicableOn(QUERY));
  }

  @Test
  public void schema_formats() throws Exception {
    assertTrue(XML.applicableOn(SCHEMA));
  }
}
