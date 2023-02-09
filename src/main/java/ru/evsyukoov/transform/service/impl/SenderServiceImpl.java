package ru.evsyukoov.transform.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import ru.evsyukoov.transform.service.SenderService;

@Service
@Slf4j
public class SenderServiceImpl implements SenderService {

    @Override
    public void sendInlineAnswer(AnswerInlineQuery query, long clientId) {
        //
    }
}
