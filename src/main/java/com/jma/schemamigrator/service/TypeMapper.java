package com.jma.schemamigrator.service;

import com.jma.schemamigrator.model.Column;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

@Service
public class TypeMapper {

    private static final Map<Integer, String> SQL_TYPE_TO_H2 = new HashMap<>();
    private static final Map<String, String> ORACLE_TYPE_NAME_TO_H2 = new HashMap<>();

    static {
        // Map based on java.sql.Types constants
        SQL_TYPE_TO_H2.put(Types.CHAR, "CHAR");
        SQL_TYPE_TO_H2.put(Types.VARCHAR, "VARCHAR");
        SQL_TYPE_TO_H2.put(Types.NVARCHAR, "VARCHAR");
        SQL_TYPE_TO_H2.put(Types.LONGVARCHAR, "CLOB");
        SQL_TYPE_TO_H2.put(Types.NUMERIC, "NUMERIC");
        SQL_TYPE_TO_H2.put(Types.DECIMAL, "DECIMAL");
        SQL_TYPE_TO_H2.put(Types.INTEGER, "INT");
        SQL_TYPE_TO_H2.put(Types.SMALLINT, "SMALLINT");
        SQL_TYPE_TO_H2.put(Types.BIGINT, "BIGINT");
        SQL_TYPE_TO_H2.put(Types.REAL, "REAL");
        SQL_TYPE_TO_H2.put(Types.DOUBLE, "DOUBLE");
        SQL_TYPE_TO_H2.put(Types.FLOAT, "FLOAT");
        SQL_TYPE_TO_H2.put(Types.DATE, "DATE");
        SQL_TYPE_TO_H2.put(Types.TIME, "TIME");
        SQL_TYPE_TO_H2.put(Types.TIMESTAMP, "TIMESTAMP");
        SQL_TYPE_TO_H2.put(Types.BLOB, "BLOB");
        SQL_TYPE_TO_H2.put(Types.CLOB, "CLOB");
        SQL_TYPE_TO_H2.put(Types.BOOLEAN, "BOOLEAN");
        SQL_TYPE_TO_H2.put(Types.BINARY, "BINARY");
        SQL_TYPE_TO_H2.put(Types.VARBINARY, "VARBINARY");

        // Override with Oracle type name if needed
        ORACLE_TYPE_NAME_TO_H2.put("VARCHAR2", "VARCHAR");
        ORACLE_TYPE_NAME_TO_H2.put("NVARCHAR2", "VARCHAR");
        ORACLE_TYPE_NAME_TO_H2.put("NUMBER", "NUMERIC");
        ORACLE_TYPE_NAME_TO_H2.put("LONG", "CLOB");
        ORACLE_TYPE_NAME_TO_H2.put("RAW", "VARBINARY");
        ORACLE_TYPE_NAME_TO_H2.put("LONG RAW", "BLOB");
        ORACLE_TYPE_NAME_TO_H2.put("ROWID", "VARCHAR");
        ORACLE_TYPE_NAME_TO_H2.put("UROWID", "VARCHAR");
        ORACLE_TYPE_NAME_TO_H2.put("DATE", "TIMESTAMP"); // Oracle DATE includes time, map to TIMESTAMP
        ORACLE_TYPE_NAME_TO_H2.put("TIMESTAMP", "TIMESTAMP");
        ORACLE_TYPE_NAME_TO_H2.put("CLOB", "CLOB");
        ORACLE_TYPE_NAME_TO_H2.put("BLOB", "BLOB");
        ORACLE_TYPE_NAME_TO_H2.put("BFILE", "BLOB");
    }

    public String getH2Type(Column column) {
        // First try by Oracle type name (more specific)
        String oracleTypeName = column.getTypeName().toUpperCase();
        if (ORACLE_TYPE_NAME_TO_H2.containsKey(oracleTypeName)) {
            return ORACLE_TYPE_NAME_TO_H2.get(oracleTypeName);
        }
        // Fallback to SQL type code
        String h2Type = SQL_TYPE_TO_H2.get(column.getDataType());
        if (h2Type != null) {
            return h2Type;
        }
        // Default
        return "VARCHAR";
    }

    public String formatColumnDefinition(Column column) {
        String type = getH2Type(column);
        // Add size/precision if applicable
        if (type.matches("(?i)CHAR|VARCHAR|VARCHAR2|NVARCHAR2")) {
            type += "(" + column.getColumnSize() + ")";
        } else if (type.matches("(?i)NUMERIC|DECIMAL|NUMBER")) {
            if (column.getDecimalDigits() > 0) {
                type += "(" + column.getColumnSize() + "," + column.getDecimalDigits() + ")";
            } else if (column.getColumnSize() > 0) {
                type += "(" + column.getColumnSize() + ")";
            }
        }
        // Add NOT NULL if applicable
        if (!column.isNullable()) {
            type += " NOT NULL";
        }
        // Add DEFAULT if present
        if (column.getDefaultValue() != null && !column.getDefaultValue().isBlank()) {
            type += " DEFAULT " + column.getDefaultValue(); // Be careful with quoting – we assume it's valid SQL
        }
        return type;
    }
}