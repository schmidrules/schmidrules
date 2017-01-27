package org.schmidrules.configuration.dto;

import org.schmidrules.configuration.LocationListener;
import org.schmidrules.dependency.Location;

public class PackageReferenceDto {
    private String name;
    private Location location;

    public PackageReferenceDto(String name, int elementLength) {
        this.name = name;
        location = LocationListener.getLocation(elementLength);
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
