package org.schmidrules.xmi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.schmidrules.configuration.dto.ArchitectureDto;
import org.schmidrules.configuration.dto.ComponentDto;
import org.schmidrules.configuration.dto.ComponentReferenceDto;
import org.schmidrules.configuration.dto.ConfigurationException;

/**
 * Prepares all components for xmi. Creates unique xmi ids for all components and resolves their dependencies.
 */
public class PreparePhase {
    private final ArchitectureDto architecture;
    private final Map<String, PreparedComponent> componentsById = new HashMap<>();

    public PreparePhase(ArchitectureDto architecture) {
        this.architecture = architecture;
        prepareComponents();
    }

    public Collection<PreparedComponent> getComponents() {
        return componentsById.values();
    }

    private void prepareComponents() {
        if (architecture.getComponents() == null) {
            throw new ConfigurationException("No component declared by architecture");
        }

        for (ComponentDto c : architecture.getComponents()) {
            componentsById.put(c.getId(), new PreparedComponent(c));
        }

        for (ComponentDto c : architecture.getComponents()) {
            prepareComponentDependencies(c);
        }
    }

    private void prepareComponentDependencies(ComponentDto componentDefinition) {
        PreparedComponent referer = componentsById.get(componentDefinition.getId());

        for (ComponentReferenceDto referredComponent : componentDefinition.getSafeComponentDependencies()) {
            PreparedComponent referred = componentsById.get(referredComponent.getName());

            if (referred == null) {
                throw new ConfigurationException(
                        "Undefined component reference: " + referredComponent.getName() + " in component " + componentDefinition.getId());
            }

            referer.addPreparedComponentDependency(referred);
        }
    }

}
