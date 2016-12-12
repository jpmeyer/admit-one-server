package com.patientping.hiring.persistence;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.patientping.hiring.util.PatternUtils;
import com.patientping.hiring.expressions.FilterBaseVisitor;
import com.patientping.hiring.expressions.FilterLexer;
import com.patientping.hiring.expressions.FilterParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class Filter {
  static final String IS_NOT_NULL = " is not null";
  static final String IS_NULL = " is null";
  static final Filter EMPTY = new Filter("", ImmutableMap.of());

  private final String predicate;
  private final Map<String, Object> literals;

  public Filter(final String predicate, final Map<String, Object> literals) {
    this.predicate = predicate;
    this.literals = literals == null ? ImmutableMap.of() : ImmutableMap.copyOf(literals);
  }

  public String getSqlWhereClause() {
    return Strings.isNullOrEmpty(predicate) ? "" : "where " + predicate;
  }

  public Map<String, Object> getLiterals() {
    return literals;
  }

  public Filter and(final String predicate) {
    return logicalOperation(predicate, "and", true);
  }

  public Filter and(final String predicate, final Map<String, Object> literals) {
    return logicalOperation(predicate, "and", true, literals);
  }

  private Filter logicalOperation(final String predicate, final String operator, final boolean requiresParenthesis) {
    return new Filter(Strings.isNullOrEmpty(this.predicate) ? predicate : requiresParenthesis ? '(' + this.predicate + ") " + operator + ' ' + predicate : this.predicate + ' ' + operator + ' ' + predicate, literals);
  }

  private Filter logicalOperation(final String predicate, final String operator, final boolean requiresParenthesis, final Map<String, Object> literals) {
    final Map<String, Object> newLiterals;
    if(literals != null && !literals.isEmpty()) {
      final Map<String, Object> mergedLiterals = Maps.newHashMap();
      mergedLiterals.putAll(this.literals);
      mergedLiterals.putAll(literals);
      newLiterals = ImmutableMap.copyOf(mergedLiterals);
    } else {
      newLiterals = this.literals;
    }
    return new Filter(Strings.isNullOrEmpty(this.predicate) ? predicate : requiresParenthesis ? '(' + this.predicate + ") " + operator + ' ' + predicate : this.predicate + ' ' + operator + ' ' + predicate, newLiterals);
  }

  public static Filter compile(final String input, final Map<String, String> columnMappings) {
    if(Strings.isNullOrEmpty(input)) {
      return EMPTY;
    }
    final Visitor visitor = new Visitor(columnMappings);
    final String criteria = visitor.visitFilter(parse(input));
    return new Filter(criteria, visitor.getParameters());
  }

  static FilterParser.FilterContext parse(final String input) {
    final FilterLexer lexer = new FilterLexer(new ANTLRInputStream(input));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final FilterParser parser = new FilterParser(tokens);
    return parser.filter();
  }

  public String getPredicate() {
    return predicate;
  }

  private static final class Visitor extends FilterBaseVisitor<String> {
    private final Map<String, String> columnMappings;
    private final Map<String, Object> parameters = new HashMap<>();

    public Visitor(final Map<String, String> columnMappings) {
      this.columnMappings = columnMappings;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    @Override
    public String visitFilter(@NotNull final FilterParser.FilterContext ctx) {
      final String predicate = visitExpression(ctx.expression());
      return Strings.isNullOrEmpty(predicate) ? "" : predicate;
    }

    @Override
    public String visitExpression(@NotNull final FilterParser.ExpressionContext ctx) {
      final String result;
      switch(ctx.getChildCount()) {
        case 3:
          result = visitBinaryExpression(ctx);
          break;
        case 2:
          result = visitUnaryExpression(ctx);
          break;
        default:
          result = visitChildren(ctx);
      }
      return result;
    }

    private String visitBinaryExpression(@NotNull final FilterParser.ExpressionContext ctx) {
      final String operator = ((TerminalNode) ctx.getChild(1)).getSymbol().getText();
      switch(operator) {
        case "==":
        case "!=":
          if(isNull(ctx.expression(0))) {
            return isNull(ctx.expression(1)) ? "" : visit(ctx.expression(1)) + ("!=".equals(operator) ? IS_NOT_NULL : IS_NULL);
          } else if(isNull(ctx.expression(1))) {
            return visit(ctx.expression(0)) + ("!=".equals(operator) ? IS_NOT_NULL : IS_NULL);
          }
        default:
          return String.format(binaryExpressionFormats.get(operator), visit(ctx.expression(0)), visit(ctx.expression(1)));
      }
    }

    private boolean isNull(@NotNull final FilterParser.ExpressionContext ctx) {
      return (ctx.getChildCount() == 1 && ctx.getChild(0) instanceof FilterParser.NullContext);
    }

    private String visitUnaryExpression(@NotNull final FilterParser.ExpressionContext ctx) {
      final String operator = ((TerminalNode) ctx.getChild(0)).getSymbol().getText();
      return String.format(unaryExpressionFormats.get(operator), visit(ctx.expression(0)));
    }

    @Override
    public String visitParenthesizedExpression(@NotNull final FilterParser.ParenthesizedExpressionContext ctx) {
      return '(' + visitExpression(ctx.expression()) + ')';
    }

    @Override
    public String visitMember(@NotNull final FilterParser.MemberContext ctx) {
      final LinkedList<String> names = new LinkedList<>();
      FilterParser.MemberContext childCtx = ctx;
      do {
        if (childCtx.getChildCount() == 1) {
          names.addFirst(((TerminalNode) childCtx.getChild(0)).getSymbol().getText());
          break;
        } else {
          names.addFirst(((TerminalNode) childCtx.getChild(2)).getSymbol().getText());
          childCtx = childCtx.member();
        }
      } while (true);

      final String qualifiedName = Joiner.on('.').join(names);
      final String column = columnMappings.get(qualifiedName);
      if (Strings.isNullOrEmpty(column)) {
        throw new IllegalArgumentException(qualifiedName);
      }
      return column + ' ';
    }

    @Override
    public String visitFunction(@NotNull final FilterParser.FunctionContext ctx) {
      final String name = ((TerminalNode)ctx.getChild(0)).getSymbol().getText();
      final FilterParser.ExpressionListContext args = ctx.expressionList();
      final String result;
      switch(name) {
        case "date": // only support date literals
          final String quotedDateString = args.expression(0).literal().getText();
          final Instant dateValue = Instant.parse(quotedDateString.substring(1, quotedDateString.length() - 1));
          result = registerLiteral(Timestamp.from(dateValue));
          break;
        case "startsWith": {
          result = String.format("left(%1$s, len(%2$s)) == %2$s ", args.expression(0), args.expression(1));
          break;
        }
        case "endsWith": {
          result = String.format("right(%1$s, len(%2$s)) == %2$s ", args.expression(0), args.expression(1));
          break;
        }
        case "contains": {
          result = String.format("instr(%1$s, %2$s) ", args.expression(0), args.expression(1));
          break;
        }
        case "matches": {
          result = String.format("%1$s regexp %2$s ", args.expression(0), args.expression(1));
          break;
        }
        default:
          throw new IllegalArgumentException(name);
      }
      return result;
    }

    @Override
    public String visitInteger(@NotNull final FilterParser.IntegerContext ctx) {
      return registerLiteral(Integer.parseInt(ctx.getText()));
    }

    @Override
    public String visitDouble(@NotNull final FilterParser.DoubleContext ctx) {
      return registerLiteral(Double.parseDouble(ctx.getText()));
    }

    @Override
    public String visitString(@NotNull final FilterParser.StringContext ctx) {
      final String quotedValue = ctx.getText();
      final String value = PatternUtils.unescape_perl_string(quotedValue.substring(1, quotedValue.length() - 1));
      return registerLiteral(value);
    }

    @Override
    public String visitBoolean(@NotNull final FilterParser.BooleanContext ctx) {
      return registerLiteral(Boolean.parseBoolean(ctx.getText()));
    }

    @Override
    public String visitNull(@NotNull final FilterParser.NullContext ctx) {
      return registerLiteral(null);
    }

    @Override
    public String visitErrorNode(@NotNull final ErrorNode node) {
      throw new IllegalArgumentException();
    }

    private String registerLiteral(final Object value) {
      final String name = "_" + parameters.size();
      parameters.put(name, value);
      return ':' + name;
    }
  }

  private static final Map<String, String> binaryExpressionFormats = ImmutableMap.<String, String>builder()
      .put("==", "%1$s <=> %2$s ")
      .put("!=", "%1$s != %2$s ")
      .put("<", "%1$s < %2$s ")
      .put("<=", "%1$s <= %2$s ")
      .put(">=", "%1$s >= %2$s ")
      .put(">", "%1$s > %2$s ")
      .put("+", "%1$s + %2$s ")
      .put("-", "%1$s - %2$s ")
      .put("*", "%1$s * %2$s ")
      .put("/", "%1$s / %2$s ")
      .put("%", "%1$s %% %2$s ")
      .put("&&", "%1$s and %2$s ")
      .put("||", "%1$s or %2$s ")
      .put("^", "exp(%1$s, %2$s) ")
      .build();

  private static final Map<String, String> unaryExpressionFormats = ImmutableMap.<String, String>builder()
      .put("-", "- %1$s ")
      .put("+", "+ %1$s ")
      .put("!", "! %1$s ")
      .build();

  /* Operations by type:
  String
  startsWith()
  endsWith()
  matches()
  contains()
  ==
  !=

  Number
  <
  <=
  ==
  !=
  >=
  >

  +
  -
  *
  /
  %

  Boolean
  !
  &
  |
  ==
  !=

  Date
  date()
  <
  <=
  ==
  !=
  >=
  >
*/
}
