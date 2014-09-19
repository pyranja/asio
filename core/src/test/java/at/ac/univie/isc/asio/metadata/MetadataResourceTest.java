package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Column;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import at.ac.univie.isc.asio.config.JaxrsSpec;
import at.ac.univie.isc.asio.engine.sql.XmlSchemaType;
import at.ac.univie.isc.asio.jaxrs.DisableAuthorizationFilter;
import at.ac.univie.isc.asio.jaxrs.EmbeddedServer;
import at.ac.univie.isc.asio.jaxrs.ManagedClient;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

  private final Supplier<DatasetMetadata> metaSource = Suppliers.ofInstance(StaticMetadata.MOCK_METADATA);
  private final Supplier<SqlSchema> schemaSource = Suppliers.ofInstance(MOCK_SQL_SCHEMA);

  private final JSONProvider json = new JSONProvider();
  @Rule
  public Timeout timeout = new Timeout(2000);
  @Rule
  public EmbeddedServer service = EmbeddedServer
      .host(JaxrsSpec.create(MetadataResource.class, DisableAuthorizationFilter.class))
      .resource(new MetadataResource(metaSource, schemaSource))
      .enableLogging()
      .clientConfig(ManagedClient.create().use(json))
      .create();

  private WebTarget endpoint;

  @Before
  public void setup() {
    endpoint = service.endpoint().path("meta");
    json.setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio", "asio"));
  }

  @Test
  public void should_fetch_xml_metadata() throws Exception {
    final Response response =
        endpoint.request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_fetch_json_metadata() throws Exception {
    final Response response =
        endpoint.request(MediaType.APPLICATION_JSON).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(DatasetMetadata.class), is(StaticMetadata.MOCK_METADATA));
  }

  @Test
  public void should_fetch_xml_schema() throws Exception {
    final Response response =
        endpoint.path("schema").request(MediaType.APPLICATION_XML).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(SqlSchema.class), is(MOCK_SQL_SCHEMA));
  }

  @Test
  public void should_fetch_json_schema() throws Exception {
    final Response response =
        endpoint.path("schema").request(MediaType.APPLICATION_JSON).get();
    assertThat(response, hasFamily(Response.Status.Family.SUCCESSFUL));
    assertThat(response.readEntity(SqlSchema.class), is(MOCK_SQL_SCHEMA));
  }
}
