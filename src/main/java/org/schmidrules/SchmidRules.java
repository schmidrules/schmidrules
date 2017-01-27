package org.schmidrules;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.schmidrules.check.BuildPhase;
import org.schmidrules.check.DependencyCheckPhase;
import org.schmidrules.check.UsageCheckPhase;
import org.schmidrules.check.violation.Violation;
import org.schmidrules.configuration.ConfigurationLoader;
import org.schmidrules.configuration.dto.ArchitectureDto;

public class SchmidRules {

    private final ArchitectureDto architecture;
    private final Collection<File> baseDirs;

    public SchmidRules(ArchitectureDto architecture, Collection<File> baseDirs) {
        this.architecture = architecture;
        this.baseDirs = baseDirs;
    }

    public SchmidRules(File configFile, Collection<File> baseDirs) {
        this(new ConfigurationLoader(configFile.getAbsolutePath()).getArchitecture(), baseDirs);
    }

    public Collection<Violation> check() {
        Collection<Violation> violations = new ArrayList<>();

        BuildPhase builder = new BuildPhase(architecture);

        DependencyCheckPhase checker = new DependencyCheckPhase(builder.getComponents(), architecture.getConfiguration().getTargetScope());

        Stream<String> sources = architecture.getConfiguration().getSources().stream();

        for (File baseDir : baseDirs) {
            sources.map(src -> new File(baseDir, src))//
                    .filter(File::isDirectory)//
                    .forEach(checker::check);
        }
        violations.addAll(checker.getViolations());

        UsageCheckPhase usageChecker = new UsageCheckPhase(builder.getGrants());
        violations.addAll(usageChecker.getViolations());

        return violations;
    }

}
