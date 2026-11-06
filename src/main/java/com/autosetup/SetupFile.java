package com.autosetup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and edits setup.txt in the ZenithProxy folder (next to the launcher).
 *
 * One command per line, written like in Discord (leading "." optional).
 * Blank lines and lines starting with # or // are ignored.
 */
public final class SetupFile {
    public static final Path PATH = Path.of("setup.txt");

    private SetupFile() {}

    public static boolean exists() {
        return Files.isRegularFile(PATH);
    }

    /** Full path, for showing the user where the file should go. */
    public static String location() {
        return PATH.toAbsolutePath().normalize().toString();
    }

    /** The commands in the file, cleaned up. Empty if the file is missing. */
    public static List<String> readCommands() throws IOException {
        List<String> commands = new ArrayList<>();
        for (String line : readLines()) {
            if (isCommand(line)) commands.add(AutoSetupPlugin.clean(line));
        }
        return commands;
    }

    /** Adds a command at a position (1 = first). Creates the file if needed. Returns its new number. */
    public static int insert(int number, String cmd) throws IOException {
        List<String> lines = readLines();
        List<Integer> commandLines = commandLineIndexes(lines);
        int position = Math.max(1, Math.min(number, commandLines.size() + 1));
        if (position > commandLines.size()) {
            lines.add(cmd);
        } else {
            lines.add(commandLines.get(position - 1), cmd);
        }
        writeLines(lines);
        return position;
    }

    /** Adds a command to the end. Returns its number. */
    public static int add(String cmd) throws IOException {
        return insert(Integer.MAX_VALUE, cmd);
    }

    /** Removes command number N (1 = first). Returns it, or null if there's no such number. */
    public static String remove(int number) throws IOException {
        List<String> lines = readLines();
        List<Integer> commandLines = commandLineIndexes(lines);
        if (number < 1 || number > commandLines.size()) return null;
        String removed = lines.remove((int) commandLines.get(number - 1));
        writeLines(lines);
        return AutoSetupPlugin.clean(removed);
    }

    /** Removes every command but keeps comments. */
    public static void clear() throws IOException {
        if (!exists()) return;
        List<String> lines = readLines();
        lines.removeIf(SetupFile::isCommand);
        writeLines(lines);
    }

    private static boolean isCommand(String line) {
        String s = line.trim();
        return !s.isEmpty() && !s.startsWith("#") && !s.startsWith("//") && !AutoSetupPlugin.clean(s).isEmpty();
    }

    private static List<Integer> commandLineIndexes(List<String> lines) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (isCommand(lines.get(i))) indexes.add(i);
        }
        return indexes;
    }

    private static List<String> readLines() throws IOException {
        if (!exists()) return new ArrayList<>();
        List<String> lines = new ArrayList<>(Files.readAllLines(PATH, StandardCharsets.UTF_8));
        // Windows Notepad sometimes puts an invisible marker at the start of the file
        if (!lines.isEmpty() && lines.get(0).startsWith("﻿")) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    private static void writeLines(List<String> lines) throws IOException {
        Files.write(PATH, lines, StandardCharsets.UTF_8);
    }
}
