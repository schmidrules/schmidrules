package org.schmidrules.configuration.dto;

import static org.schmidrules.configuration.dto.Tags.COMPONENT_REFERENCE_TAG;
import static org.schmidrules.configuration.dto.Tags.PACKAGE_REFERENCE_TAG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class ComponentDto {

    private String id;
    private String description;
    private List<PackageReferenceDto> publicPackages;
    private List<PackageReferenceDto> internalPackages = new ArrayList<>();
    private List<PackageReferenceDto> packageDependencies = new ArrayList<>();
    private List<ComponentReferenceDto> componentDependencies = new ArrayList<>();

    @XmlAttribute(name = "id", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(required = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElementWrapper(name = "publicPackages")
    @XmlElement(name = PACKAGE_REFERENCE_TAG)
    @XmlJavaTypeAdapter(PackageReferenceAdapter.class)
    public List<PackageReferenceDto> getPublicPackages() {
        return publicPackages;
    }

    public void setPublicPackages(List<PackageReferenceDto> publicPackages) {
        this.publicPackages = publicPackages;
    }

    @XmlElementWrapper(name = "internalPackages")
    @XmlElement(name = PACKAGE_REFERENCE_TAG)
    @XmlJavaTypeAdapter(PackageReferenceAdapter.class)
    public List<PackageReferenceDto> getInternalPackages() {
        return internalPackages;
    }

    public void setInternalPackages(List<PackageReferenceDto> internalPackages) {
        this.internalPackages = internalPackages;
    }

    @XmlElementWrapper(name = "packageDependencies")
    @XmlElement(name = PACKAGE_REFERENCE_TAG)
    @XmlJavaTypeAdapter(PackageReferenceAdapter.class)
    public List<PackageReferenceDto> getPackageDependencies() {
        return packageDependencies;
    }

    public void setPackageDependencies(List<PackageReferenceDto> packageDependencies) {
        this.packageDependencies = packageDependencies;
    }

    public List<ComponentReferenceDto> getSafeComponentDependencies() {
        if (componentDependencies == null) {
            return Collections.emptyList();
        }

        return componentDependencies.stream().collect(Collectors.toList());
    }

    /**
     * @deprecated Use <code>getSafeComponentDependencies</code> instead. This is kept around for JAXB only.
     */
    @XmlElementWrapper(name = "componentDependencies")
    @XmlElement(name = COMPONENT_REFERENCE_TAG)
    @Deprecated
    @XmlJavaTypeAdapter(ComponentReferenceAdapter.class)
    public List<ComponentReferenceDto> getComponentDependencies() {
        return componentDependencies;
    }

    public void setComponentDependencies(List<ComponentReferenceDto> componentDependencies) {
        this.componentDependencies = componentDependencies;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (!(object instanceof ComponentDto)) {
            return false;
        }

        final ComponentDto that = (ComponentDto) object;

        if ((id != null) ? (!id.equals(that.getId())) : (that.getId() != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : 0;
    }
}
