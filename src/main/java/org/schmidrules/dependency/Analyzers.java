package org.schmidrules.dependency;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.schmidrules.dependency.c.CFileAnalyzer;
import org.schmidrules.dependency.java.JavaFileAnalyzer;

public class Analyzers {
    private static final Collection<Analyzer> analyzers = Collections.unmodifiableList(Arrays.asList( //
            new JavaFileAnalyzer(), //
            new CFileAnalyzer() //
    ));

    public static Optional<AnalyzedFile> analyze(File f, File sourceDir, Charset charset) {
        Optional<AnalyzedFile> result = analyzers.stream() //
                .map(a -> a.analyzeIfSuitable(f, sourceDir, charset)) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .findAny();
        return result;
    }
}
