package org.schmidrules;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.schmidrules.check.violation.Violation;

public class CLI {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("SchmidRules <config file> <base dir> [<base dir>]");
            System.exit(0);
        }
        List<File> fileArgs = Arrays.asList(args).stream(). //
                map(File::new). //
                collect(Collectors.toList());

        File configFile = fileArgs.get(0);
        List<File> baseDirs = fileArgs.subList(1, fileArgs.size());
        SchmidRules rules = new SchmidRules(configFile, baseDirs);

        Collection<Violation> violations = rules.check();
        violations.forEach(System.out::println);

        int error = violations.size() > 0 ? 1 : 0;
        System.exit(error);
    }

}
