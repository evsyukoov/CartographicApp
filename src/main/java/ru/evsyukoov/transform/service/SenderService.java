package ru.evsyukoov.transform.service;

import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;

public interface SenderService {

    void sendInlineAnswer(AnswerInlineQuery query, long clientId);
}
