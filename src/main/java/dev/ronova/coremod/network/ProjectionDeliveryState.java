package dev.ronova.coremod.network;

/** Bounded connection-local delivery evidence; no server Truth or renderer completion is inferred. */
public final class ProjectionDeliveryState {
    public enum Stage { IDLE, QUEUED, WAIT_ACK, CONFIRMED, ROOT_WAIT, CAPACITY, QUEUE_BUDGET, HEAP_BUDGET, TRANSPORT, ACK_TIMEOUT, CLOSED }
    private Stage stage = Stage.IDLE;
    private long failures, retryAt;
    private int consecutive;
    public Stage stage() { return stage; }
    public long retryAt() { return retryAt; }
    public long failures() { return failures; }
    public void queued() { stage = Stage.QUEUED; retryAt = 0; }
    public void sent() { stage = Stage.WAIT_ACK; }
    public void confirmed() { stage = Stage.CONFIRMED; consecutive = 0; retryAt = 0; }
    public void close() { stage = Stage.CLOSED; retryAt = Long.MAX_VALUE; }
    public void failed(Stage reason, long now) {
        if (reason == Stage.IDLE || reason == Stage.QUEUED || reason == Stage.WAIT_ACK || reason == Stage.CONFIRMED || reason == Stage.CLOSED)
            throw new IllegalArgumentException("not a delivery failure");
        stage = reason; if (failures != Long.MAX_VALUE) failures++;
        consecutive = Math.min(5, consecutive + 1);
        long delay = Math.min(200, 20L << (consecutive - 1));
        retryAt = now > Long.MAX_VALUE - delay ? Long.MAX_VALUE : now + delay;
    }
    public String summary(long now, boolean refreshPending) {
        String reason = switch (stage) {
            case IDLE -> "尚未发布"; case QUEUED -> "分片发送中"; case WAIT_ACK -> "等待完整图像回执";
            case CONFIRMED -> "投影数据已确认"; case ROOT_WAIT -> "等待 Root 持久化";
            case CAPACITY -> "图像超过容量上限"; case QUEUE_BUDGET -> "发送队列预算不足";
            case HEAP_BUDGET -> "运行时堆预算不足"; case TRANSPORT -> "发送失败";
            case ACK_TIMEOUT -> "图像回执超时"; case CLOSED -> "连接已失效";
        };
        String waiting = retryAt > now && stage != Stage.CLOSED ? "，" + (retryAt - now) + " tick 后重试" : "";
        return "本连接投影：" + reason + waiting + (refreshPending ? "；有更新待发布" : "") + (failures > 0 ? "；累计受阻 " + failures : "");
    }
}
