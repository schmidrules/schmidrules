package org.schmidrules.check.violation;

import org.schmidrules.dependency.ComponentReference;

public class UnusedComponentGrantViolation extends Violation {

    public UnusedComponentGrantViolation(ComponentReference grant) {
        super(Severity.ERROR, "unused " + grant, grant.getLocation());
    }
}
