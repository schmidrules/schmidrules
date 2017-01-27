package org.schmidrules.configuration.dto;

import org.schmidrules.dependency.Location;

public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 2927997046974817678L;

    private final Location location;

    public ConfigurationException(String message) {
        this(message, null);
    }

    public ConfigurationException(String message, Location location) {
        this(message, location, null);
    }

    public ConfigurationException(String message, Location location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
