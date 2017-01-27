package org.schmidrules;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.schmidrules.configuration.ConfigurationLoader;
import org.schmidrules.configuration.dto.ArchitectureDto;
import org.schmidrules.xmi.CreationPhase;
import org.schmidrules.xmi.PreparePhase;

public class SchmidRulesXmi {

    private final ArchitectureDto architecture;
    private final String projectName;

    public SchmidRulesXmi(ArchitectureDto architecture, String projectName) {
        this.architecture = architecture;
        this.projectName = projectName;
    }

    public SchmidRulesXmi(File configFile, String projectName) {
        this(new ConfigurationLoader(configFile.getAbsolutePath()).getArchitecture(), projectName);
    }

    public void createXmi(OutputStream out) throws IOException {
        PreparePhase preparer = new PreparePhase(architecture);
        CreationPhase creator = new CreationPhase(projectName, preparer.getComponents());
        creator.output(out);
    }

}
