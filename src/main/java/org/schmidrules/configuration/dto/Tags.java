package org.schmidrules.configuration.dto;

public class Tags {

    public static final String PACKAGE_REFERENCE_TAG = "package";
    public static final String COMPONENT_REFERENCE_TAG = "componentDependency";

    /**
     * Note: May be too short in case of extra white spaces.
     */
    public static final int tagWrapperLength(String tag) {
        return tag.length() * 2 + 5;
    }
}
