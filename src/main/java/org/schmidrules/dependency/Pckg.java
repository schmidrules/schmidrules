package org.schmidrules.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Pckg implements Comparable<Pckg> {
    public static final Pckg DEFAULT = new Pckg(".");

    private final String name;
    private final Location location;
    private final Map<Pckg, Collection<String>> dependsOn = new HashMap<>();

    public Pckg(String name, Location location) {
        this.name = name.intern();
        this.location = location;
    }

    public Pckg(String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void addDependency(Pckg dependency, String accessor) {
        Collection<String> accessors = dependsOn.get(dependency);

        if (accessors == null) {
            accessors = new HashSet<>();
            dependsOn.put(dependency, accessors);
        }

        accessors.add(accessor);
    }

    public Map<Pckg, Collection<String>> getDependencies() {
        return dependsOn;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Pckg other = (Pckg) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Pckg other) {
        return name.compareTo(other.name);
    }
}
