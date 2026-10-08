package dev.ronova.coremod.network;

import java.util.Map;

public final class StrengthStatusDetailSelfTest {
    public static void main(String[] args) {
        String value=StrengthStatusDetail.compose("完成观察".repeat(100),
            StrengthStatusDetail.recovery(Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE),
            "发送等待".repeat(100),"反馈待接续".repeat(100),Map.of("record_tasks_pending",2L,
                "record_tasks_monitor_blocked",1L,"record_tasks_cancel_pending",1L,"record_tasks_expired_pending",0L));
        require(value.length()<=256&&value.contains("整链未证")&&value.contains("/在途2")
            &&value.contains("取消待退1")&&value.contains("发送等待")&&value.contains("反馈待接续"),
            "a bounded GUI detail hid a domain or its unresolved qualification");
        require(value.split(" · ",-1).length==5,"GUI omitted a domain");
        String missing=StrengthStatusDetail.compose(null,null,null,null,Map.of());
        require(missing.contains("在途未知")&&!missing.contains("在途0"),"missing observation became zero pending work");
        require(missing.contains("结果待记未知") && missing.contains("待收集未知"),
            "missing receipt diagnostics became completed work");
        String receiptOnly = StrengthStatusDetail.compose("未完成", "整链未证", "发送正常", "反馈稳定",
            Map.of("mutation_receipts_pending", 7L, "record_results_uncollected", 0L,
                "record_tasks_pending", 0L, "record_tasks_monitor_blocked", 0L,
                "record_tasks_cancel_pending", 0L, "record_tasks_expired_pending", 0L));
        require(receiptOnly.contains("结果待记7/待收集0/在途0"),
            "finished workers hid receipts still awaiting Root acknowledgement");
        String large = StrengthStatusDetail.compose("完成".repeat(100), "来源".repeat(100),
            "投递".repeat(100), "反馈".repeat(100), Map.of("mutation_receipts_pending", Long.MAX_VALUE,
                "record_results_uncollected", Long.MAX_VALUE, "record_tasks_pending", Long.MAX_VALUE,
                "record_tasks_monitor_blocked", Long.MAX_VALUE, "record_tasks_cancel_pending", Long.MAX_VALUE,
                "record_tasks_expired_pending", Long.MAX_VALUE));
        require(large.length() <= 256 && large.contains("结果待记≥1万") && large.contains("待收集≥1万")
            && large.contains("过期待退≥1万") && large.split(" · ", -1).length == 5,
            "large pending counts clipped a receipt state or exceeded the wire limit");
        String unicode=StrengthStatusDetail.compose("a".repeat(52)+"😀".repeat(50),"来源","传送","反馈",Map.of());
        for(int i=0;i<unicode.length();i++) {
            char c=unicode.charAt(i);
            if(Character.isHighSurrogate(c))require(++i<unicode.length()&&Character.isLowSurrogate(unicode.charAt(i)),"split surrogate in detail");
            else require(!Character.isLowSurrogate(c),"orphan surrogate in detail");
        }
        require(StrengthStatusDetail.recovery(0,-1,2).contains("快照未知"),"invalid source count became a valid total");
        require(StrengthStatusDetail.recovery(2,3,4,5).contains("记录5；快照9")
            && StrengthStatusDetail.recovery(2,-1,4,5).contains("记录未知"),
            "loaded record counters were omitted or unknown values became zero");
        System.out.println("StrengthStatusDetailSelfTest PASS");
    }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
