package org.schmidrules.dependency.c;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

import org.schmidrules.dependency.AnalyzedFile;
import org.schmidrules.dependency.AnalyzedFile.Builder;
import org.schmidrules.dependency.Analyzer;
import org.schmidrules.dependency.Dependency;
import org.schmidrules.dependency.Linifier;
import org.schmidrules.dependency.Linifier.Line;
import org.schmidrules.dependency.Pckg;

public class CFileAnalyzer extends Analyzer {

    public CFileAnalyzer() {
        super(Arrays.asList("c", "h"));
    }

    @Override
    public AnalyzedFile analyze(File file, File sourceDir, Charset charset) throws IOException {
        Pckg pckg = packageFromPath(file, sourceDir);
        Builder result = new AnalyzedFile.Builder(pckg + "/" + file.getName(), pckg);

        try (final FileInputStream fis = new FileInputStream(file)) {
            Linifier.linify(fis, charset).forEach(line -> parseLine(result, line));
        }

        return result.build();
    }

    private static void parseLine(Builder result, Line line) {
        Regex.matchPackageFromIncludeLine(line.text) //
                .map(p -> new Dependency(p, line.location)) //
                .ifPresent(result::addDependency);
    }

    private static Pckg packageFromPath(File file, File sourceDir) {
        Path relativePath = sourceDir.toPath().relativize(file.toPath());

        Path parent = relativePath.getParent();

        if (parent == null) {
            return Pckg.DEFAULT;
        }

        return new Pckg(parent.toString().replaceAll("\\\\", "/").toLowerCase());
    }
}
