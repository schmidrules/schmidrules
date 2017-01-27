package org.schmidrules.configuration.dto;

import static org.schmidrules.configuration.dto.Tags.COMPONENT_REFERENCE_TAG;
import static org.schmidrules.configuration.dto.Tags.tagWrapperLength;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ComponentReferenceAdapter extends XmlAdapter<String, ComponentReferenceDto> {

    private static final int TAG_CHARS = tagWrapperLength(COMPONENT_REFERENCE_TAG);

    @Override
    public ComponentReferenceDto unmarshal(String p) {
        int textLength = p.length() + TAG_CHARS;
        return new ComponentReferenceDto(p.trim(), textLength);
    }

    @Override
    public String marshal(ComponentReferenceDto javaPackage) {
        return javaPackage.getName();
    }
}
