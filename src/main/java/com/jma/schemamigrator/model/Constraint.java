package com.jma.schemamigrator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.jma.schemamigrator.model.Constraint.ConstraintType.PRIMARY_KEY;

@Data
@Builder
public class Constraint {
    private String name;
    private ConstraintType type;
    private List<String> columns;          // columns in this table
    private String referencedTable;        // for FK
    private List<String> referencedColumns;// for FK
    private String checkCondition;         // for CHECK

    public enum ConstraintType { PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK }

    public static Constraint primaryKey(String name, List<String> columns) {
        return Constraint.builder().name(name).type(PRIMARY_KEY).columns(columns).build();
    }

    public static Constraint foreignKey(String name, List<String> columns, String refTable, List<String> refColumns) {
        return Constraint.builder().name(name).type(ConstraintType.FOREIGN_KEY)
                .columns(columns).referencedTable(refTable).referencedColumns(refColumns).build();
    }

    public static Constraint unique(String name, List<String> columns) {
        return Constraint.builder().name(name).type(ConstraintType.UNIQUE).columns(columns).build();
    }

    public static Constraint check(String name, List<String> columns, String condition) {
        return Constraint.builder().name(name).type(ConstraintType.CHECK).columns(columns).checkCondition(condition).build();
    }
}
