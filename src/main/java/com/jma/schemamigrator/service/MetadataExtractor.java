package com.jma.schemamigrator.service;

import com.jma.schemamigrator.model.Column;
import com.jma.schemamigrator.model.Constraint;
import com.jma.schemamigrator.model.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataExtractor {

    public Table extractTable(String tableName, String schema, String url, String user, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = null; // Usually not used in Oracle
            String schemaPattern = schema;

            Table table = new Table();
            table.setName(tableName);
            table.setColumns(extractColumns(metaData, catalog, schemaPattern, tableName));
            table.setPrimaryKey(extractPrimaryKey(metaData, catalog, schemaPattern, tableName));
            table.setForeignKeys(extractForeignKeys(metaData, catalog, schemaPattern, tableName));
            table.setUniqueConstraints(extractUniqueConstraints(metaData, catalog, schemaPattern, tableName));
            // Check constraints require querying ALL_CONSTRAINTS / ALL_CONS_COLUMNS, not via DatabaseMetaData
            table.setCheckConstraints(extractCheckConstraints(conn, schema, tableName));
            return table;
        }
    }

    private List<Column> extractColumns(DatabaseMetaData metaData, String catalog, String schema, String table) throws SQLException {
        List<Column> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog, schema, table, null)) {
            while (rs.next()) {
                Column col = new Column();
                col.setName(rs.getString("COLUMN_NAME"));
                col.setDataType(rs.getInt("DATA_TYPE"));
                col.setTypeName(rs.getString("TYPE_NAME"));
                col.setColumnSize(rs.getInt("COLUMN_SIZE"));
                col.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                col.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                col.setDefaultValue(rs.getString("COLUMN_DEF"));
                columns.add(col);
            }
        }
        return columns;
    }

    private Constraint extractPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String table) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, table)) {
            List<String> columns = new ArrayList<>();
            String pkName = null;
            while (rs.next()) {
                pkName = rs.getString("PK_NAME");
                columns.add(rs.getString("COLUMN_NAME"));
            }
            if (!columns.isEmpty()) {
                return Constraint.primaryKey(pkName, columns);
            }
        }
        return null;
    }

    private List<Constraint> extractForeignKeys(DatabaseMetaData metaData, String catalog, String schema, String table) throws SQLException {
        List<Constraint> fks = new ArrayList<>();
        try (ResultSet rs = metaData.getImportedKeys(catalog, schema, table)) {
            // May return multiple rows per FK (one per column)
            // We need to group by FK_NAME
            java.util.Map<String, Constraint> map = new java.util.HashMap<>();
            while (rs.next()) {
                String fkName = rs.getString("FK_NAME");
                if (fkName == null) continue;
                String column = rs.getString("FKCOLUMN_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                String pkColumn = rs.getString("PKCOLUMN_NAME");
                Constraint fk = map.computeIfAbsent(fkName,
                        k -> Constraint.foreignKey(fkName, new ArrayList<>(), pkTable, new ArrayList<>()));
                fk.getColumns().add(column);
                fk.getReferencedColumns().add(pkColumn);
            }
            fks.addAll(map.values());
        }
        return fks;
    }

    private List<Constraint> extractUniqueConstraints(DatabaseMetaData metaData, String catalog, String schema, String table) throws SQLException {
        List<Constraint> uniques = new ArrayList<>();

        // First, get all primary key columns to exclude them
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, table)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        // Get all indexes that are unique
        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, table, true, false)) {
            Map<String, Constraint> map = new HashMap<>();

            while (rs.next()) {
                // Skip non-unique indexes
                if (!rs.getBoolean("NON_UNIQUE")) { // NON_UNIQUE=false means it's a unique index
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName == null) continue;

                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName == null) continue;

                    // Skip if this column is part of the primary key
                    if (pkColumns.contains(columnName)) continue;

                    Constraint uc = map.computeIfAbsent(indexName,
                            k -> Constraint.unique(indexName, new ArrayList<>()));
                    uc.getColumns().add(columnName);
                }
            }
            uniques.addAll(map.values());
        }
        return uniques;
    }

    private List<Constraint> extractCheckConstraints(Connection conn, String schema, String table) throws SQLException {
        List<Constraint> checks = new ArrayList<>();
        String sql = """
                SELECT ac.constraint_name, ac.search_condition, acc.column_name
                FROM all_constraints ac
                JOIN all_cons_columns acc ON ac.constraint_name = acc.constraint_name
                    AND ac.owner = acc.owner
                WHERE ac.owner = ? AND ac.table_name = ? AND ac.constraint_type = 'C'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<String, Constraint> map = new java.util.HashMap<>();
                while (rs.next()) {
                    String name = rs.getString("constraint_name");
                    String condition = rs.getString("search_condition");
                    String column = rs.getString("column_name");
                    Constraint cc = map.computeIfAbsent(name,
                            k -> Constraint.check(name, new ArrayList<>(), condition));
                    cc.getColumns().add(column);
                }
                checks.addAll(map.values());
            }
        }
        return checks;
    }
}
