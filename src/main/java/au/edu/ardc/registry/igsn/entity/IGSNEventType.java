package au.edu.ardc.registry.igsn.entity;

public enum IGSNEventType {
    MINT("igsn-mint"),
    UPDATE("igsn-update"),
    RESERVE("igsn-reserve"),
    TRANSFER("igsn-owner-transfer");

    private final String action;

    IGSNEventType(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
