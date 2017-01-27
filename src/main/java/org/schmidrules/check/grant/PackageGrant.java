package org.schmidrules.check.grant;

import org.schmidrules.check.violation.UnusedPackageGrantViolation;
import org.schmidrules.check.violation.Violation;
import org.schmidrules.dependency.Pckg;

/**
 * Grants access to a package.
 */
public class PackageGrant extends AccessGrant {
    private final Pckg granted;

    public PackageGrant(Pckg pckg) {
        this.granted = pckg;
    }

    @Override
    protected boolean checkAccess(Pckg pckg) {
        return this.granted.equals(pckg);
    }

    @Override
    public Violation asUnusedViolation() {
        return new UnusedPackageGrantViolation(granted, granted.getLocation());
    }

    @Override
    public String toString() {
        return "package " + granted.getName();
    }
}
