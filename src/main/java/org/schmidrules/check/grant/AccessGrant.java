package org.schmidrules.check.grant;

import org.schmidrules.check.violation.Violation;
import org.schmidrules.dependency.Pckg;

/**
 * Grants access to something. The idea is that every config-statement resolves to an access grant. This way it's usage can be verified.
 */
public abstract class AccessGrant {

    private boolean wasNeeded;

    public final boolean allowsAccessTo(Pckg pckg) {
        boolean allowed = checkAccess(pckg);

        if (allowed) {
            wasNeeded = true;
        }

        return allowed;
    }

    protected abstract boolean checkAccess(Pckg pckg);

    public boolean wasNeeded() {
        return wasNeeded;
    }

    public void resetUsage() {
        wasNeeded = false;
    }

    public abstract Violation asUnusedViolation();
}
