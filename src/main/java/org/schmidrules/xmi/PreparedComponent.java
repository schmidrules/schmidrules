package org.schmidrules.xmi;

import java.util.ArrayList;
import java.util.List;

import org.schmidrules.configuration.dto.ComponentDto;

public class PreparedComponent {
    private final String xmiId;
    private final ComponentDto component;
    private final List<PreparedComponent> preparedComponentDependencies = new ArrayList<>();

    public PreparedComponent(ComponentDto component) {
        this.xmiId = IdGenerator.createId();
        this.component = component;
    }

    public String getXmiId() {
        return xmiId;
    }

    public ComponentDto getComponent() {
        return component;
    }

    public List<PreparedComponent> getPreparedComponentDependencies() {
        return preparedComponentDependencies;
    }

    public void addPreparedComponentDependency(PreparedComponent referred) {
        preparedComponentDependencies.add(referred);
    }

}
