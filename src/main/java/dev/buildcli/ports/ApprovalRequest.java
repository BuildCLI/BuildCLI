package dev.buildcli.ports;

/**
 * @param grantKey  what "always allow" would cover (null: this kind of request is always asked, e.g. a commit)
 * @param grantLabel that, in words for the user: "let ana edit files in this chat"
 */
public record ApprovalRequest(String agent, String kind, String summary, String detail, String grantKey, String grantLabel) {
    public ApprovalRequest(String agent, String kind, String summary, String detail) {
        this(agent, kind, summary, detail, null, null);
    }
}
