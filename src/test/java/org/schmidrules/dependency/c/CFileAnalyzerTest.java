package org.schmidrules.dependency.c;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.schmidrules.dependency.AnalyzedFile;
import org.schmidrules.dependency.Pckg;

public class CFileAnalyzerTest {

    private final CFileAnalyzer analyzer = new CFileAnalyzer();

    @Test
    public void shouldFindDependencies() throws Exception {
        AnalyzedFile file = analyzer.analyze(file("srcfolder/transact/billing/billing_record.c"), file("srcfolder"), Charset.defaultCharset());

        Collection<Pckg> dependencies = file.getDependencies().stream()//
        		.map(dep -> dep.pckg)
        		.collect(Collectors.toList());
        
        assertThat(dependencies, hasItems(pckg("transact/billing"), pckg("dgw")));
    }

    @Test
    public void shouldFindOwnPackage() throws Exception {
        AnalyzedFile file = analyzer.analyze(file("srcfolder/transact/billing/billing_record.c"), file("srcfolder"), Charset.defaultCharset());

        assertThat(file.getPackage(), equalTo(pckg("transact/billing")));
    }

    private File file(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(fileName).getFile());
    }
    
    private static Pckg pckg(String name) {
        return new Pckg(name);
    }
}
