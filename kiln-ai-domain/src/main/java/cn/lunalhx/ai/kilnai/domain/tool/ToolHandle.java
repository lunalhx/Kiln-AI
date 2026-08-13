package cn.lunalhx.ai.kilnai.domain.tool;

public record ToolHandle(String id, int version, String inputSchema) {
    public String qualifiedId() {
        return id + "@" + version;
    }
}
