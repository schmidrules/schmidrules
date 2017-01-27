package org.schmidrules.check;

import static org.schmidrules.check.Converters.fromDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.schmidrules.check.grant.AccessGrant;
import org.schmidrules.check.grant.PackageGrant;
import org.schmidrules.configuration.dto.ArchitectureDto;
import org.schmidrules.configuration.dto.ComponentDto;
import org.schmidrules.configuration.dto.ComponentReferenceDto;
import org.schmidrules.configuration.dto.ConfigurationException;
import org.schmidrules.configuration.dto.PackageReferenceDto;
import org.schmidrules.dependency.ComponentReference;

/**
 * Builds a network of access rules using component and package rules from an architecture description. Can also provide the grants for rule
 * usage verification.
 */
public class BuildPhase {
    private final ArchitectureDto architecture;
    private final Map<String, CheckableComponent> componentsById = new HashMap<>();
    private final Collection<AccessGrant> grants = new ArrayList<>();

    public BuildPhase(ArchitectureDto architecture) {
        this.architecture = architecture;
        build();
    }

    private void build() {
        buildComponents();

        addGlobalDependencies();
    }

    public Collection<CheckableComponent> getComponents() {
        return componentsById.values();
    }

    public Collection<AccessGrant> getGrants() {
        return grants;
    }

    private void buildComponents() {
        if (architecture.getComponents() == null) {
            throw new ConfigurationException("No component declared by architecture");
        }

        for (ComponentDto c : architecture.getComponents()) {
            componentsById.put(c.getId(), new CheckableComponent(c.getId()));
        }

        for (ComponentDto c : architecture.getComponents()) {
            buildComponent(c);
        }
    }

    private void buildComponent(ComponentDto componentDefinition) {
        CheckableComponent referer = componentsById.get(componentDefinition.getId());

        addComponentReferences(componentDefinition, referer);
        addPackages(referer, componentDefinition.getInternalPackages(), false);
        addPackages(referer, componentDefinition.getPublicPackages(), true);
        addPackageDependencies(referer, componentDefinition.getPackageDependencies());
    }

    private void addComponentReferences(ComponentDto componentDefinition, CheckableComponent referer) {
        for (ComponentReferenceDto dto : componentDefinition.getSafeComponentDependencies()) {
            ComponentReference reference = fromDto(dto);
            CheckableComponent referred = componentsById.get(reference.getName());

            if (referred == null) {
                throw new ConfigurationException(
                        "Undefined component reference: " + reference.getName() + " in component " + componentDefinition.getId(),
                        reference.getLocation());
            }

            referer.grant(register(referred.asGrant(reference)));
        }
    }

    private static void addPackages(CheckableComponent referer, List<PackageReferenceDto> packages, boolean publik) {
        if (packages == null) {
            return;
        }

        for (PackageReferenceDto pckg : packages) {
            referer.addPackage(fromDto(pckg), publik);
        }
    }

    private void addPackageDependencies(CheckableComponent referer, List<PackageReferenceDto> packages) {
        if (packages == null) {
            return;
        }

        for (PackageReferenceDto pckg : packages) {
            referer.grant(register(new PackageGrant(fromDto(pckg))));
        }
    }

    private void addGlobalDependencies() {
        for (PackageReferenceDto globalPackageDependency : architecture.getConfiguration().getGlobalPackageDependencies()) {
            AccessGrant grant = register(new PackageGrant(fromDto(globalPackageDependency)));
            for (CheckableComponent component : componentsById.values()) {
                component.grant(grant);
            }
        }
    }

    private AccessGrant register(AccessGrant grant) {
        grants.add(grant);

        return grant;
    }
}
