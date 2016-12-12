package com.patientping.hiring.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public final class Ordering {
  private static final Pattern RAW_FIELD_PATTERN = Pattern.compile("^[^ ]+$");
  private static final Splitter.MapSplitter ORDERING_SPLITTER = Splitter.on(',')
      .trimResults()
      .omitEmptyStrings()
      .withKeyValueSeparator(Splitter.on(' ').trimResults().omitEmptyStrings());

  private final String property;
  private final Direction direction;

  public Ordering(final String property, final Direction direction) {
    this.property = property;
    this.direction = direction;
  }

  public String getProperty() {
    return property;
  }
  public Direction getDirection() {
    return direction;
  }

  public static ImmutableList<Ordering> parse(final String ordering) {
    return Strings.isNullOrEmpty(ordering) ? ImmutableList.of() :
        ImmutableList.copyOf(ORDERING_SPLITTER
        .split(ordering)
        .entrySet()
        .stream()
        .map(e -> new Ordering(e.getKey(), Direction.fromString(e.getValue())))
        .iterator());
  }

  public static ImmutableList<Ordering> parse(final String ordering, Ordering defaultOrdering) {
    return Strings.isNullOrEmpty(ordering) ? ImmutableList.of(defaultOrdering) :
        isRawField(ordering) ? ImmutableList.of(new Ordering(ordering, defaultOrdering.getDirection())) :
        ImmutableList.copyOf(ORDERING_SPLITTER
            .split(ordering)
            .entrySet()
            .stream()
            .map(e -> new Ordering(e.getKey(), Direction.fromString(e.getValue())))
            .iterator());
  }

  public static String toString(final Collection<Ordering> ordering) {
    return ordering
        .stream()
        .map(o -> o.getProperty() + ' ' + o.getDirection().getName())
        .collect(Collectors.joining(","));
  }

  static boolean isRawField(final String ordering) {
    return RAW_FIELD_PATTERN.matcher(ordering).matches();
  }

  public enum Direction {
    ASCENDING("ascending"),
    DESCENDING("descending");
    
    final String name;

    Direction(final String name) {
      this.name = name;
    }

    @JsonValue
    public String getName() {
      return name;
    }

    private final static Map<String, Direction> names =
        stream(Direction.values()).collect(toMap(Direction::getName, Function.identity()));

    @JsonCreator
    public static Direction fromString(final String name) {
      return names.get(name);
    }

    private static final Direction[] values = values();
    public static Direction valueOf(final int index) {
      return values[index];
    }
  }
}
