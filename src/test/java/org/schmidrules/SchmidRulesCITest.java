package org.schmidrules;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.schmidrules.SchmidRules;
import org.schmidrules.check.violation.Violation;

public class SchmidRulesCITest {
    @Test
    public void shouldAnalyzeSourceFolder() {
        SchmidRules rules =
                new SchmidRules(new File("src/test/resources/c-itest-arch.xml"), Arrays.asList(new File(".")));

        Collection<Violation> violations = rules.check();

        assertEquals(emptyList(), violations);
    }
}
