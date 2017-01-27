package org.schmidrules.configuration;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.schmidrules.configuration.dto.ArchitectureDto;
import org.schmidrules.configuration.dto.ComponentDto;
import org.schmidrules.configuration.dto.ConfigurationDto;
import org.schmidrules.configuration.dto.PackageReferenceDto;

public class ArchitectureTest {

    @Test
    public void shouldMarshalAndUnMarshalArchitecture() {
        ArchitectureDto architecture = new ArchitectureDto();

        ConfigurationDto configuration = new ConfigurationDto();
        List<String> sources = new ArrayList<>();
        sources.add("src/main/java");
        configuration.setSources(sources);
        // configuration.setRefererScope("org.archi");
        configuration.setTargetScope("org.archi");
        // configuration.setGlobalPackageDependencies(globalPackageDependencies);
        architecture.setConfiguration(configuration);

        List<ComponentDto> components = new ArrayList<>();
        ComponentDto component1 = new ComponentDto();
        component1.setId("dao");
        component1.setDescription("Service package is allowed to access dao.");

        List<PackageReferenceDto> publicPackages = new ArrayList<>();
        publicPackages.add(new PackageReferenceDto("org.archi.service", 1));
        component1.setPublicPackages(publicPackages);

        List<PackageReferenceDto> allows = new ArrayList<>();
        allows.add(new PackageReferenceDto("org.archi.dao",1));
        allows.add(new PackageReferenceDto("org.archi.dao2",1));
        component1.setPackageDependencies(allows);

        components.add(component1);
        architecture.setComponents(components);

        String xml = XmlUtil.marshal(architecture);
        System.out.println(xml);

        ArchitectureDto result = XmlUtil.unmarshal(new StringReader(xml), ArchitectureDto.class);
        assertEquals(architecture.getComponents().get(0), result.getComponents().get(0));
    }

}
