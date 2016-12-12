package com.patientping.hiring.persistence;

import com.google.common.base.Strings;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public final class Mappings {
  static URI asUri(final String uri) {
    return uri == null ? null : URI.create(uri);
  }

  static BigDecimal asDecimal(final String decimal) {
    return decimal == null ? null : new BigDecimal(decimal);
  }

  static LocalDate asLocalDate(final Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
  }

  static Instant asInstant(final Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  static boolean hasColumn(final ResultSetMetaData rsmd, final String name) throws SQLException {
    if(!Strings.isNullOrEmpty(name)) {
      for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
        if (name.equalsIgnoreCase(rsmd.getColumnName(i)) || name.equalsIgnoreCase(rsmd.getColumnLabel(i))) {
          return true;
        }
      }
    }
    return false;
  }

  private Mappings() {}

  public static Integer asInteger(final ResultSet r, final String name) throws SQLException {
    final int value = r.getInt(name);
    return r.wasNull() ? null : value;
  }

  static String getColumnName(final Map<String, String> mappings, final String column) {
    return mappings.getOrDefault(column, column);
  }
}
