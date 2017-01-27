package org.schmidrules.dependency.c;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;
import org.schmidrules.dependency.Pckg;

public class RegexTest {

    private static Pckg PACKAGE = new Pckg("transact/message");

    private static String INCLUDE_LINE = "#incLUDE \"" + PACKAGE.toString().toUpperCase() + "/billingUtil.H\"";

    @Test
    public void shouldMatchInclude() {
        Optional<Pckg> pckg = Regex.matchPackageFromIncludeLine(INCLUDE_LINE);

        assertEquals(PACKAGE, pckg.get());
    }

    @Test
    public void shouldNotMatchGarbage() {
        Optional<Pckg> pckg = Regex.matchPackageFromIncludeLine("garbage");

        assertFalse(pckg.isPresent());
    }

    @Test
    public void shouldRecognizeDefaultPackage() {
        Optional<Pckg> pckg = Regex.matchPackageFromIncludeLine("#include <stdio.h>");

        assertEquals(Pckg.DEFAULT, pckg.get());
    }
}
