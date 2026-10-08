package dev.ronova.coremod.integrity;

import java.util.concurrent.atomic.AtomicInteger;

public final class EnforcementRuntimeSelfTest {
    private EnforcementRuntimeSelfTest() {}

    public static void main(String[] args) {
        verifyBoundedPollingAndPublication();
        verifyFailureIsolation();
        verifyChallengePublication();
        verifyProofNormalization();
        verifyObservationPairing();
        verifyObservationLifetime();
        verifyRawDispatch();
        verifyWorldIndexClassRequirement();
        System.out.println(
            "EnforcementRuntimeSelfTest: PASS "
                + "(bounded polling, immutable publication, challenge isolation, failure isolation)"
        );
    }

    private static void verifyWorldIndexClassRequirement() {
        class Source implements EnforcementProofSource {
            int mask=0x1f85;
            public EnforcementProof read(){return proof(true,mask,mask);}
            public CodeIntegrityProof codeIntegrity(){return new CodeIntegrityProof(true,2,2,2,0);}
        }
        var source=new Source();var runtime=new EnforcementRuntime(source);
        int required=dev.ronova.coremod.policy.TerminalActionPolicy.Step.REMOVED_RECORDS.mask();
        require(!runtime.observesRegisteredSemantics(required),"Level evidence substituted for missing ServerLevel evidence");
        source.mask=0x1f89;
        require(runtime.observesRegisteredSemantics(required),"registered world index class inventory was refused");
    }

    private static void verifyBoundedPollingAndPublication() {
        AtomicInteger reads = new AtomicInteger();
        EnforcementProof active = proof(true, 0x1f, 0x1f);
        EnforcementRuntime runtime = new EnforcementRuntime(() -> {
            reads.incrementAndGet();
            return active;
        }, 20);

        runtime.pulse(0);
        runtime.pulse(1);
        runtime.pulse(19);
        require(reads.get() == 1, "proof source entered the server hot path");
        require(runtime.published().equals(active), "active proof was not published");

        runtime.pulse(20);
        require(reads.get() == 2 && runtime.polls() == 2L,
            "proof source did not poll at the frozen one-second boundary");
        runtime.pulse(Integer.MIN_VALUE);
        require(reads.get() == 3, "unsigned tick wrap did not reopen the poll boundary");
    }

    private static void verifyFailureIsolation() {
        AtomicInteger reads = new AtomicInteger();
        EnforcementRuntime runtime = new EnforcementRuntime(() -> {
            if (reads.getAndIncrement() == 0) return null;
            throw new IllegalStateException("injected read failure");
        }, 1);
        runtime.pulse(0);
        require(runtime.published() == EnforcementProof.UNAVAILABLE,
            "null proof must publish unavailable");
        runtime.pulse(1);
        require(runtime.published() == EnforcementProof.UNAVAILABLE,
            "read exception must preserve unavailable projection");
        require(runtime.readFailures() == 2L,
            "proof read failures were not observable");
    }


    private static void verifyChallengePublication() {
        class Source implements EnforcementProofSource {
            boolean fail;
            public EnforcementProof read(){return proof(true,0x1f,0x1f);}
            public CodeIntegrityProof codeIntegrity(){return new CodeIntegrityProof(true,2,2,2,0);}
            public CapabilityChallengeProof challengeProof(){
                if(fail)throw new IllegalStateException("challenge failure");
                return new CapabilityChallengeProof(true,true,4,4,4,0);
            }
        }
        var source=new Source();var runtime=new EnforcementRuntime(source,1);
        runtime.pulse(0);
        require(runtime.challengeProof().current()&&runtime.challengeProof().generation()==4,
            "behavior challenge evidence was not published");
        source.fail=true;runtime.pulse(1);
        require(runtime.challengeProof()==CapabilityChallengeProof.UNAVAILABLE,
            "challenge exception retained stale proof");
        require(runtime.published().current()&&runtime.codeIntegrity().current(),
            "challenge failure destroyed independent semantic/code evidence");
        require(runtime.challengeReadFailures()==1,
            "challenge read failure was not observable");
    }

    private static void verifyProofNormalization() {
        EnforcementProof incomplete = proof(true, 0x1f, 0x0f);
        require(!incomplete.current() && incomplete.state() == EnforcementProof.State.DEGRADED,
            "incomplete semantic coverage must never claim active");
        require(EnforcementProof.UNAVAILABLE.state() == EnforcementProof.State.UNAVAILABLE,
            "unsupported proof must report unavailable");
        require(proof(true, 0x8063, 0x8063).current(), "leaf accessor bit was truncated");
        require(!proof(true, 0x8063, 0x63).current(), "old health mask attested the new leaf getter");
    }

    private static void verifyObservationPairing() {
        var stable = new CodeIntegrityProof(true, 10, 8, 8, 0);
        class Source implements EnforcementProofSource {
            int semanticReads, codeReads; boolean semanticChange, codeChange, unavailable;
            public EnforcementProof read() {
                long transforms = semanticChange && semanticReads++ > 0 ? 6 : 5;
                return new EnforcementProof(true,true,true,0x6000,0x6000,1,1,0,transforms,0,1,0,0);
            }
            public CodeIntegrityProof codeIntegrity() {
                if (unavailable) return CodeIntegrityProof.UNAVAILABLE;
                return codeChange && codeReads++ > 0 ? new CodeIntegrityProof(true,10,8,8,1) : stable;
            }
        }
        var source = new Source();
        require(new EnforcementRuntime(source).observesRegisteredSemantics(0x6000),"stable registered observation refused");
        source = new Source();source.semanticChange=true;
        require(!new EnforcementRuntime(source).observesRegisteredSemantics(0x6000),"semantic/code ABA accepted");
        source = new Source();source.codeChange=true;
        require(!new EnforcementRuntime(source).observesRegisteredSemantics(0x6000),"different code snapshots combined");
        source = new Source();source.unavailable=true;
        require(!new EnforcementRuntime(source).observesRegisteredSemantics(0x6000),"unavailable code became proof");
        source = new Source();
        require(!new EnforcementRuntime(source).observesRegisteredSemantics(0x6001),"partial required mask accepted");
        require(!new EnforcementRuntime(source).observesRegisteredSemantics(0),"empty mask accepted");
    }

    private static void verifyObservationLifetime() {
        class Source implements EnforcementProofSource {
            long generation=1, transformations=5, replacements=0, age=0;
            boolean available=true;
            public EnforcementProof read() {
                return available ? new EnforcementProof(true,true,true,0x6000,0x6000,
                    generation,1,0,transformations,0,1,age,0) : EnforcementProof.UNAVAILABLE;
            }
            public CodeIntegrityProof codeIntegrity() { return new CodeIntegrityProof(true,10,8,8,replacements); }
        }
        var source=new Source();
        var now=new java.util.concurrent.atomic.AtomicLong();
        var runtime=new EnforcementRuntime(source,20,now::get);
        var observation=runtime.beginObservation(0x6000);
        require(observation!=null&&observation.stillCurrent(),"fresh operation observation unavailable");
        source.age=7;
        require(observation.finish(),"normal proof age invalidated stable operation");
        require(!observation.finish()&&!observation.stillCurrent(),"finished observation reused");
        observation.close();
        require(observation.state()==EnforcementRuntime.Observation.State.CONFIRMED,"close fabricated a second result");

        observation=runtime.beginObservation(0x6000);source.transformations++;
        require(!observation.finish(),"changed transform generation published an observation");
        source.transformations--;
        require(!observation.stillCurrent(),"invalidated observation revived after ABA");
        observation=runtime.beginObservation(0x6000);source.generation++;
        require(!observation.finish(),"new Native lease silently extended an old observation");
        observation=runtime.beginObservation(0x6000);source.replacements++;
        require(!observation.finish(),"code-image replacement published old result");
        observation=runtime.beginObservation(0x6000);source.available=false;
        require(!observation.stillCurrent(),"Native unavailable gap accepted");
        source.available=true;
        require(!observation.finish(),"recovered Native revived old observation");

        observation=runtime.beginObservation(0x6000);
        now.set(EnforcementRuntime.OBSERVATION_WINDOW_NANOS-1);
        require(observation.stillCurrent(),"observation expired before its boundary");
        now.incrementAndGet();
        require(!observation.finish(),"observation survived its deadline");
        require(observation.state()==EnforcementRuntime.Observation.State.EXPIRED,"deadline reason lost");
        observation=runtime.beginObservation(0x6000);now.decrementAndGet();
        require(!observation.finish(),"negative elapsed clock accepted");
        observation=runtime.beginObservation(0x6000);observation.close();
        require(!observation.finish()&&observation.state()==EnforcementRuntime.Observation.State.CLOSED,
            "close became successful observation");

        var foreign=runtime.beginObservation(0x6000);
        Thread thread=new Thread(()->require(!foreign.stillCurrent(),"observation used by a foreign thread"));
        thread.start();
        try { thread.join(); } catch(InterruptedException interrupted) {
            Thread.currentThread().interrupt();throw new AssertionError(interrupted);
        }
        require(!foreign.finish()&&foreign.state()==EnforcementRuntime.Observation.State.WRONG_THREAD,
            "foreign use did not permanently invalidate observation");
        source.available=false;
        require(runtime.beginObservation(0x6000)==null,"unavailable Native issued an observation");
    }

    public interface RawParent { Object ronova$value(); }
    public interface RawChild extends RawParent {}
    public interface EmptyRaw {}
    public static class RawBase implements RawChild {
        public Object ronova$value() { return this; }
    }
    public static class RawDerived extends RawBase {}
    public static class RawOverride extends RawBase {
        @Override public Object ronova$value() { return null; }
    }
    public static class EmptyBase implements EmptyRaw {}
    private static void verifyRawDispatch() {
        require(RawAccessorDispatch.matches(new RawDerived(),RawBase.class,RawChild.class),"inherited base dispatch refused");
        require(!RawAccessorDispatch.matches(new RawOverride(),RawBase.class,RawChild.class),"inherited interface hid subclass override");
        require(!RawAccessorDispatch.matches(new EmptyBase(),EmptyBase.class,EmptyRaw.class),"empty accessor attested execution");
        require(!RawAccessorDispatch.matches(new RawBase(),RawBase.class,RawBase.class),"non-interface accessor accepted");
        require(!RawAccessorDispatch.matches(null,RawBase.class,RawChild.class),"null receiver accepted");
    }

    private static EnforcementProof proof(boolean current, int required, int verified) {
        return new EnforcementProof(
            true, current, true, required, verified,
            1L, 1L, 0L, 5L, 0L, 1L, 10L, 0L
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
