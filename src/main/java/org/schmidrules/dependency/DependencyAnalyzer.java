package org.schmidrules.dependency;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyAnalyzer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Collection<Pckg> sourcePackages = new TreeSet<>();
    private final Map<String, Pckg> packagesByName = new HashMap<>();

    public Collection<Pckg> createDependencyGraph(File... sourceDirectories) {
        for (File sourceDirectory : sourceDirectories) {
            logger.info("analyzing " + sourceDirectory);
            analyze(sourceDirectory, sourceDirectory);
        }
        return sourcePackages;
    }

    private void analyze(File file, File sourceDirectory) {
        if (file.isDirectory()) {
            for (String name : file.list()) {
                analyze(new File(file, name), sourceDirectory);
            }
            return;
        }

        Optional<AnalyzedFile> result = Analyzers.analyze(file, sourceDirectory, Charset.defaultCharset());

        if (!result.isPresent()) {
            logger.debug("no suitable analyzer, skipping {}", file);
            return;
        }

        AnalyzedFile af = result.get();
        logger.debug(af.dump());

        Pckg pckg = internReference(af.getPackage());
        sourcePackages.add(pckg);

        af.getDependencies().stream() //
                .map(dep -> dep.pckg).map(this::internReference) //
                .forEach(dep -> pckg.addDependency(dep, af.toString()));
    }

    private Pckg internReference(Pckg anotherPackage) {
        Pckg pckg = packagesByName.get(anotherPackage.getName());

        if (pckg == null) {
            pckg = anotherPackage;
            packagesByName.put(pckg.getName(), pckg);
        }

        return pckg;
    }
}
