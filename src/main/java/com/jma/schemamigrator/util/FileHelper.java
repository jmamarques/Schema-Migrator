package com.jma.schemamigrator.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class FileHelper {


    public List<String> readTableList(java.io.File file) throws IOException {
        String content = Files.readString(file.toPath());
        return Arrays.stream(content.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
