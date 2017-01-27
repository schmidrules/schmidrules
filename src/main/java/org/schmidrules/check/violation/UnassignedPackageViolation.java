package org.schmidrules.check.violation;

import org.schmidrules.dependency.Pckg;

public class UnassignedPackageViolation extends Violation {

    public final Pckg pckg;

    public UnassignedPackageViolation(Pckg pckg) {
        super(Severity.WARN, "Package " + pckg.getName() + " is not part of a component", pckg.getLocation());
        this.pckg = pckg;
    }
}
