package bot;


import convert.Point;
import dao.SelectDAO;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class Client {

    //номер где будет храниться предыдущий стейт клиента в случае ухода с основной ветки
    private int prevState;

    public int getPrevState() {
        return prevState;
    }

    public void setPrevState(int prevState) {
        this.prevState = prevState;
    }

    private LinkedList<Point> pointsFromFile;

    private  String uploadPath;

    private String savePath;

    public String getExtension() {
        return extension;
    }

    private String extension;

    private String errorMSG;

    private SelectDAO sd;

    public void setSd(SelectDAO sd) {
        this.sd = sd;
    }


    public SelectDAO getSd() {
        return sd;
    }

    public String getErrorMSG() {
        return errorMSG;
    }

    public void setErrorMSG(String errorMSG) {
        this.errorMSG = errorMSG;
    }

    private String choosedSK;

    private String choosedType;

    private String choosedZone;

    private ArrayList<File> files;

    public int getTransformType() {
        return transformType;
    }

    public void setTransformType(int transformType) {
        this.transformType = transformType;
    }

    private int transformType;

    private long id;

    public ArrayList<File> getFiles() {
        return files;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }

    private int         state;

    private Boolean     isClientReady;

    public Client(long id) {
        this.id = id;
        uploadPath = "./src/main/resources/uploaded/" + "file_" + id;
        isClientReady = false;
        savePath = "./src/main/resources/send/" + id;
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

    public String getChoosedZone() {
        return choosedZone;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setExtension(String extension) {
        this.extension = extension;
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

    public String getChoosedType() {
        return choosedType;
    }
}
