package org.schmidrules.configuration.dto;

import static org.schmidrules.configuration.dto.Tags.PACKAGE_REFERENCE_TAG;
import static org.schmidrules.configuration.dto.Tags.tagWrapperLength;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class PackageReferenceAdapter extends XmlAdapter<String, PackageReferenceDto> {

    private static final int TAG_CHARS = tagWrapperLength(PACKAGE_REFERENCE_TAG);

    @Override
    public PackageReferenceDto unmarshal(String p) {
        int textLength = p.length() + TAG_CHARS;
        return new PackageReferenceDto(p.trim(), textLength);
    }

    @Override
    public String marshal(PackageReferenceDto javaPackage) {
        return javaPackage.getName();
    }
}
