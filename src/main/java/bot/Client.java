package bot;


import convert.Point;

import java.util.ArrayList;
import java.util.LinkedList;

public class Client {
    private LinkedList<Point> pointsFromFile;

    private  String uploadPath;

    private String choosedSK;

    private String choosedType;

    private String choosedZone;

    private long id;

    private int         state;

    private Boolean     isClientReady;

    public Client(long id) {
        this.id = id;
        uploadPath = "./resources/uploaded/" + "file_" + id;
        isClientReady = false;
    }

    public void setPointsFromFile(LinkedList<Point> pointsFromFile) {
        this.pointsFromFile = pointsFromFile;
    }

    public void setChoosedSK(String choosedSK) {
        this.choosedSK = choosedSK;
    }

    public void setChoosedType(String choosedType) {
        this.choosedType = choosedType;
    }

    public void setGetChoosedZone(String choosedZone) {
        this.choosedZone = choosedZone;
    }

    public String getChoosedSK() {
        return choosedSK;
    }

    public String getChoosedType() {
        return choosedType;
    }

    public String getChoosedZone() {
        return choosedZone;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public long getId() {
        return id;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public LinkedList<Point> getPointsFromFile() {
        return pointsFromFile;
    }

    public  void    clean()
    {
        choosedSK = null;
        choosedType = null;
        choosedZone = null;
        pointsFromFile = null;
    }

    public void setClientReady(Boolean clientReady) {
        isClientReady = clientReady;
    }

    public Boolean getClientReady() {
        return isClientReady;
    }
}
