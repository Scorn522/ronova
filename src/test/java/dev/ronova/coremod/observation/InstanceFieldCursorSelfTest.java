package dev.ronova.coremod.observation;

import java.util.HashSet;

public final class InstanceFieldCursorSelfTest {
    private static class Wide {
        Object a00,a01,a02,a03,a04,a05,a06,a07,a08,a09,a10,a11,a12,a13,a14,a15,
            a16,a17,a18,a19,a20,a21,a22,a23,a24,a25,a26,a27,a28,a29,a30,a31,
            a32,a33,a34,a35,a36,a37,a38,a39,a40,a41,a42,a43,a44,a45,a46,a47,
            a48,a49,a50,a51,a52,a53,a54,a55,a56,a57,a58,a59,a60,a61,a62,a63;
        Object zSnapshot = new Object();
    }
    private static final class Child extends Wide {
        static Object ignoredStatic;
        int ignoredPrimitive;
        Object childValue;
    }
    private static final class Other { Object replacement; }
    public static void main(String[] args) throws Exception {
        var cursor = new InstanceFieldCursor();
        var found = new HashSet<String>(); var owner = new Child(); boolean completed = false;
        for (int visit = 0; visit < 100; visit++) {
            var step = cursor.next(Child.class);
            require(!step.unavailable(), "fixture field unexpectedly unavailable");
            if (step.complete()) { completed = true; break; }
            if (step.field() == null) continue;
            require(found.add(step.field().getName()), "field was repeatedly inspected instead of advancing");
            if (step.field().getName().equals("zSnapshot"))
                require(step.field().get(owner) == owner.zSnapshot && found.size() > 64,
                    "late Object-typed snapshot was not reached after the first 64 references");
        }
        require(completed && found.size() == 66 && found.contains("zSnapshot")
            && !found.contains("ignoredStatic") && !found.contains("ignoredPrimitive"),
            "hierarchy traversal omitted fields or included static/primitive slots");
        require(cursor.next(Child.class).complete(), "completed cursor silently restarted");
        require(cursor.next(Other.class).field().getName().equals("replacement"), "different class reused an old position");
        cursor.clear();
        require(cursor.next(Wide.class).field().getName().equals("a00"), "explicit reset retained stale position");
        var layout = ReflectionFieldLayout.get(Wide.class, 100);
        require(ReflectionFieldLayout.get(Wide.class, 101) == layout && layout.size() == 65
            && layout.field(0).getName().equals("a00") && layout.field(64).getName().equals("zSnapshot"),
            "warm discovery rebuilt or reordered its field layout");
        var refreshed = ReflectionFieldLayout.get(Wide.class, 1_000_000_100L);
        require(refreshed != layout && refreshed.size() == layout.size() && refreshed.shape().equals(layout.shape())
            && ReflectionFieldLayout.get(Other.class, 1_000_000_100L).size() == 1,
            "expired metadata was reused or one class inherited another layout");
        require(ReflectionFieldLayout.get(Wide.class, 50) != refreshed,
            "clock reversal retained a stale metadata lifetime");
        require(!ReflectionFieldLayout.get(Other.class).shape().equals(layout.shape()),
            "different field structures had the same layout identity");
        require(cursor.next(Wide.class).field().getName().equals("a01"),
            "metadata refresh reset progress despite unchanged structure");
        System.out.println("InstanceFieldCursorSelfTest PASS");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
