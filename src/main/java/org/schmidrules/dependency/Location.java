package org.schmidrules.dependency;

public class Location {
    private static final int NO_DATA = -1;

    public final int lineNumber;
    public final int lineCharOffset;
    public final int charStart;
    public final int charEnd;

    public Location(int lineNumber, int lineCharOffset, int charStart, int charEnd) {
        this.lineNumber = lineNumber;
        this.lineCharOffset = lineCharOffset;
        this.charStart = charStart;
        this.charEnd = charEnd;
    }

    public Location(int lineNumber) {
        this(lineNumber, NO_DATA, NO_DATA, NO_DATA);
    }

    public boolean hasCharAccuracy() {
        return lineCharOffset != NO_DATA && //
                charStart != NO_DATA && //
                charEnd != NO_DATA;
    }

    @Override
    public String toString() {
        return "Location [lineNumber=" + lineNumber + ", lineCharOffset=" + lineCharOffset + ", charStart=" + charStart + ", charEnd="
                + charEnd + "]";
    }
}