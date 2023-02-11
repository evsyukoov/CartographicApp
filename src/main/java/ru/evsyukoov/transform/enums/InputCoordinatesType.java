package ru.evsyukoov.transform.enums;

public enum InputCoordinatesType {

    WGS_84("World geocentric coordinate system"),
    MSK("Russian local rectangular coordinate system");

    private final String description;

    InputCoordinatesType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
