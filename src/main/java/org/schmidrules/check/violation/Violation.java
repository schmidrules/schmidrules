package org.schmidrules.check.violation;

import org.schmidrules.dependency.Location;

public abstract class Violation {
    public final String description;
    public final Location location;
    private final Severity severity;

    public Violation(String description) {
        this(Severity.ERROR, description, null);
    }

    public Violation(Severity severity, String description, Location location) {
        this.description = description;
        this.location = location;
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Violation [description=" + description + ", location=" + location + "]";
    }

    public enum Severity {
        ERROR, WARN
    }
}
