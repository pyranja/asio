package at.ac.univie.isc.asio.metadata;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 3/20/14 ; Time: 11:51 AM
 */
public class MockMetadata {

  public static final ObjectFactory CREATOR = new ObjectFactory();
  public static final DatatypeFactory TYPES = makeFactory();

  private static DatatypeFactory makeFactory() {
    try {
      return DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  public static final DatasetMetadata INSTANCE = CREATOR.createDatasetMetadata()
      .withGlobalID(CREATOR.createDatasetMetadataGlobalID("test-globalid"))
      .withLocalID("test-localid")
      .withSparqlEndPoint("http://asio.com/sparql")
      .withStatus(MetadataStatus.ACTIVE).withType(ResourceType.DATASET)
      .withAuthor
          (CREATOR.createDatasetMetadataAuthor("test-author"))
      .withCreationDate(CREATOR.createDatasetMetadataCreationDate(
          TYPES.newXMLGregorianCalendar(2000, 1, 1, 12, 00, 00, 00, 0)))
      .withUpdateDate(CREATOR.createDatasetMetadataUpdateDate(
          TYPES.newXMLGregorianCalendar(200, 1, 2, 12, 00, 00, 00, 0)))
      .withDescription(CREATOR.createDatasetMetadataDescription("my test dataset"))
      .withLicence(CREATOR.createDatasetMetadataLicence(LicenceType.MIT))
      .withViews(CREATOR.createDatasetMetadataViews(2))
      ;
}
