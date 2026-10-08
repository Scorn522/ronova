package dev.ronova.coremod.integrity;

/** Stateless execution canary for the registered Native code-image guard. */
public final class CodeSealProbe {
    private CodeSealProbe() {}
    public static long evaluate(long challenge) {
        long mixed=challenge^0x7170D15EA5E12345L;
        return ((mixed<<17)|(mixed>>>47))+0x13579BDF2468ACE0L;
    }
}
