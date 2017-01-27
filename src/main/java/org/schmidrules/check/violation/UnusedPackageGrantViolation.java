package org.schmidrules.check.violation;

import org.schmidrules.dependency.Location;
import org.schmidrules.dependency.Pckg;

public class UnusedPackageGrantViolation extends Violation {

    public UnusedPackageGrantViolation(Pckg granted, Location location) {
        super(Severity.ERROR, "grant unused: package " + granted, location);
    }
}
