package com.jma.schemamigrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Table {
    private String name;
    private List<Column> columns;
    private Constraint primaryKey;
    private List<Constraint> foreignKeys;
    private List<Constraint> uniqueConstraints;
    private List<Constraint> checkConstraints;
}
