package bot;

import dao.InlineDataAccessObject;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InlineMod {
    Update update;
    GeodeticBot bot;

    private static final Logger logger = Logger.getLogger(InlineMod.class.getName());


    public InlineMod(Update update, GeodeticBot bot) {
        this.update = update;
        this.bot = bot;
    }

    //для WebHook вернуть AnswerInlineQuery == BotApiMethod
    public void answerInline() throws SQLException {
        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(update.getInlineQuery().getId());
        if (update.getInlineQuery().getQuery().length() < 3) {
            answerInlineQuery.setResults(prepareSimpleAnswer());
        } else {
            answerInlineQuery.setResults(prepareQueryAnswer(update.getInlineQuery().getQuery()));
        }
        try {
            bot.execute(answerInlineQuery);
        } catch (TelegramApiException e) {
            logger.log(Level.SEVERE, "Problem with sending inline answer");
        }
    }

    private InlineQueryResult prepareSimpleAnswer()
    {
        InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
        InputTextMessageContent itmc = new InputTextMessageContent();
        inlineQueryResultArticle.setId(update.getInlineQuery().getId());
        itmc.setMessageText("Начните вводить свою систему координат(хотя бы 3 буквы)");
        inlineQueryResultArticle.setInputMessageContent(itmc);
        inlineQueryResultArticle.setTitle("Начните вводить свою систему координат(хотя бы 3 буквы)");
        return inlineQueryResultArticle;
    }

    private List<InlineQueryResult> prepareQueryAnswer(String receive) throws SQLException {
        List<InlineQueryResult> result = new ArrayList<>();
        int i = 0;
        for (String description : InlineDataAccessObject.findParams(receive)) {
            InlineQueryResultArticle inlineQueryResultArticle = new InlineQueryResultArticle();
            inlineQueryResultArticle.setId(String.valueOf(++i));
            InputTextMessageContent itmc = new InputTextMessageContent();
            itmc.setMessageText(description);
            inlineQueryResultArticle.setInputMessageContent(itmc);
            inlineQueryResultArticle.setTitle(description);
            result.add(inlineQueryResultArticle);
        }
        return result;
    }
}
