package at.ac.univie.isc.asio.engine.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class CsvRecordMapperTest {
  private final DSLContext create = DSL.using(SQLDialect.MYSQL);
  private RecordMapper<Record, String[]> mapper = CsvWriter.RECORD_MAPPER();

  // ============= Boolean

  @Test
  public void true_bool_values() {
    final Record record = booleanRecord(true);
    final String[] row = mapper.map(record);
    assertThat(row, is(arrayContaining("true", "true")));
  }

  @Test
  public void false_bool_values() throws Exception {
    final Record record = booleanRecord(false);
    final String[] row = mapper.map(record);
    assertThat(row, is(arrayContaining("false", "false")));
  }

  @Test
  public void null_bool_values() throws Exception {
    final Record record = booleanRecord(null);
    final String[] row = mapper.map(record);
    assertThat(row, is(arrayContaining("null", "null")));
  }

  private Record booleanRecord(final Boolean value) {
    final Record record = create.newRecord(
        DSL.fieldByName(SQLDataType.BOOLEAN, "boolean")
        , DSL.fieldByName(SQLDataType.BIT, "bit")
    );
    record.fromArray(new Object[] {value, value});
    return record;
  }

  // ============= String

  @Test
  public void empty_string_values() throws Exception {
    final Record record = stringRecord("");
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("\"\"")));
  }

  @Test
  public void simple_string_values() throws Exception {
    final Record record = stringRecord("test");
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("\"test\"")));
  }

  @Test
  public void with_quote_string_values() throws Exception {
    final Record record = stringRecord("quo\"ted");
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("\"quo\"\"ted\"")));
  }

  @Test
  public void with_comma_string_values() throws Exception {
    final Record record = stringRecord("com,ma");
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("\"com,ma\"")));
  }

  @Test
  public void null_string_values() throws Exception {
    final Record record = stringRecord(null);
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("null")));
  }

  private Record stringRecord(final String value) {
    final Record record = create.newRecord(
        DSL.fieldByName(SQLDataType.CHAR, "char")
        , DSL.fieldByName(SQLDataType.VARCHAR, "varchar")
        , DSL.fieldByName(SQLDataType.LONGVARCHAR, "longvarchar")
        , DSL.fieldByName(SQLDataType.CLOB, "clob")
    );
    record.fromArray(new Object[] { value, value, value, value });
    return record;
  }

  // ============= DateTime

  @Test
  public void now_datetime_value() throws Exception {
    final Calendar now = Calendar.getInstance(Locale.ENGLISH);
    final Record record = create.newRecord(DSL.fieldByName(SQLDataType.TIMESTAMP, "timestamp"));
    record.fromArray(new Timestamp(now.getTimeInMillis()));
    final String[] row = mapper.map(record);
    final String expectedDateTime = DatatypeConverter.printDateTime(now);
    assertThat(Arrays.asList(row), everyItem(equalTo(expectedDateTime)));
  }

  @Test
  public void time_value() throws Exception {
    final Record record = create.newRecord(DSL.fieldByName(SQLDataType.TIME, "time"));
    record.fromArray("12:34:56");
    final String[] row = mapper.map(record);
    assertThat(row[0], startsWith("12:34:56.000")); // ignore timezone offset
  }

  @Test
  public void date_value() throws Exception {
    final Record record = create.newRecord(DSL.fieldByName(SQLDataType.DATE, "date"));
    record.fromArray("2014-12-24");
    final String[] row = mapper.map(record);
    assertThat(row[0], is("2014-12-24"));
  }

  @Test
  public void null_temporal_values() throws Exception {
    final Record record = create.newRecord(
        DSL.fieldByName(SQLDataType.TIMESTAMP, "timestamp")
        , DSL.fieldByName(SQLDataType.TIME, "time")
        , DSL.fieldByName(SQLDataType.DATE, "date")
    );
    record.fromArray(new Object[] { null, null, null });
    final String[] row = mapper.map(record);
    assertThat(Arrays.asList(row), everyItem(is("null")));
  }
}
