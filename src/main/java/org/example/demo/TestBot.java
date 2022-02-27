package org.example.demo;

import lombok.SneakyThrows;
import org.example.demo.entity.Currency;
import org.example.demo.service.CurrencyConversionService;
import org.example.demo.service.CurrencyModeService;
import org.example.demo.service.impl.HashMapCurrencyModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestBot.class);
    private final CurrencyModeService modeService =  CurrencyModeService.getInstance();
    private final CurrencyConversionService conversionService = CurrencyConversionService.getInstance();
    protected TestBot(DefaultBotOptions options) {
        super(options);
    }

    @Override
    public String getBotUsername() {
        LOGGER.info("Return username ");
        return "@Scyberbot";
    }

    @Override
    public String getBotToken() {
        LOGGER.info("Token Requested ");
        return "5181936945:AAHwOVaM3DBnXbDh9PQSa21cqVj7mnhvOcQ";
    }

    @SneakyThrows
    public static void main(String []args){
        TestBot bot = new TestBot(new DefaultBotOptions());
        LOGGER.info("Register api starting");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(bot);
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            handleCallBack(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            handleMessage(update.getMessage());
        }

//        Message msg = update.getMessage();
//            if(update.hasMessage()) {
//                LOGGER.info("Message updates {} ", msg);
//                msg = update.getMessage();
//            }
//            if(msg.hasText()) {
//                LOGGER.info("Message text {}", msg.getText());
//                execute(SendMessage.builder()
//                        .chatId(msg.getChatId().toString())
//                        .text("You send :" + msg.getText())
//                        .build());
//            }

    }
    @SneakyThrows
    private void handleCallBack(CallbackQuery callbackQuery) {
            Message message = callbackQuery.getMessage();
            String[] params = callbackQuery.getData().split(":");
            String action = params[0];
            Currency newCurrency  = Currency.valueOf(params[1]);

            switch (action){
                case "ORIGINAL":
                    modeService.setOriginalCurrency(message.getChatId(),newCurrency);
                    break;
                case "TARGET":
                    modeService.setTargetCurrency(message.getChatId(),newCurrency);
                    break;
            }
        List<List<InlineKeyboardButton>> bottoms = new ArrayList<>();
        Currency originalCurrency = modeService.getOriginalCurrency(message.getChatId());
        Currency targetCurrency = modeService.getTargetCurrency(message.getChatId());
        for(Currency currency :Currency.values()){
            bottoms.add(Arrays.asList(
                    InlineKeyboardButton.builder()
                            .text(getCurrencyButton(originalCurrency,currency))
                            .callbackData("ORIGINAL:" + currency).build(),
                    InlineKeyboardButton.builder()
                            .text(getCurrencyButton(targetCurrency,currency))
                            .callbackData("TARGET:" + currency).build()));
        }
            execute(EditMessageReplyMarkup.builder()
                    .chatId(message.getChatId().toString())
                    .messageId(message.getMessageId())
                    .replyMarkup(InlineKeyboardMarkup.builder().keyboard(bottoms).build())
                    .build());

    }

    @SneakyThrows
    private void handleMessage(Message message) {

        if(message.hasText() & message.hasEntities()){
            Optional<MessageEntity> commandEntity =
                    message.getEntities().stream().filter(e -> "bot_command".equals(e.getType())).findFirst();
            if(commandEntity.isPresent()){
               String comandd =  message.getText().substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                switch (comandd){
                    case "/set_currency":
                        List<List<InlineKeyboardButton>> bottoms = new ArrayList<>();
                        Currency originalCurrency = modeService.getOriginalCurrency(message.getChatId());
                        Currency targetCurrency = modeService.getTargetCurrency(message.getChatId());
                        for(Currency currency :Currency.values()){
                            bottoms.add(Arrays.asList(
                                    InlineKeyboardButton.builder()
                                                        .text(getCurrencyButton(originalCurrency,currency))
                                                        .callbackData("ORIGINAL: " + currency).build(),
                                    InlineKeyboardButton.builder()
                                                        .text(getCurrencyButton(targetCurrency,currency))
                                                        .callbackData("TARGET:" + currency).build()));
                        }
                        execute(SendMessage.builder()
                                .chatId(message.getChatId().toString())
                                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(bottoms).build())
                                .text("Please choose original and Target Currency").build());
                        return;
                    default:
                        execute(SendMessage.builder().chatId(message.getChatId().toString()).text("Uknown command").build());
                        return;
                }
            }

        }

        if(message.hasText()){
            String messageText = message.getText();
            Optional<Double> value = parceDouble(messageText);
            Currency orginalCurrency = modeService.getOriginalCurrency(message.getChatId());
            Currency targetCurrency = modeService.getTargetCurrency(message.getChatId());
            double ratio = conversionService.getConversionRatio(orginalCurrency,targetCurrency);
            System.out.println("ration " + ratio);
            if (value.isPresent()){
                execute(SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text(String.format("%4.2f %s is %4.2f %s", value.get(), orginalCurrency, (value.get() * ratio), targetCurrency )).build());
                return;
            }
        }

    }

    private Optional<Double> parceDouble(String messageText) {
        try {
            return  Optional.of(Double.parseDouble(messageText));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String getCurrencyButton(Currency saved, Currency current){
        return saved == current ? current + " ✅️" : current.name();
    }
}
