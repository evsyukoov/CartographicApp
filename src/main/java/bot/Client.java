package bot;

import bot.enums.OutputFileType;
import bot.enums.TransType;
import convert.InfoReader;

public class Client {

    //номер где будет храниться предыдущий стейт клиента в случае ухода с основной ветки
    private int prevState;

    TransType transType;

    OutputFileType outputFileType;

    InfoReader infoReader;

    String srcSystem;

    String tgtSystem;

    private int state;

    private Boolean isClientReady;

    private String extension;

    private String errorMSG;

    private final String uploadPath;

    private final String savePath;

    private final long id;


    public void setTransType(TransType transType) {
        this.transType = transType;
    }

    public String getSrcSystem() {
        return srcSystem;
    }

    public void setSrcSystem(String srcSystem) {
        this.srcSystem = srcSystem;
    }

    public String getTgtSystem() {
        return tgtSystem;
    }

    public void setTgtSystem(String tgtSystem) {
        this.tgtSystem = tgtSystem;
    }

    public TransType getTransType() {
        return transType;
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

    public String getExtension() {
        return extension;
    }

    public String getErrorMSG() {
        return errorMSG;
    }

    public void setErrorMSG(String errorMSG) {
        this.errorMSG = errorMSG;
    }

    public String getSavePath() {
        return savePath;
    }

    public Client(long id) {
        this.id = id;
        uploadPath = "./src/main/resources/uploaded/" + "file_" + id;
        isClientReady = false;
        savePath = "./src/main/resources/send/" + id;
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

    public void setClientReady(Boolean clientReady) {
        isClientReady = clientReady;
    }

    public Boolean getClientReady() {
        return isClientReady;
    }

}
