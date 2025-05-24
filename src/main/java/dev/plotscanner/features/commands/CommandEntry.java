package dev.plotscanner.features.commands;

public record CommandEntry(String command, long delay) {
    public long getCommandCD() {
        return (50L * (command.length()) + 25L) + 100L;
    }
}
