package org.schmidrules.dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AnalyzedFile {
    private final String name;
    private final Pckg pckg;
    private final Set<Dependency> dependencies;

    public AnalyzedFile(String name, Pckg pckg, Set<Dependency> dependencies) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(pckg);

        this.name = name.intern();
        this.pckg = pckg;
        this.dependencies = Collections.unmodifiableSet(dependencies);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Pckg getPackage() {
        return pckg;
    }

    public Collection<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return name;
    }

    public String dump() {
        return "AnalyzedFile [name=" + name + ", pckg=" + pckg + ", dependencies=" + dependencies + "]";
    }

    public static class Builder {
        private String name;
        private Pckg pckg;
        private final Set<Dependency> dependencies = new HashSet<>();

        public Builder() {
            this(null);
        }

        public Builder(String name) {
            this(name, null);
        }

        public Builder(String name, Pckg pckg) {
            this.name = name;
            this.pckg = pckg;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Pckg getPckg() {
            return pckg;
        }

        public void setPckg(Pckg pckg) {
            this.pckg = pckg;
        }

        public void addDependency(Dependency dependency) {
            dependencies.add(dependency);
        }

        public AnalyzedFile build() {
            return new AnalyzedFile(name, pckg, dependencies);
        }
    }
}
