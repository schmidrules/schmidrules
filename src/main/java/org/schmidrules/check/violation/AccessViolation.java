package org.schmidrules.check.violation;

import java.util.Collection;

import org.schmidrules.check.CheckableComponent;
import org.schmidrules.dependency.Location;
import org.schmidrules.dependency.Pckg;

public class AccessViolation extends Violation {
    public final CheckableComponent accessor;
    public final Pckg accessedPackage;
    public final CheckableComponent accessedComponent;

    public AccessViolation(Location location, CheckableComponent accessor, Pckg accessedPackage, CheckableComponent accessedComponent) {
        super(Severity.ERROR, "Component " + accessor.getId() + " is not allowed to access " + accessedPackage, location);
        this.accessor = accessor;
        this.accessedPackage = accessedPackage;
        this.accessedComponent = accessedComponent;
    }

    public AccessViolation(Location location, CheckableComponent accessor, Pckg accessedPackage, CheckableComponent accessedComponent,
            Collection<String> accessorPackages) {
        super(Severity.ERROR,
                "Component " + accessor.getId() + " is not allowed to access " + accessedPackage + " (accessors=" + accessorPackages + ")",
                location);
        this.accessor = accessor;
        this.accessedPackage = accessedPackage;
        this.accessedComponent = accessedComponent;
    }
}
