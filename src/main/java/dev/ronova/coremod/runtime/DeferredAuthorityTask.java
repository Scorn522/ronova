package dev.ronova.coremod.runtime;

import dev.ronova.coremod.root.api.AuthorityStamp;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Single-use delayed work expectation; admission/action permission is still checked by the body. */
public final class DeferredAuthorityTask {
    public enum Result { EXECUTED, STALE, EXPIRED, CONTEXT_LOST, ALREADY_CONSUMED }
    private final AuthorityStamp expected;
    private final long createdAt;
    private final long lifetime;
    private final AtomicBoolean consumed = new AtomicBoolean();
    public DeferredAuthorityTask(AuthorityStamp expected, long createdAt, long lifetime) {
        this.expected = Objects.requireNonNull(expected, "expected");
        if (lifetime <= 0) throw new IllegalArgumentException("task lifetime must be positive");
        this.createdAt = createdAt; this.lifetime = lifetime;
    }
    public Result run(AuthorityStamp current, boolean sameContext, long now, Runnable body) {
        Objects.requireNonNull(body, "body");
        if (!consumed.compareAndSet(false, true)) return Result.ALREADY_CONSUMED;
        if (!sameContext) return Result.CONTEXT_LOST;
        long age = now - createdAt;
        if (age < 0 || age > lifetime) return Result.EXPIRED;
        if (!expected.settled() || !expected.sameAuthority(current)) return Result.STALE;
        body.run(); return Result.EXECUTED;
    }
}
