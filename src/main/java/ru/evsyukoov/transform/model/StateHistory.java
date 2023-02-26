package ru.evsyukoov.transform.model;

import ru.evsyukoov.transform.stateMachine.State;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "state_history")
public class StateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long stateId;

    @ManyToOne(fetch = FetchType.EAGER)
    private Client client;

    @Column(name = "state")
    private State state;

    @Column(name = "order_num")
    private int orderNum;

    @Column(name = "response")
    private String response;

    @Column(name = "client_choice")
    private String clientChoice;

    public long getStateId() {
        return stateId;
    }

    public void setStateId(long stateId) {
        this.stateId = stateId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getClientChoice() {
        return clientChoice;
    }

    public void setClientChoice(String clientChoice) {
        this.clientChoice = clientChoice;
    }
}
