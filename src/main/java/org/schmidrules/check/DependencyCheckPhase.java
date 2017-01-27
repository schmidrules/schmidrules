package org.schmidrules.check;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import org.schmidrules.check.violation.Violation;
import org.schmidrules.check.violation.AccessViolation;
import org.schmidrules.check.violation.UnassignedPackageViolation;
import org.schmidrules.dependency.DependencyAnalyzer;
import org.schmidrules.dependency.Location;
import org.schmidrules.dependency.Pckg;

/**
 * Verifies if the given architecture rules apply to some source files.
 */
public class DependencyCheckPhase {
    private final Collection<CheckableComponent> components;
    private final Collection<Violation> violations = new ArrayList<>();
    private final String targetScope;

    public DependencyCheckPhase(Collection<CheckableComponent> components, String targetScope) {
        this.components = components;
        this.targetScope = targetScope == null ? "" : targetScope;
    }

    public void check(File sourceDirectory) {
        Collection<Pckg> pckgs = new DependencyAnalyzer().createDependencyGraph(sourceDirectory);

        for (Pckg pckg : pckgs) {
            checkPckg(pckg);
        }
    }

    private void checkPckg(Pckg pckg) {
        CheckableComponent component = componentOf(pckg);

        if (component == null) {
            violations.add(new UnassignedPackageViolation(pckg));
            return;
        }

        for (Entry<Pckg, Collection<String>> dependency : pckg.getDependencies().entrySet()) {
            Pckg referredPckg = dependency.getKey();
            if (referredPckg.getName().startsWith(targetScope) && !component.isAllowedToAccess(referredPckg)) {
                violations.add(
                        new AccessViolation(new Location(1), component, referredPckg, componentOf(referredPckg), dependency.getValue()));
            }
        }
    }

    private CheckableComponent componentOf(Pckg pckg) {
        for (CheckableComponent component : components) {
            if (component.containsPackage(pckg)) {
                return component;
            }
        }

        return null;
    }

    public Collection<Violation> getViolations() {
        return violations;
    }
}
