package bot;


import bot.enums.InputCoordinatesType;
import bot.enums.OutputFileType;
import bot.enums.TransType;
import convert.InfoReader;
import dao.SelectDAO;

import java.io.File;
import java.util.ArrayList;

public class Client {

    //номер где будет храниться предыдущий стейт клиента в случае ухода с основной ветки
    private int prevState;

    TransType transType;


    public TransType getTransType() {
        return transType;
    }

    OutputFileType outputFileType;

    InfoReader infoReader;

    String transformationParametrs;

    public String getTransformationParametrs() {
        return transformationParametrs;
    }

    public void setTransformationParametrs(String transformationParametrs) {
        this.transformationParametrs = transformationParametrs;
    }

    public OutputFileType getOutputFileType() {
        return outputFileType;
    }

    public void setOutputFileType(OutputFileType outputFileType) {
        this.outputFileType = outputFileType;
    }

    public void setInfoReader(InfoReader infoReader) {
        this.infoReader = infoReader;
    }

    public InfoReader getInfoReader() {
        return infoReader;
    }

    public int getPrevState() {
        return prevState;
    }

    public void setPrevState(int prevState) {
        this.prevState = prevState;
    }

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


    public  void    clean()
    {
        choosedSK = null;
        choosedType = null;
        choosedZone = null;
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

    public void analizeTransformationType(String receive) {
        if (infoReader.getInputCoordinatesType() == InputCoordinatesType.WGS)
        {
            if (receive.equals("GPX"))
            {
                transType = TransType.WGS_TO_WGS;
                outputFileType = OutputFileType.GPX;
            }
            else if (receive.equals("KML"))
            {
                transType = TransType.WGS_TO_WGS;
                outputFileType = OutputFileType.KML;
            }
            else if (receive.equals("CSV(плоская)"))
            {
                transType = TransType.WGS_TO_MSK;
                outputFileType = OutputFileType.CSV;
            }
        }
        else
        {
            if (receive.equals("GPX")) {
                transType = TransType.MSK_TO_WGS;
                outputFileType = OutputFileType.GPX;
            }
            else if (receive.equals("KML")) {
                transType = TransType.MSK_TO_WGS;
                outputFileType = OutputFileType.KML;
            }
            else if (receive.equals("CSV(плоская)")){
                transType = TransType.MSK_TO_MSK;
                outputFileType = OutputFileType.CSV;
            }
        }
    }
}
