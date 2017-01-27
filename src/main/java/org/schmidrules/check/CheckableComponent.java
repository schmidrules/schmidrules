package org.schmidrules.check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.schmidrules.check.grant.AccessGrant;
import org.schmidrules.check.grant.PackageGrant;
import org.schmidrules.check.violation.UnusedComponentGrantViolation;
import org.schmidrules.check.violation.Violation;
import org.schmidrules.dependency.ComponentReference;
import org.schmidrules.dependency.Pckg;

/**
 * A set of packages and access rules that can be verified.
 */
public class CheckableComponent {
    private final String id;
    /**
     * Things this component's packages are allowed to access.
     */
    private final List<AccessGrant> grants = new ArrayList<>();
    private final Set<Pckg> packages = new HashSet<>();
    private final Set<Pckg> publicPackages = new HashSet<>();

    public CheckableComponent(String id) {
        this.id = id;
    }

    public void grant(AccessGrant g) {
        grants.add(g);
    }

    public void addPackage(Pckg pckg, boolean publik) {
        packages.add(pckg);

        if (publik) {
            publicPackages.add(pckg);
        }

        // Note this internal grant is never registered/verified and it's good like that
        grants.add(new PackageGrant(pckg));
    }

    protected boolean allowsExternalAccessTo(Pckg pckg) {
        return publicPackages.contains(pckg);
    }

    public AccessGrant asGrant(ComponentReference reference) {
        return new AccessGrant() {
            @Override
            protected boolean checkAccess(Pckg pckg) {
                return allowsExternalAccessTo(pckg);
            }

            @Override
            public String toString() {
                return "component " + getId();
            }

            @Override
            public Violation asUnusedViolation() {
                return new UnusedComponentGrantViolation(reference);
            }
        };
    }

    public boolean containsPackage(Pckg pckg) {
        return packages.contains(pckg);
    }

    public String getId() {
        return id;
    }

    public boolean isAllowedToAccess(Pckg pckg) {
        for (AccessGrant grant : grants) {
            if (grant.allowsAccessTo(pckg)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "CheckableComponent [id=" + id + ", grants=" + grants + ", packages=" + packages + ", publicPackages=" + publicPackages
                + "]";
    }
}
