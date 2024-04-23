package org.schmidrules;

import java.io.ByteArrayOutputStream;

import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.DateScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;
import org.junit.Test;
import org.schmidrules.configuration.ConfigurationLoader;
import org.schmidrules.configuration.dto.ArchitectureDto;

public class SchmidRulesXmiTest {

    @Test
    public void shouldCreateXmi() throws Exception {
        ConfigurationLoader configurationLoader = new ConfigurationLoader("schmid-sample.xml");
        ArchitectureDto architecture = configurationLoader.getArchitecture();
        SchmidRulesXmi schmidRulesXmi = new SchmidRulesXmi(architecture, "SCHMID-SAMPLE");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        schmidRulesXmi.createXmi(out);
        
        String xml = new String(out.toByteArray());
        Approvals.verify(xml, approvalOptions);
    }
    
    Options approvalOptions = new Options(new MultiScrubber(
        new RegExScrubber("EAID_[0-9a-fA-F]{8}_[0-9a-fA-F]{4}_[0-9a-fA-F]{4}_[0-9a-fA-F]{4}_[0-9a-fA-F]{12}", n -> "EAID_" + n),
        new Scrubber[] { new DateScrubber("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}") }));

}
