package org.schmidrules.dependency;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("meaningless tests, since there is no validation in the constructor anymore")
public class PckgTest {

    @Test
    public void shouldAcceptValidPackageName() {
        assertPackageNameValid("java.package");
    }

    @Test
    public void shouldAcceptSingleCharacterPackageName() {
        assertPackageNameValid("a.a");
    }

    /**
     * Although it is forbidden by the coding style guide, it is technically allowed, so we look
     * away.
     */
    @Test
    public void shouldAcceptUpperCaseLettersMidPart() {
        assertPackageNameValid("java.upperCase");
    }

    /**
     * This is technically allowed too, but we can't allow it because this would break class name
     * detection.
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptPartStartingWithUpperCaseLetter() {
        assertPackageNameValid("java.Uppercase");
    }

    @SuppressWarnings("unused")
    private static void assertPackageNameValid(String name) {
        new Pckg(name);
    }
}
