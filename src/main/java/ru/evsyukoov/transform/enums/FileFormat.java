package ru.evsyukoov.transform.enums;

public enum FileFormat {

    CSV("CSV"),
    DXF("DXF"),
    TXT("TXT"),
    GPX("GPX"),
    KML("KML"),
    KMZ("KMZ");

    private final String description;

    public String getDescription() {
        return description;
    }

    FileFormat(String description) {
        this.description = description;
    }
}
