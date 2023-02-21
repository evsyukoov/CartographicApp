package ru.evsyukoov.transform.enums;

import java.util.Arrays;

public enum TransformationType {
    MSK_TO_WGS("MSK -> WGS-84"),
    WGS_TO_MSK("WGS-84 -> MSK"),
    MSK_TO_MSK("MSK -> MSK"),
    WGS_TO_WGS("WGS-84 -> WGS-84");

    private final String description;

    public String getDescription() {
        return description;
    }

    TransformationType(String description) {
        this.description = description;
    }

    public static TransformationType getTypeByDescription(String description) {
        return Arrays.stream(TransformationType.values())
                .filter(type -> type.getDescription().equals(description))
                .findFirst()
                .orElse(null);
    }
}
