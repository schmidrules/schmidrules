package org.schmidrules.configuration.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "architecture")
public class ArchitectureDto {

    private ConfigurationDto configuration;

    private List<ComponentDto> components;

    @XmlElement(name = "configuration")
    public ConfigurationDto getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurationDto configuration) {
        this.configuration = configuration;
    }

    @XmlElementWrapper(name = "components")
    @XmlElement(name = "component")
    public List<ComponentDto> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentDto> components) {
        this.components = components;
    }

}
