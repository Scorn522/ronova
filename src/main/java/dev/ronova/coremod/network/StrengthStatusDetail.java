package dev.ronova.coremod.network;

import java.util.Map;

/** Display-only domains share the existing 256-character detail field, never execution authority. */
public final class StrengthStatusDetail {
    private StrengthStatusDetail() {}
    public static String compose(String completion, String source, String delivery, String feedback, Map<String,Long> tasks) {
        String work="记录：结果待记"+pendingCount(tasks.get("mutation_receipts_pending"))
            +"/待收集"+pendingCount(tasks.get("record_results_uncollected"))
            +"/在途"+pendingCount(tasks.get("record_tasks_pending"))
            +"/阻塞"+pendingCount(tasks.get("record_tasks_monitor_blocked"))
            +"/取消待退"+pendingCount(tasks.get("record_tasks_cancel_pending"))
            +"/过期待退"+pendingCount(tasks.get("record_tasks_expired_pending"));
        return clip(completion,54)+" · "+clip(source,48)+" · "+clip(work,58)
            +" · "+clip(delivery,40)+" · "+clip(feedback,43);
    }
    public static String recovery(long records,long staticCells,long loadedCells) {
        return recovery(records, 0, staticCells, loadedCells);
    }
    public static String recovery(long staticRecords, long loadedRecords, long staticCells, long loadedCells) {
        // Keep the scope and unresolved-chain qualification ahead of any clipped counters.
        return "本世界恢复：整链未证；记录"+count(sum(staticRecords, loadedRecords))+"；快照"+count(sum(staticCells, loadedCells));
    }
    private static Long sum(long first, long second) {
        return first < 0 || second < 0 ? null : first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
    }
    private static String count(Long value) {
        if(value==null||value<0)return "未知";
        if(value<10_000)return value.toString();
        if(value<100_000_000)return "约"+(value/10_000)+"万";
        if(value<1_000_000_000_000L)return "约"+(value/100_000_000)+"亿";
        return "约"+(value/1_000_000_000_000L)+"万亿";
    }
    /** Active queues are bounded; keep every state visible even if a future producer reports larger counts. */
    private static String pendingCount(Long value) {
        if (value == null || value < 0) return "未知";
        return value < 10_000 ? value.toString() : "≥1万";
    }
    private static String clip(String value,int limit) {
        if(value==null||value.isEmpty())return "未观察";
        if(value.length()<=limit)return value;
        int end=limit-1;
        if(Character.isHighSurrogate(value.charAt(end-1)))end--;
        return value.substring(0,end)+"…";
    }
}
