package org.schmidrules;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.schmidrules.check.violation.Violation;

public class SchmidRulesJavaITest {
    @Test
    public void shouldNotViolateItself() {
        SchmidRules rules = new SchmidRules(new File("src/main/config/schmid-rules.xml"), Arrays.asList(new File(".")));

        Collection<Violation> violations = rules.check();

        assertEquals(emptyList(), violations);
    }
}
