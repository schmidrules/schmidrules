package org.schmidrules.dependency.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.schmidrules.dependency.AnalyzedFile;
import org.schmidrules.dependency.AnalyzedFile.Builder;
import org.schmidrules.dependency.Analyzer;
import org.schmidrules.dependency.Dependency;
import org.schmidrules.dependency.Linifier;
import org.schmidrules.dependency.Linifier.Line;
import org.schmidrules.dependency.Location;
import org.schmidrules.dependency.Pckg;

public class JavaFileAnalyzer extends Analyzer {
    private static final String WHITE_SPACE = "[ \t]+";

    private static final String PACKAGE_NAME_PART_PATTERN_STRING = "[_a-z0-9][_a-zA-Z0-9]*";
    private static final String PACKAGE_NAME_PATTERN_STRING = PACKAGE_NAME_PART_PATTERN_STRING + "([\\./]"
            + PACKAGE_NAME_PART_PATTERN_STRING + ")*";
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package" + WHITE_SPACE + "(" + PACKAGE_NAME_PATTERN_STRING + ");");
    private static final int PACKAGE_PATTERN_GROUP_NAME = 1;

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "import(" + WHITE_SPACE + "static)?" + WHITE_SPACE + "(" + PACKAGE_NAME_PATTERN_STRING + ")\\.([_A-Za-z0-9\\.]+)(\\.\\*)?;");
    private static final int IMPORT_PATTERN_GROUP_PACKAGE = 2;

    public JavaFileAnalyzer() {
        super(Arrays.asList("java"));
    }

    @Override
    public AnalyzedFile analyze(File file, File sourceDir, Charset charset) throws IOException {
        Builder result = AnalyzedFile.newBuilder();

        try (final FileInputStream fis = new FileInputStream(file)) {
            Linifier.linify(fis, charset).forEach(line -> {
                if (result.getPckg() == null) {
                    searchPackage(result, line);
                }

                searchDependencies(result, line);
            });
        }

        result.setName(result.getPckg() + "." + file.getName().substring(0, file.getName().length() - 5));

        return result.build();
    }

    private static void searchDependencies(Builder result, Line line) {
        Matcher matcher = IMPORT_PATTERN.matcher(line.text);
        if (matcher.find()) {
            Location location = matchLocation(line, matcher);
            Pckg pckg = new Pckg(matcher.group(IMPORT_PATTERN_GROUP_PACKAGE));
            result.addDependency(new Dependency(pckg, location));
        }
    }

    private static Location matchLocation(Line line, Matcher matcher) {
        return new Location(line.location.lineNumber, line.location.lineCharOffset, matcher.start(), matcher.end());
    }

    private static void searchPackage(Builder result, Line line) {
        Matcher matcher = PACKAGE_PATTERN.matcher(line.text);
        if (matcher.find()) {
            Location location = matchLocation(line, matcher);
            result.setPckg(new Pckg(matcher.group(PACKAGE_PATTERN_GROUP_NAME), location));
        }
    }
}
