package com.jma.schemamigrator;

import com.jma.schemamigrator.command.MigrateCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class SchemaMigratorApplication implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine.IFactory iFactory;
    private final MigrateCommand migrateCommand;
    private int exitCode;

    public SchemaMigratorApplication(CommandLine.IFactory iFactory, MigrateCommand migrateCommand) {
        this.iFactory = iFactory;
        this.migrateCommand = migrateCommand;
    }

    public static void main(String[] args) {
        SpringApplication.run(SchemaMigratorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        CommandLine cmd = new CommandLine(migrateCommand, iFactory);
        exitCode = cmd.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}