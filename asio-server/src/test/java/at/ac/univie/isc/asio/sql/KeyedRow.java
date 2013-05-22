package at.ac.univie.isc.asio.sql;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * DTO (key, [columns])
 * 
 * @author Chris Borckholder
 */
public class KeyedRow {

	private final String key;
	private final List<String> columns;

	public KeyedRow(final String key, final List<String> columns) {
		super();
		this.key = key;
		this.columns = ImmutableList.copyOf(columns);
	}

	/**
	 * @return primary key value of this row
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return all column values of this row (including primary key)
	 */
	public List<String> getColumns() {
		return columns;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KeyedRow)) {
			return false;
		}
		final KeyedRow other = (KeyedRow) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (columns == null) {
			if (other.columns != null) {
				return false;
			}
		} else if (!columns.equals(other.columns)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("KeyedRow [key=%s, columns=%s]", key, columns);
	}
}
