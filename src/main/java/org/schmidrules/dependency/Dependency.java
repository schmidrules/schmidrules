package org.schmidrules.dependency;

import java.util.Objects;

/**
 * Dependency of a text unit. Package is the identity of this class, location is optional.
 */
public class Dependency {
    public final Pckg pckg;
    public final Location location;

    public Dependency(Pckg pckg, Location location) {
        Objects.requireNonNull(pckg);

        this.pckg = pckg;
        this.location = location;
    }

    @Override
    public String toString() {
        return "Dependency [pckg=" + pckg + ", location=" + location + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((pckg == null) ? 0 : pckg.hashCode());
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
        Dependency other = (Dependency) obj;
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (pckg == null) {
            if (other.pckg != null) {
                return false;
            }
        } else if (!pckg.equals(other.pckg)) {
            return false;
        }
        return true;
    }
}
