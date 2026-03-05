package com.jma.schemamigrator.command;

import com.jma.schemamigrator.service.SchemaGenerator;
import com.jma.schemamigrator.util.FileHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Component
@CommandLine.Command(name = "migrate", description = "Migrate Oracle table definitions to H2 schema", mixinStandardHelpOptions = true, version = "1.0")
public class MigrateCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-i", "--input-file"}, required = true, description = "File containing semicolon-separated list of tables")
    private File inputFile;
    @CommandLine.Option(names = {"-o", "--output-file"}, description = "Output SQL file (default: schema.sql)")
    private File outputFile = new File("schema.sql");
    @CommandLine.Option(names = {"--oracle-url"}, required = true, description = "Oracle JDBC URL (e.g., jdbc:oracle:thin:@//host:port/service)")
    private String oracleUrl;
    @CommandLine.Option(names = {"--oracle-user"}, required = true, description = "Oracle username")
    private String oracleUser;
    @CommandLine.Option(names = {"--oracle-password"}, required = true, description = "Oracle password", interactive = true, arity = "0..1")
    private String oraclePassword;
    @CommandLine.Option(names = {"--schema"}, description = "Oracle schema name (default: same as user)")
    private String schema;
    @CommandLine.Option(names = {"--drop-existing"}, description = "Include DROP TABLE statements before CREATE")
    private boolean dropExisting = false;
    private final SchemaGenerator schemaGenerator;

    public MigrateCommand(SchemaGenerator schemaGenerator) {
        this.schemaGenerator = schemaGenerator;
    }

    @Override
    public Integer call() throws Exception {
        // Resolve schema if not provided
        if (schema == null || schema.isBlank()) {
            schema = oracleUser.toUpperCase();
        }
        // Read table names from input file
        List<String> tables = FileHelper.readTableList(inputFile);
        if (tables.isEmpty()) {
            log.error("No tables found in input file: {}", inputFile);
            return 1;
        }
        // Generate schema
        schemaGenerator.generate(tables, oracleUrl, oracleUser, oraclePassword, schema, outputFile, dropExisting);
        log.info("Schema successfully written to {}", outputFile.getAbsolutePath());
        return 0;
    }
}