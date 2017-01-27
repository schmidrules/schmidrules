package org.schmidrules.check;

import org.schmidrules.configuration.dto.ComponentReferenceDto;
import org.schmidrules.configuration.dto.PackageReferenceDto;
import org.schmidrules.dependency.ComponentReference;
import org.schmidrules.dependency.Pckg;

public class Converters {

    public static Pckg fromDto(PackageReferenceDto dto) {
        return new Pckg(dto.getName(), dto.getLocation());
    }

    public static ComponentReference fromDto(ComponentReferenceDto dto) {
        return new ComponentReference(dto.getName(), dto.getLocation());
    }
}
