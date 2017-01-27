package org.schmidrules.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.schmidrules.configuration.dto.ArchitectureDto;

public class ConfigurationLoaderTest {

    @Test
    public void shouldReadLoadConfiguration() {
        ArchitectureDto architecture = new ConfigurationLoader("schmid-sample.xml").getArchitecture();
        assertEquals("util",architecture.getComponents().get(0).getId());
    }
}
