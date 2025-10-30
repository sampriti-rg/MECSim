package applications.remec;

public class UMI {
    private  int dcId;
    private  long userId;
    private  long messageId;

    public UMI(int dcId, long userId, long messageId) {
        this.dcId = dcId;
        this.userId = userId;
        this.messageId = messageId;
    }

    public int getDcId() {
        return dcId;
    }

    public long getUserId() {
        return userId;
    }

    public long getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return String.format("DC ID: %d, User ID: %d, Message ID: %d", dcId, userId, messageId);
    }
}
