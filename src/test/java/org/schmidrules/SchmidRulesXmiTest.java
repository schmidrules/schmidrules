package org.schmidrules;

import org.junit.Test;
import org.schmidrules.configuration.ConfigurationLoader;
import org.schmidrules.configuration.dto.ArchitectureDto;

public class SchmidRulesXmiTest {

    // TODO quick and dirty test with output and without assert
    @Test
    public void shouldCreateXmi() throws Exception {
        ConfigurationLoader configurationLoader = new ConfigurationLoader("schmid-sample.xml");
        ArchitectureDto architecture = configurationLoader.getArchitecture();
        SchmidRulesXmi schmidRulesXmi = new SchmidRulesXmi(architecture, "SCHMID-SAMPLE");
        schmidRulesXmi.createXmi(System.out);
    }
}
