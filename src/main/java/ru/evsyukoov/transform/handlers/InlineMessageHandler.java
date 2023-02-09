package ru.evsyukoov.transform.handlers;

import com.ibm.icu.text.Transliterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import ru.evsyukoov.transform.constants.Messages;
import ru.evsyukoov.transform.model.CoordinateSystem;
import ru.evsyukoov.transform.repository.CoordinateSystemRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InlineMessageHandler {

    private final Transliterator latinToCyrillic;

    private final Transliterator cyrillicToLatin;

    private final CoordinateSystemRepository coordinateSystemRepository;

    @Value("${bot.max-result}")
    private Integer maxResult;

    @Autowired
    public InlineMessageHandler(Transliterator latinToCyrillic,
                                Transliterator cyrillicToLatin,
                                CoordinateSystemRepository coordinateSystemRepository) {
        this.latinToCyrillic = latinToCyrillic;
        this.cyrillicToLatin = cyrillicToLatin;
        this.coordinateSystemRepository = coordinateSystemRepository;
    }

    public AnswerInlineQuery getInlineAnswer(Update update) {
        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(update.getInlineQuery().getId());
        if (update.getInlineQuery().getQuery().length() == 0) {
            answerInlineQuery.setResults(preparePrompt(update));
        } else {
            List<InlineQueryResult> result = prepareQueryAnswer(update.getInlineQuery().getQuery());
            answerInlineQuery.setResults(result);
        }
        return answerInlineQuery;
    }

    private List<InlineQueryResult> preparePrompt(Update update) {
        InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
        InputTextMessageContent itmc = new InputTextMessageContent();
        inlineQueryResultArticle.setId(update.getInlineQuery().getId());
        itmc.setMessageText(Messages.INLINE_QUERY_PROMPT);
        inlineQueryResultArticle.setInputMessageContent(itmc);
        inlineQueryResultArticle.setTitle(Messages.INLINE_QUERY_PROMPT);
        return Collections.singletonList(inlineQueryResultArticle);
    }

    private List<InlineQueryResult> prepareQueryAnswer(String receive) {
        List<InlineQueryResult> result = new ArrayList<>();
        List<String> projects = filterStrings(receive);
        if (CollectionUtils.isEmpty(projects)) {
            projects = filterStrings(transliterateString(receive));
        }
        int i = 0;
        for (String project : projects) {
            InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
            inlineQueryResultArticle.setId(String.valueOf(++i));
            InputTextMessageContent itmc = new InputTextMessageContent();
            itmc.setMessageText(project);
            inlineQueryResultArticle.setInputMessageContent(itmc);
            inlineQueryResultArticle.setTitle(project);
            result.add(inlineQueryResultArticle);
        }
        return result;
    }

    private List<String> filterStrings(String text) {
        return coordinateSystemRepository
                .findCoordinateSystemByDescriptionLikeIgnoreCase("%" + text + "%")
                .stream()
                .limit(maxResult)
                .map(CoordinateSystem::getDescription)
                .collect(Collectors.toList());
    }

    private String transliterateString(String text) {
        if (isCyrillic(text)) {
            return cyrillicToLatin.transliterate(text);
        } else {
            return latinToCyrillic.transliterate(text);
        }
    }

    private boolean isCyrillic(String text) {
        return text.chars()
                .mapToObj(Character.UnicodeBlock::of)
                .anyMatch(Character.UnicodeBlock.CYRILLIC::equals);
    }
}
