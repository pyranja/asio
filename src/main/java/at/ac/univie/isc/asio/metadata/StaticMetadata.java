package at.ac.univie.isc.asio.metadata;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 3/20/14 ; Time: 11:51 AM
 */
public class StaticMetadata {

  private static final ObjectFactory CREATOR = new ObjectFactory();
  private static final DatatypeFactory TYPES = makeFactory();

  private static DatatypeFactory makeFactory() {
    try {
      return DatatypeFactory.newInstance();
    } catch (final DatatypeConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  public static final DatasetMetadata MOCK_METADATA = CREATOR
      .createDatasetMetadata()
      .withName("test-dataset")
      .withGlobalID(CREATOR.createDatasetMetadataGlobalID("test-globalid"))
      .withLocalID("test-localid")
      .withSparqlEndPoint("http://asio.com/sparql")
      .withStatus(MetadataStatus.ACTIVE)
      .withType(ResourceType.DATASET)
      .withAuthor(CREATOR.createDatasetMetadataAuthor("test-author"))
      .withCreationDate(
          CREATOR.createDatasetMetadataCreationDate(TYPES.newXMLGregorianCalendar(2000, 1, 1, 12,
              0, 0, 0, 0)))
      .withUpdateDate(
          CREATOR.createDatasetMetadataUpdateDate(TYPES.newXMLGregorianCalendar(200, 1, 2, 12, 0,
              0, 0, 0)))
      .withDescription(CREATOR.createDatasetMetadataDescription("my test dataset"))
      .withLicence(CREATOR.createDatasetMetadataLicence(LicenceType.MIT))
      .withViews(CREATOR.createDatasetMetadataViews(2));

  public static final DatasetMetadata NOT_AVAILABLE = CREATOR.createDatasetMetadata()
      .withName("unknown").withGlobalID(CREATOR.createDatasetMetadataGlobalID("unknown"))
      .withLocalID("unknown").withSparqlEndPoint("unknown").withStatus(MetadataStatus.NON_ACTIVE)
      .withType(ResourceType.DATASET).withAuthor(CREATOR.createDatasetMetadataAuthor("unknown"))
      .withDescription(CREATOR.createDatasetMetadataDescription("no information is available"));
}
