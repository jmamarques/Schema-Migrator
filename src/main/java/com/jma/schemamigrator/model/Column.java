package com.jma.schemamigrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    private String name;
    private int dataType;       // java.sql.Types
    private String typeName;    // Oracle type name
    private int columnSize;
    private int decimalDigits;
    private boolean nullable;
    private String defaultValue;
}