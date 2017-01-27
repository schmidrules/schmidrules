package org.schmidrules.dependency.c;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.schmidrules.dependency.Pckg;

public class Regex {
    private static final String PATH_SEPARATOR_UNIX = "/";
    private static final String WHITE_SPACE = "[ \t]+";
    private static final String OPT_WHITE_SPACE = "(" + WHITE_SPACE + ")?";
    private static final String FILE = "[_a-z0-9\\.]+";
    private static final String PATH = FILE + "(" + PATH_SEPARATOR_UNIX + FILE + ")*";
    private static final String FILE_LABEL = "[\"<](" + PATH + PATH_SEPARATOR_UNIX + ")?" + FILE + "[\">]";
    private static final String INCLUDE = OPT_WHITE_SPACE + "#include" + WHITE_SPACE + FILE_LABEL + OPT_WHITE_SPACE;

    private static final int INCLUDE_GROUP_FOLDER = 2;
    private static final Pattern PATTERN_INCLUDE = Pattern.compile(INCLUDE);

    public static Optional<Pckg> matchPackageFromIncludeLine(String line) {
        Matcher matcher = PATTERN_INCLUDE.matcher(line.toLowerCase());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        String pckg = matcher.group(INCLUDE_GROUP_FOLDER);

        if (pckg == null) {
            return Optional.of(Pckg.DEFAULT);
        }

        return Optional.of(new Pckg(pckg.substring(0, pckg.length() - 1)));
    }
}
