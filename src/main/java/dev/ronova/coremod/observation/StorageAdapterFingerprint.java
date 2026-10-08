package dev.ronova.coremod.observation;

import java.io.*;
import java.security.*;
import java.util.HexFormat;

/** Artifact fingerprint for persisted plan compatibility; not an attestation of executing instructions. */
final class StorageAdapterFingerprint {
    private static final String HASH=compute();
    static String value(){return HASH;}
    private static String compute() {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(Class<?> type:new Class<?>[]{NbtStorageImage.class,NbtWriteReferences.class,SavedDataEncodedWrite.class,StorageImageTask.class,
                StorageTransactionTask.class,RecoverySourceTracker.StorageCommitRequest.class}) {
                String name="/"+type.getName().replace('.','/')+".class";
                try(var input=type.getResourceAsStream(name)) {
                    if(input==null)return null;byte[] bytes=input.readNBytes(1024*1024+1);
                    if(bytes.length>1024*1024)return null;
                    digest.update(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);
                    digest.update(bytes);digest.update((byte)0);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(IOException|GeneralSecurityException|RuntimeException unavailable){return null;}
    }
}
