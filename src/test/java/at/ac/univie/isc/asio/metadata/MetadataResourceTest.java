package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.config.JaxrsSpec;
import at.ac.univie.isc.asio.engine.sql.Column;
import at.ac.univie.isc.asio.engine.sql.SqlSchema;
import at.ac.univie.isc.asio.engine.sql.Table;
import at.ac.univie.isc.asio.jaxrs.EmbeddedServer;
import at.ac.univie.isc.asio.jaxrs.ManagedClient;
import at.ac.univie.isc.asio.jooq.XmlSchemaType;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataResourceTest {
  public static final SqlSchema MOCK_SQL_SCHEMA = new SqlSchema().withTable(
      new Table()
          .withName("testTable")
          .withCatalog("testCatalog")
          .withSchema("testSchema")
          .withColumn(
              new Column()
                  .withName("testColumn1")
                  .withType(XmlSchemaType.LONG.qname())
                  .withSqlType(XmlSchemaType.LONG.sqlType().getTypeName())
                  .withLength(10),
              new Column()
                  .withName("testColumn2")
                  .withType(XmlSchemaType.STRING.qname())
                  .withSqlType(XmlSchemaType.STRING.sqlType().getTypeName())
                  .withLength(50)
          )
  );

  private final Supplier<DatasetMetadata> metaSource = mock(Supplier.class);
  private final Supplier<SqlSchema> schemaSource = mock(Supplier.class);

  private final JSONProvider json = new JSONProvider();
  @Rule
  public Timeout timeout = new Timeout(2000);
  @Rule
  public EmbeddedServer service = EmbeddedServer
      .host(JaxrsSpec.create(MetadataResource.class))
      .resource(new MetadataResource(metaSource, schemaSource))
      .enableLogging()
      .clientConfig(ManagedClient.create().use(json))
      .create();

  private WebTarget endpoint;

  @Before
  public void setup() {
    endpoint = service.endpoint().path("{permission}").path("meta");
    when(metaSource.get()).thenReturn(StaticMetadata.MOCK_METADATA);
    when(schemaSource.get()).thenReturn(MOCK_SQL_SCHEMA);
    json.setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio", "asio"));
  }

  @Test
  public void should_fetch_xml_metadata() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_fetch_json_metadata() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").request(MediaType.APPLICATION_JSON).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_not_allow_access_to_metadata_for_unauthorized() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "none").request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasStatus(Response.Status.FORBIDDEN));
  }

  @Test
  public void should_fetch_xml_schema() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").path("schema")
            .request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(SqlSchema.class), is(MOCK_SQL_SCHEMA));
  }

  @Test
  public void should_fetch_json_schema() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "read").path("schema")
            .request(MediaType.APPLICATION_JSON).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(SqlSchema.class), is(MOCK_SQL_SCHEMA));
  }

  @Test
  public void should_not_allow_access_to_schema_for_unauthorized() throws Exception {
    final Response response =
        endpoint.resolveTemplate("permission", "none").path("schema")
            .request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasStatus(Response.Status.FORBIDDEN));
  }
}
