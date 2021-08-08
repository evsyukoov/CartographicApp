package controller;

import bot.GeodeticBot;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;


@RestController
public class EntryPointController {

    GeodeticBot geodeticBot;

    public EntryPointController() {
        geodeticBot = new GeodeticBot();
    }
}

//    @RequestMapping(value = "/", method = RequestMethod.POST)
//    public BotApiMethod API(@RequestBody Update update) {
//        return geodeticBot.onWebhookUpdateReceived(update);
//    }}
