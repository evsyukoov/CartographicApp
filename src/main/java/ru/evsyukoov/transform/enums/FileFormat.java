package ru.evsyukoov.transform.enums;

public enum FileFormat {
    CSV_RECTANGULAR("CSV(плоские)"),
    CSV("CSV(WGS-84)"),
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
