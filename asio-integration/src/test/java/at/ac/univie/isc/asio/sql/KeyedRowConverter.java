package at.ac.univie.isc.asio.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * convert : row -> (key, [columns])
 * 
 * @author Chris Borckholder
 */
public class KeyedRowConverter implements
		ResultSetIterator.RowConverter<KeyedRow> {

	private int columnCount = -1;
	private final String keyColumn;

	public KeyedRowConverter(final String keyColumn) {
		super();
		this.keyColumn = keyColumn;
	}

	@Override
	public KeyedRow process(final ResultSet rs) throws SQLException {
		if (columnCount < 0) {
			columnCount = rs.getMetaData().getColumnCount();
		}
		final String key = rs.getString(keyColumn);
		final List<String> columns = new ArrayList<>(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			columns.add(rs.getString(i));
		}
		return new KeyedRow(key, columns);
	}
}