package dev.ronova.coremod.observation;

import dev.ronova.coremod.root.api.*;

public final class RecoverySourceLocationSelfTest {
    public static void main(String[] args) {
        var statik = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER,
            "", "", "test.Manager", "records", "", "");
        require(statik.locatorHash().equals(EffectRecord.hash("static-nbt-record\ntest.Manager#records\ndirect")),
            "static descriptor changed the existing effect locator");
        var loaded = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA,
            "test:dimension", "缓存:folder/name", "test.Data", "records", "test.Entry", "snapshot");
        require(loaded.locatorHash().equals(EffectRecord.hash("test:dimension/缓存:folder/name\ntest.Data#records\ntest.Entry#snapshot")),
            "loaded descriptor changed the existing effect locator");
        for (var location : new RecoverySourceLocation[] { statik, loaded }) {
            require(RecoverySourceLocation.parse(location.canonical()).equals(location), "descriptor did not round trip");
            for (String malformed : new String[] { location.canonical() + "extra", location.canonical().substring(0, location.canonical().length()-1),
                location.canonical().replace("ronova-source-location/1", "ronova-source-location/2"),
                location.canonical().replaceFirst("\\n", "\n0") }) {
                boolean rejected = false;
                try { RecoverySourceLocation.parse(malformed); } catch (IllegalArgumentException expected) { rejected = true; }
                require(rejected, "malformed descriptor was accepted");
            }
        }
        boolean rejected = false;
        try { new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER, "test:dimension", "key", "Owner", "field", "", ""); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "static descriptor accepted loaded storage coordinates");
        rejected = false;
        try { new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA, "dimension", "key", "Owner", "field", "Entry", ""); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "partial snapshot field was accepted as a direct record");
        System.out.println("RecoverySourceLocationSelfTest PASS");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
