package org.schmidrules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.schmidrules.check.violation.Violation;
import org.schmidrules.check.violation.Violation.Severity;

public class CLI {

    public static void main(String[] args) {
        if (args.length < 3) {
            printHelp();
            System.exit(0);
        }

        String mode = args[0];
        File configFile = new File(args[1]);
        List<File> fileArgs = Arrays.asList(args).stream(). //
                skip(2). //
                map(File::new). //
                collect(Collectors.toList());

        if ("check".equalsIgnoreCase(mode)) {
            int error = checkArchitecture(configFile, fileArgs);
            System.exit(error);
        }

        if ("xmi".equalsIgnoreCase(mode)) {
            int error = exportXmi(configFile, fileArgs.get(0));
            System.exit(error);
        }

        printHelp();
        System.exit(1);
    }

    private static void printHelp() {
        System.out.println("SchmidRules check <config file> <base dir> [<base dir>]");
        System.out.println("SchmidRules xmi <config file> <target xmi file>");
    }

    private static int checkArchitecture(File configFile, List<File> baseDirs) {
        SchmidRules rules = new SchmidRules(configFile, baseDirs);

        Collection<Violation> violations = rules.check();
        violations.forEach(System.out::println);

        Predicate<Violation> isError = violation -> violation.getSeverity() == Severity.ERROR;
        long errorCount = violations.stream().filter(isError).count();
        int error = errorCount > 0 ? 1 : 0;
        return error;
    }

    private static int exportXmi(File configFile, File target) {
        SchmidRulesXmi xmi = new SchmidRulesXmi(configFile, "SCHMID-SAMPLE");

        try (FileOutputStream out = new FileOutputStream(target)) {
            xmi.createXmi(out);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return 1;
        }
        return 0;
    }

}
