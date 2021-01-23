package bot;

public enum BotState {
    //state = 0
    CHOOSE_TYPE {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    },

    CHOOSE_SK {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    },

    CHOOSE_ZONE {
        @Override
        public void writeToClient(BotContext botContext) {

        }

        @Override
        public void readFromClient(BotContext botContext) {

        }

        @Override
        public BotState next(BotContext botContext) {
            return null;
        }
    };



    public abstract void writeToClient(BotContext botContext);

    public abstract void readFromClient(BotContext botContext);

    public abstract BotState next(BotContext botContext);
}
