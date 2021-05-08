package webhook;

import bot.GeodeticBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;


@RestController
public class EntryPointController {

    GeodeticBot geodeticBot;

    public EntryPointController(GeodeticBot geodeticBot) {
        this.geodeticBot = geodeticBot;
    }

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public Object Test()
    {
        return "Server working on 8443\n";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public BotApiMethod API(@RequestBody Update update) {
        return geodeticBot.onWebhookUpdateReceived(update);
    }}
