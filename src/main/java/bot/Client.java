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

    String targetType;

    String targetSk;

    String targetZone;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetSk() {
        return targetSk;
    }

    public void setTargetSk(String targetSk) {
        this.targetSk = targetSk;
    }

    public String getTargetZone() {
        return targetZone;
    }

    public void setTargetZone(String targetZone) {
        this.targetZone = targetZone;
    }

    public TransType getTransType() {
        return transType;
    }

    OutputFileType outputFileType;

    InfoReader infoReader;

    String transformationParametrs;

    //параметры для перевода типа плоские в плоские

    String secondTransformationParamters;

    public String getSecondTransformationParamters() {
        return secondTransformationParamters;
    }

    public void setSecondTransformationParamters(String secondTransformationParamters) {
        this.secondTransformationParamters = secondTransformationParamters;
    }

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

    private File file;

    private long id;

    public File getFile() {
        return file;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setFile(File file) {
        this.file = file;
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

    public void setChoosedZone(String choosedZone) {
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
            else if (receive.equals("CSV(плоские)"))
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
            else if (receive.equals("CSV(WGS-84)")){
                transType = TransType.MSK_TO_WGS;
                outputFileType = OutputFileType.CSV;
            }
            else if (receive.equals("CSV(плоские)"))
            {
                transType = TransType.MSK_TO_MSK;
                outputFileType = OutputFileType.CSV;
            }
        }
    }
}
