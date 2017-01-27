package org.schmidrules.configuration.dto;

import static org.schmidrules.configuration.dto.Tags.PACKAGE_REFERENCE_TAG;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ConfigurationDto {

    private List<String> sources;
    private String targetScope = "";
    private List<PackageReferenceDto> globalPackageDependencies;

    @XmlElementWrapper(name = "sources", required = true)
    @XmlElement(name = "source", required = true)
    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    @XmlElement
    public String getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(String targetScope) {
        this.targetScope = targetScope;
    }

    @XmlElementWrapper(name = "globalPackageDependencies")
    @XmlElement(name = PACKAGE_REFERENCE_TAG)
    @XmlJavaTypeAdapter(PackageReferenceAdapter.class)
    public List<PackageReferenceDto> getGlobalPackageDependencies() {
        return globalPackageDependencies;
    }

    public void setGlobalPackageDependencies(List<PackageReferenceDto> globalPackageDependencies) {
        this.globalPackageDependencies = globalPackageDependencies;
    }
}
