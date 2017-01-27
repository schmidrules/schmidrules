package org.schmidrules.dependency;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public abstract class Analyzer {

    protected final Collection<String> suitableExtensions;

    public Analyzer(Collection<String> suitableExtensions) {
        this.suitableExtensions = Collections.unmodifiableCollection(new ArrayList<>(suitableExtensions));
    }

    protected boolean isSuitableFor(File file) {
        String name = file.getName().toLowerCase();

        return suitableExtensions.stream() //
                .filter(ext -> name.endsWith('.' + ext)) //
                .findAny() //
                .isPresent();
    }

    protected abstract AnalyzedFile analyze(File file, File sourceDir, Charset charset) throws IOException;

    /**
     * Returns an analyzed file if the input is suitable.
     */
    public Optional<AnalyzedFile> analyzeIfSuitable(File file, File sourceDir, Charset charset) {
        if (!isSuitableFor(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(analyze(file, sourceDir, charset));
        } catch (Exception e) {
            throw new IllegalArgumentException("could not analyze " + file, e);
        }
    }
}
