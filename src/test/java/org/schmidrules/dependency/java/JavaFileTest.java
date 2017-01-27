package org.schmidrules.dependency.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.schmidrules.dependency.AnalyzedFile;
import org.schmidrules.dependency.Pckg;

public class JavaFileTest {

    private static final String PACKAGE = "a.a.a";
    private static final String UGLY_PACKAGE = "a.aA.a";
    private static final String CLASS = "AaAa";
    private static final String METHOD = "aaAa";
    private static final String CONSTANT = "AA_AA";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldDetectPackage() {
        AnalyzedFile src = analyzeFileWith(PACKAGE);

        assertEquals(PACKAGE, src.getPackage().getName());
    }

    @Test
    public void shouldDetectImport() {
        AnalyzedFile src = analyzeFileWith(PACKAGE, "import " + PACKAGE + "." + CLASS + ";");

        assertContainsPackage(src, PACKAGE);
    }

    @Test
    public void shouldDetectStaticMethodImport() {
        AnalyzedFile src = analyzeFileWith(PACKAGE, "import static " + PACKAGE + "." + CLASS + "." + METHOD + ";");

        assertContainsPackage(src, PACKAGE);
    }

    @Test
    public void shouldDetectStaticClassImport() {
        AnalyzedFile src = analyzeFileWith(PACKAGE, "import static " + PACKAGE + "." + CLASS + "." + CLASS + ";");

        assertContainsPackage(src, PACKAGE);
    }

    @Test
    public void shouldDetectStaticConstantImport() {
        AnalyzedFile src = analyzeFileWith(PACKAGE, "import static " + PACKAGE + "." + CLASS + "." + CONSTANT + ";");

        assertContainsPackage(src, PACKAGE);
    }

    @Test
    public void shouldDetectUglyPackageDeclaration() {
        AnalyzedFile src = analyzeFileWith(UGLY_PACKAGE);

        assertEquals(UGLY_PACKAGE, src.getPackage().getName());
    }

    @Test
    public void shouldDetectUglyPackageDependency() {
        AnalyzedFile src = analyzeFileWith(PACKAGE, "import " + UGLY_PACKAGE + "." + CLASS + ";");

        assertContainsPackage(src, UGLY_PACKAGE);
    }

    private static void assertContainsPackage(AnalyzedFile src, String... pckgs) {
        Collection<Pckg> dependencies = src.getDependencies().stream()//
        		.map(dep -> dep.pckg)
        		.collect(Collectors.toList());

        for (String pckg : pckgs) {
            if (!dependencies.contains(pckg(pckg))) {
                fail(pckg + " not in " + dependencies);
            }
        }
    }

    private AnalyzedFile analyzeFileWith(String packageName, String... importStatements) {
        try {
            File source = folder.newFile("MyClass.java");
            try (PrintStream ps = new PrintStream(new FileOutputStream(source))) {
                ps.println("package " + packageName + ";");
                for (String importStatement : importStatements) {
                    ps.println(importStatement);
                }
            }

            JavaFileAnalyzer javaFile = new JavaFileAnalyzer();
            return javaFile.analyzeIfSuitable(source, null, Charset.defaultCharset()).get();
        } catch (IOException e) {
            throw new IllegalStateException("failed analyzing", e);
        }
    }
    
    private static Pckg pckg(String name) {
        return new Pckg(name);
    }
}
