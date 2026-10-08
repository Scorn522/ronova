package dev.ronova.coremod.root.api;

import java.util.Objects;
import java.util.UUID;

/** Durable save incarnation and commit semantics. Contains no holder, loader, path pointer or handle. */
public record RecoveryStorageGeneration(String backend,UUID registration,long generation) {
    public static final String WINDOWS_NTFS_TXF="windows-ntfs-txf/v1";
    public RecoveryStorageGeneration {
        Objects.requireNonNull(backend);Objects.requireNonNull(registration);
        if(!WINDOWS_NTFS_TXF.equals(backend)||generation<1)throw new IllegalArgumentException("unsupported storage generation");
    }
    public String canonical(){return backend+"\n"+registration+"\n"+generation;}
    public static RecoveryStorageGeneration parse(String text) {
        if(text==null||text.length()>128)throw new IllegalArgumentException("invalid storage generation");
        String[] fields=text.split("\n",-1);if(fields.length!=3)throw new IllegalArgumentException("invalid storage generation fields");
        var result=new RecoveryStorageGeneration(fields[0],UUID.fromString(fields[1]),Long.parseLong(fields[2]));
        if(!result.canonical().equals(text))throw new IllegalArgumentException("noncanonical storage generation");return result;
    }
}
