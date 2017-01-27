package org.schmidrules.dependency;

public class ComponentReference {

    private final String name;
    private final Location location;

    public ComponentReference(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "component-reference " + name;
    }
}
