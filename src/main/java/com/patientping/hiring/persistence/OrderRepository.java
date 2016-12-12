package com.patientping.hiring.persistence;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.patientping.hiring.core.Order;
import com.patientping.hiring.core.Ordering;
import com.patientping.hiring.core.Token;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindMap;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@UseStringTemplate3StatementLocator
public abstract class OrderRepository implements AutoCloseable, Transactional<OrderRepository> {
  static final Ordering ID_ORDERING = new Ordering("id", Ordering.Direction.ASCENDING);
  static final String COLUMNS = "orders.id,"  +
      "orders.tickets," +
      "shows.id as show_id," +
      "shows.doorsOpenInstant as show_doorsOpenInstant," +
      "users.id as buyer_id," +
      "users.login as buyer_login";

  static final String FROM = " from admitone.orders" +
      " inner join admitone.users on users.id = orders.buyer" +
      " inner join admitone.shows on shows.id = orders.show";

  static final String USER_PREDICATE = "users.id = :userId";

  static final Map<String, String> COLUMN_MAPPINGS = ImmutableMap.<String, String>builder()
      .put("id", "orders.id")
      .put("tickets", "videos.created_at")
      .put("show.id", "shows.id")
      .put("show.doorOpenInstant", "shows.doorOpenInstant")
      .put("user.id", "users.id")
      .put("user.login", "users.login")
      .build();

  @SqlQuery("select " + COLUMNS + FROM + " where orders.id = :id")
  public abstract Order getOrder(@Bind("id") long id);

  public ImmutableList<Order> findOrders(final String filter, final Collection<Ordering> ordering, final int skip, final int limit, final Token token) {
    final Filter compiledFilter;
    try {
      compiledFilter = withUserPermissions(Filter.compile(filter, COLUMN_MAPPINGS), token.getUserId(), token.isAdmin());
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException("filter", ex);
    }
    final String orderingClause;
    try {
      final Collection<Ordering> consistentOrdering;
      final Ordering lastOrdering = Iterables.getLast(ordering, null);
      consistentOrdering = (lastOrdering == null || !lastOrdering.getProperty().equals("id")) ?
          ImmutableList.<Ordering>builder().addAll(ordering).add(ID_ORDERING).build() :
          ordering;
      orderingClause = toString(consistentOrdering);
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException("ordering", ex);
    }

    return findOrders(compiledFilter.getSqlWhereClause(), orderingClause, compiledFilter.getLiterals(), skip, limit);
  }

  @SqlQuery("select " + COLUMNS + FROM + " <criteria> <ordering> limit :skip, :limit")
  public abstract ImmutableList<Order> findOrders(@Define("criteria") String criteria, @Define("ordering") String ordering, @BindMap Map<String, Object> literals, @Bind("skip") int skip, @Bind("limit") int limit);

  public int count(final String filter, final Token token) {
    final Filter compiledFilter;
    try {
      compiledFilter = withUserPermissions(Filter.compile(filter, COLUMN_MAPPINGS), token.getUserId(), token.isAdmin());
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException("filter", ex);
    }
    return count(compiledFilter.getSqlWhereClause(), compiledFilter.getLiterals());
  }

  @SqlQuery("select count(*)" + FROM + " <criteria>")
  public abstract int count(@Define("criteria") String criteria, @BindMap Map<String, Object> literals);

  public boolean deleteOrder(long id, Token token) {
    return (token.isAdmin() ? deleteOrder(id) : deleteOrder(id, token.getUserId())) > 0;
  }

  @SqlUpdate("delete" + FROM + "where orders.id = :id")
  public abstract int deleteOrder(@Bind("id") long id);

  @SqlUpdate("delete" + FROM + "where orders.id = :id and orders.buyer = :userId")
  public abstract int deleteOrder(@Bind("id") long id, @Bind("userId") long userId);

  public long createOrder(final Order order, final Token token) {
    return createOrder(order.getTickets(), order.getShow() == null || order.getShow().getId() == null ? token.getUserId() : order.getShow().getId(), order.getBuyer().getId());
  }

  @SqlUpdate("insert into admitone.orders (tickets, `show`, buyer) values (:tickets, :show, :buyer)")
  public abstract long createOrder(@Bind("tickets") int tickets, @Bind("show") long showId, @Bind("buyer") long userId);

  static String toString(final Collection<Ordering> orderings) {
    if(orderings == null || orderings.size() == 0) {
      return "";
    }

    final ArrayList<String> orderClauses = new ArrayList<>();
    for(final Ordering ordering: orderings) {
      final String mappedColumn = COLUMN_MAPPINGS.get(ordering.getProperty());
      if(mappedColumn == null) {
        throw new IllegalArgumentException(ordering.getProperty());
      }
      orderClauses.add(mappedColumn + ' ' + (Ordering.Direction.ASCENDING == ordering.getDirection() ? "" : "DESC"));
    }
    return "order by " + Joiner.on(", ").join(orderClauses);
  }

  static Filter withUserPermissions(final Filter filter, final long userId, final boolean isAdmin) {
    return isAdmin ? filter : filter.and(USER_PREDICATE, ImmutableMap.of("userId", userId));
  }

  public void updateOrder(Order order, Token token) {
    if(token.isAdmin()) {
      updateOrderByAdmin(order.getId(), order.getTickets(), order.getShow() != null ? order.getShow().getId() : null, order.getBuyer() != null ? order.getBuyer().getId() : null);
    } else {
      updateOrder(order.getId(), order.getTickets(), order.getShow() != null ? order.getShow().getId() : null, token.getUserId());
    }
  }

  @SqlUpdate("update admitone.orders set tickets = case when :tickets is not null then :tickets else tickets end, `show` = case when :show is not null then :show else `show` end, buyer = case when :buyer is not null then :buyer else buyer end where orders.id = :id")
  public abstract void updateOrderByAdmin(@Bind("id") long id, @Bind("tickets") Integer tickets, @Bind("show") Long showId, @Bind("buyer") Long userId);

  @SqlUpdate("update admitone.orders set tickets = case when :tickets is not null then :tickets else tickets end, `show` = case when :show is not null then :show else `show` end where orders.id = :id and buyer = :buyer")
  public abstract void updateOrder(@Bind("id") long id, @Bind("tickets") Integer tickets, @Bind("show") Long showId, @Bind("buyer") Long userId);
}
