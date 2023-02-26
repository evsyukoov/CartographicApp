package ru.evsyukoov.transform.enums;

public enum FileFormat {

    CSV("CSV"),
    DXF("DXF"),
    TXT("TXT"),
    GPX("GPX"),
    KML("KML"),
    KMZ("KMZ"),
    CONSOLE_IN("TEXT");

    private final String description;

    public String getDescription() {
        return description;
    }

    FileFormat(String description) {
        this.description = description;
    }
}
