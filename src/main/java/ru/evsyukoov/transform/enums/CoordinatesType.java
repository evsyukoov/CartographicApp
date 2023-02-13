package ru.evsyukoov.transform.enums;

public enum CoordinatesType {

    WGS_84("World geocentric coordinate system"),
    MSK("Russian local rectangular coordinate system");

    private final String description;

    CoordinatesType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
