package com.fitness.db;

import java.util.List;

public class SqlHelper {

    /**
     * Replaces '?' placeholders in parameterized SQL with properly escaped SQL literals.
     */
    public static String formatSql(String sql, List<Object> params) {
        if (params == null || params.isEmpty()) {
            return sql;
        }

        StringBuilder sb = new StringBuilder();
        int paramIdx = 0;
        int len = sql.length();

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            if (c == '?') {
                if (paramIdx < params.size()) {
                    sb.append(toSqlLiteral(params.get(paramIdx++)));
                } else {
                    sb.append("NULL");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toSqlLiteral(Object val) {
        if (val == null) {
            return "NULL";
        }
        if (val instanceof Number) {
            return val.toString();
        }
        if (val instanceof Boolean) {
            return ((Boolean) val) ? "1" : "0";
        }
        String str = val.toString();
        return "'" + escapeSqlString(str) + "'";
    }

    public static String escapeSqlString(String str) {
        if (str == null) return "";
        return str.replace("'", "''");
    }
}
