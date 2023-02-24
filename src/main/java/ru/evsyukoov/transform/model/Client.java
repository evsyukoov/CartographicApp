package ru.evsyukoov.transform.model;

import ru.evsyukoov.transform.bot.enums.InputCoordinatesType;
import ru.evsyukoov.transform.enums.FileFormat;
import ru.evsyukoov.transform.enums.TransformationType;
import ru.evsyukoov.transform.stateMachine.State;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "nickname")
    private String nickName;

    @Column(name = "state")
    @Enumerated
    private State state;

    @Column(name = "in_file_format")
    @Enumerated
    private FileFormat format;

    @Column(name = "transformation_type")
    @Enumerated
    private TransformationType transformationType;

    @Column(name = "count")
    private int count;

    @Column(name = "last_response")
    private String response;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public FileFormat getFormat() {
        return format;
    }

    public void setFormat(FileFormat format) {
        this.format = format;
    }

    public TransformationType getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(TransformationType transformationType) {
        this.transformationType = transformationType;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", state=" + state +
                ", count=" + count +
                '}';
    }
}
