package ru.evsyukoov.transform.controller;

import ru.evsyukoov.transform.bot.GeodeticBot;
import org.springframework.web.bind.annotation.*;


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
