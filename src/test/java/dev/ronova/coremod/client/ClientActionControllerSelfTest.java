package dev.ronova.coremod.client;

import dev.ronova.coremod.network.RonovaAction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Verifies ordered action delivery, receipt advancement and session replacement. */
public final class ClientActionControllerSelfTest {
    private ClientActionControllerSelfTest() {}

    public static void main(String[] args) {
        List<Sent> sent = new ArrayList<>();
        ClientActionController controller = new ClientActionController(
            (realm, session, sequence, action) ->
                sent.add(new Sent(realm, session, sequence, action))
        );
        UUID firstRealm = UUID.fromString(
            "11111111-2222-3333-4444-555555555555"
        );
        UUID secondRealm = UUID.fromString(
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        );
        UUID firstPlayer = UUID.fromString(
            "00000000-0000-0000-0000-000000000001"
        );
        UUID secondPlayer = UUID.fromString(
            "00000000-0000-0000-0000-000000000002"
        );
        controller.acceptContext(firstRealm, firstPlayer);

        controller.queue(RonovaAction.PRIMARY);
        controller.queue(RonovaAction.AREA);
        require(sent.size() == 1, "only the outbox head may send before its receipt");
        Sent first = sent.get(0);
        require(first.realm.equals(firstRealm)
                && first.sequence == 1L && first.action == RonovaAction.PRIMARY,
            "first request shape drifted");

        controller.acceptFeedback(first.session, first.sequence, first.action, 3);
        require(sent.size() == 2, "receipt must advance and flush the next action");
        Sent second = sent.get(1);
        require(second.session == first.session && second.sequence == 2L
            && second.action == RonovaAction.AREA, "outbox order drifted");
        require(controller.feedbackTicks() == 32 && controller.lastCount() == 3
            && controller.lastAction() == RonovaAction.PRIMARY,
            "feedback state was not retained");

        controller.acceptFeedback(second.session, second.sequence, second.action, 7);
        controller.queue(RonovaAction.LOADED);
        Sent unacknowledged = sent.get(2);
        controller.suspend();
        controller.acceptContext(firstRealm, firstPlayer);
        controller.acceptFeedback(
            unacknowledged.session, unacknowledged.sequence,
            unacknowledged.action, 1
        );
        controller.queue(RonovaAction.PRIMARY);
        Sent sameRealmResume = sent.get(3);
        require(sameRealmResume.realm.equals(firstRealm)
                && sameRealmResume.session == first.session
                && sameRealmResume.sequence == 4L,
            "same-Realm reconnect did not preserve the pending session order");

        controller.queue(RonovaAction.AREA);
        controller.suspend();
        controller.acceptContext(secondRealm, firstPlayer);
        controller.queue(RonovaAction.LOADED);
        Sent differentRealm = sent.get(4);
        require(differentRealm.realm.equals(secondRealm)
                && differentRealm.session != first.session
                && differentRealm.sequence == 1L
                && differentRealm.action == RonovaAction.LOADED,
            "different-Realm reconnect replayed an old-world pending action");

        controller.acceptFeedback(
            differentRealm.session, differentRealm.sequence,
            differentRealm.action, 7
        );
        controller.queue(RonovaAction.PRIMARY);
        controller.suspend();
        controller.acceptContext(secondRealm, secondPlayer);
        controller.queue(RonovaAction.AREA);
        Sent differentPlayer = sent.get(6);
        require(differentPlayer.realm.equals(secondRealm)
                && differentPlayer.session != differentRealm.session
                && differentPlayer.sequence == 1L
                && differentPlayer.action == RonovaAction.AREA,
            "different-player reconnect replayed another player's pending action");

        controller.acceptFeedback(
            differentPlayer.session, differentPlayer.sequence,
            differentPlayer.action, 7
        );
        controller.reset();
        controller.acceptContext(secondRealm, secondPlayer);
        controller.queue(RonovaAction.LOADED);
        Sent replacement = sent.get(7);
        require(replacement.session != first.session && replacement.sequence == 1L,
            "reset must create a new session starting at sequence one");

        System.out.println(
            "ClientActionControllerSelfTest: PASS "
                + "(ordered outbox, receipt, same-context resume, Realm/player fence)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Sent(
        UUID realm,
        long session,
        long sequence,
        RonovaAction action
    ) {}
}
