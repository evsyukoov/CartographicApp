package ru.evsyukoov.transform.dto;

import java.util.ArrayList;
import java.util.List;

public class AutocadFileInfo extends FileInfo {

    private List<Pline> plines = new ArrayList<>();

    public List<Pline> getPolylines() {
        return plines;
    }
}
