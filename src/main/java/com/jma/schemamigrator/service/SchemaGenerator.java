package com.jma.schemamigrator.service;

import com.jma.schemamigrator.model.Constraint;
import com.jma.schemamigrator.model.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaGenerator {

    private final MetadataExtractor metadataExtractor;
    private final TypeMapper typeMapper;

    public void generate(List<String> tableNames, String url, String user, String password,
                         String schema, File outputFile, boolean dropExisting)
            throws SQLException, IOException {

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Optional header
            writer.println("-- H2 schema generated from Oracle");
            writer.println("-- Generated at " + new java.util.Date());
            writer.println();

            for (String tableName : tableNames) {
                Table table = metadataExtractor.extractTable(tableName, schema, url, user, password);
                if (table.getColumns().isEmpty()) {
                    log.warn("Table {} not found or has no columns, skipping", tableName);
                    continue;
                }

                if (dropExisting) {
                    writer.println("DROP TABLE IF EXISTS " + tableName + " CASCADE;");
                }

                String createTable = buildCreateTable(table);
                writer.println(createTable);
                writer.println();

                // Add comments if needed (optional)
            }

            // Add foreign key constraints after all tables created
            for (String tableName : tableNames) {
                Table table = metadataExtractor.extractTable(tableName, schema, url, user, password);
                for (Constraint fk : table.getForeignKeys()) {
                    String alterFk = buildForeignKey(tableName, fk);
                    writer.println(alterFk);
                }
            }

            writer.flush();
        }
    }

    private String buildCreateTable(Table table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(table.getName()).append(" (\n");

        // Columns
        for (int i = 0; i < table.getColumns().size(); i++) {
            var col = table.getColumns().get(i);
            sb.append("  ").append(col.getName()).append(" ").append(typeMapper.formatColumnDefinition(col));
            if (i < table.getColumns().size() - 1) sb.append(",");
            sb.append("\n");
        }

        // Primary Key - Add comma before if we have columns
        if (table.getPrimaryKey() != null) {
            // Add comma before PRIMARY KEY if there are columns
            if (!table.getColumns().isEmpty()) {
                sb.append("  ,");  // Add comma and newline with indentation
            }
            sb.append("  CONSTRAINT ").append(table.getPrimaryKey().getName())
                    .append(" PRIMARY KEY (")
                    .append(String.join(", ", table.getPrimaryKey().getColumns()))
                    .append("),\n");
        }

        // Unique constraints
        for (Constraint uc : table.getUniqueConstraints()) {
            sb.append("  ,CONSTRAINT ").append(uc.getName())
                    .append(" UNIQUE (")
                    .append(String.join(", ", uc.getColumns()))
                    .append("),\n");
        }

        // Check constraints
        for (Constraint cc : table.getCheckConstraints()) {
            sb.append("  ,CONSTRAINT ").append(cc.getName())
                    .append(" CHECK (")
                    .append(cc.getCheckCondition())
                    .append("),\n");
        }

        // Remove trailing comma and newline if any constraints were added
        String result = sb.toString();
        if (result.endsWith(",\n")) {
            result = result.substring(0, result.length() - 2) + "\n";
        }

        result = result + ");";
        return result;
    }

    private String buildForeignKey(String tableName, Constraint fk) {
        return String.format(
                "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s);",
                tableName,
                fk.getName(),
                String.join(", ", fk.getColumns()),
                fk.getReferencedTable(),
                String.join(", ", fk.getReferencedColumns())
        );
    }
}
