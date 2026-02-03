package com.todolistp2p.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileHelpers {
    public static Path ensureDataDir() throws IOException {
        Path p = Path.of(AppConfig.DATA_DIR);
        if (!Files.exists(p)) Files.createDirectories(p);
        return p;
    }

    public static void appendLine(Path file, String line) throws IOException {
        ensureDataDir();
        Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static List<String> readAllLines(Path file) throws IOException {
        if (!Files.exists(file)) return List.of();
        return Files.readAllLines(file);
    }
}
