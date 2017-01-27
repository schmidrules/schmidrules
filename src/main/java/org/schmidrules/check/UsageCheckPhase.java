package org.schmidrules.check;

import java.util.ArrayList;
import java.util.Collection;

import org.schmidrules.check.grant.AccessGrant;
import org.schmidrules.check.violation.Violation;

/**
 * Generates violations if access rules were not used.
 */
public class UsageCheckPhase {

    private final Collection<AccessGrant> grants;
    private final Collection<Violation> violations = new ArrayList<>();

    public UsageCheckPhase(Collection<AccessGrant> grants) {
        this.grants = grants;

        check();
    }

    private void check() {
        for (AccessGrant grant : grants) {
            if (!grant.wasNeeded()) {
                violations.add(grant.asUnusedViolation());
            }
        }
    }

    public Collection<Violation> getViolations() {
        return violations;
    }

}
