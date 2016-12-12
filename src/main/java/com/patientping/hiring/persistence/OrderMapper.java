package com.patientping.hiring.persistence;

import com.patientping.hiring.core.Order;
import com.patientping.hiring.core.Show;
import com.patientping.hiring.core.User;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderMapper implements ResultSetMapper<Order> {
  @Override
  public Order map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
    return new Order(r.getLong("id"),
        new User(r.getLong("buyer_id"), r.getString("buyer_login")),
        new Show(r.getLong("show_id"), Mappings.asInstant(r.getTimestamp("show_doorsOpenInstant"))),
        r.getInt("tickets"));
  }
}
