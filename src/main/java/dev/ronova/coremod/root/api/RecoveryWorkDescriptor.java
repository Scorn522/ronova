package dev.ronova.coremod.root.api;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stable metadata inside EffectJournal. No live task, loader, holder, file handle or world reference is serialized. */
public record RecoveryWorkDescriptor(Kind kind,RecoverySourceLocation location,UUID registration,long sourceVersion,
                                     int copyDepth,String adapterHash,String baselineHash,String expectedHash,boolean descendantTask,String fileIdentity,
                                     RecoveryStorageGeneration storageGeneration) {
    public RecoveryWorkDescriptor(Kind kind,RecoverySourceLocation location,UUID registration,long sourceVersion,int copyDepth,
        String adapterHash,String baselineHash,String expectedHash,boolean descendantTask,String fileIdentity) {
        this(kind,location,registration,sourceVersion,copyDepth,adapterHash,baselineHash,expectedHash,descendantTask,fileIdentity,null);
    }
    public RecoveryWorkDescriptor(Kind kind,RecoverySourceLocation location,UUID registration,long sourceVersion,int copyDepth,
        String adapterHash,String baselineHash,String expectedHash,boolean descendantTask) {
        this(kind,location,registration,sourceVersion,copyDepth,adapterHash,baselineHash,expectedHash,descendantTask,"");
    }
    public RecoveryWorkDescriptor(Kind kind,RecoverySourceLocation location,UUID registration,long sourceVersion,int copyDepth,
        String adapterHash,String baselineHash,String expectedHash) {
        this(kind,location,registration,sourceVersion,copyDepth,adapterHash,baselineHash,expectedHash,false);
    }
    public enum Kind {
        TASK_CANCEL(0), STORAGE_COMMIT(1), RESOURCE_RELEASE(2);
        final int wire;Kind(int wire){this.wire=wire;}
    }
    public static final String OPERATION="ronova:recovery-work/v1";
    public RecoveryWorkDescriptor {
        Objects.requireNonNull(kind);Objects.requireNonNull(location);Objects.requireNonNull(registration);
        if(sourceVersion<1||copyDepth<0)throw new IllegalArgumentException("invalid recovery work source version");
        adapterHash=EffectRecord.checkedHash(adapterHash);baselineHash=EffectRecord.checkedHash(baselineHash);expectedHash=EffectRecord.checkedHash(expectedHash);
        if(descendantTask&&kind!=Kind.TASK_CANCEL)throw new IllegalArgumentException("descendant task flag on non-task work");
        Objects.requireNonNull(fileIdentity);
        if(!fileIdentity.isEmpty()&&(kind!=Kind.STORAGE_COMMIT||!canonicalFileIdentity(fileIdentity)))
            throw new IllegalArgumentException("invalid storage file identity");
        if(storageGeneration!=null&&(kind!=Kind.STORAGE_COMMIT||fileIdentity.isEmpty()))
            throw new IllegalArgumentException("storage generation without file identity");
    }
    private static boolean canonicalFileIdentity(String value) {
        if(value.length()>66)return false;
        String[] fields=value.split(":",-1);
        if(fields.length!=4||!fields[0].equals("win32-file-id/1"))return false;
        try {
            for(int i=1;i<4;i++) {
                if(fields[i].isEmpty()||fields[i].length()>16
                    ||!Long.toUnsignedString(Long.parseUnsignedLong(fields[i],16),16).equals(fields[i]))return false;
            }
            return true;
        } catch(NumberFormatException invalid){return false;}
    }
    public String canonical() {
        return (storageGeneration!=null?"4\n":!fileIdentity.isEmpty()?"3\n":descendantTask?"2\n":"1\n")+kind.name()+"\n"+Base64.getUrlEncoder().withoutPadding().encodeToString(location.canonical().getBytes(StandardCharsets.UTF_8))
            +"\n"+registration+"\n"+sourceVersion+"\n"+copyDepth+"\n"+adapterHash+"\n"+baselineHash+"\n"+expectedHash+(descendantTask?"\ndescendant":!fileIdentity.isEmpty()?"\n"+fileIdentity:"")
            +(storageGeneration==null?"":"\n"+storageGeneration.canonical());
    }
    public static RecoveryWorkDescriptor parse(String value) {
        if(value==null||value.length()>16_384)throw new IllegalArgumentException("invalid recovery work description");
        String[] parts=value.split("\n",-1);
        boolean descendant=parts.length==10&&parts[0].equals("2")&&parts[9].equals("descendant");
        boolean file=parts.length==10&&parts[0].equals("3");
        boolean generation=parts.length==13&&parts[0].equals("4");
        if(!generation&&!file&&!descendant&&(parts.length!=9||!parts[0].equals("1")))throw new IllegalArgumentException("unsupported recovery work description");
        var result=new RecoveryWorkDescriptor(Kind.valueOf(parts[1]),RecoverySourceLocation.parse(new String(Base64.getUrlDecoder().decode(parts[2]),StandardCharsets.UTF_8)),
            UUID.fromString(parts[3]),Long.parseLong(parts[4]),Integer.parseInt(parts[5]),parts[6],parts[7],parts[8],descendant,file||generation?parts[9]:"",
            generation?RecoveryStorageGeneration.parse(parts[10]+"\n"+parts[11]+"\n"+parts[12]):null);
        if(!result.canonical().equals(value))throw new IllegalArgumentException("noncanonical recovery work description");return result;
    }
    public EffectKey effectKey(TerminalOperationPlan terminal) {
        return new EffectKey(terminal.commandId(),terminal.qualification().targetEpoch(),OPERATION,
            slot());
    }
    public boolean matches(EffectKey key) {
        return OPERATION.equals(key.semanticOperationId())&&(key.semanticSlotId().equals(slot())||storageGeneration==null&&key.semanticSlotId().equals(legacySlot()));
    }
    private String legacySlot(){return "work/"+kind.wire+"/"+location.locatorHash()+"/"+registration.toString().replace("-","")+"/"+sourceVersion+(descendantTask?"/d":"");}
    private String slot(){return storageGeneration!=null?"work/"+kind.wire+"/g/"+EffectRecord.hash(legacySlot()+"\n"+copyDepth+"\n"+storageGeneration.canonical()):
        copyDepth<=1?legacySlot():"work/"+kind.wire+"/c/"+EffectRecord.hash(location.locatorHash()+"\n"+registration+"\n"+sourceVersion+"\n"+copyDepth+"\n"+descendantTask);}
    public int permissionDepth(){return descendantTask?Math.max(2,copyDepth):copyDepth;}
    public String payloadHash(EffectKey key,String authorityHash) {
        if(!matches(key))throw new IllegalArgumentException("recovery work key does not match description");
        return EffectRecord.hash(OPERATION+"\n"+key.canonical()+"\n"+EffectRecord.checkedHash(authorityHash)+"\n"+canonical());
    }
    public EffectRecord prepared(TerminalOperationPlan terminal) {
        EffectKey key=effectKey(terminal);
        return new EffectRecord(key,terminal.authorityHash(),terminal.authorityCommitIndex(),payloadHash(key,terminal.authorityHash()),
            terminal.activePolicyRevision(),terminal.planId(),terminal.planRevision(),"recovery-work",UUID.randomUUID(),
            EffectRecord.SideEffectClass.EXTERNAL_OPAQUE,EffectRecord.RecoveryCapability.QUERYABLE,EffectRecord.State.PREPARED,1,"recovery-work-intent");
    }
    public boolean allows(RecoveryWorkResult.Fact fact) {
        return switch(fact) {
            case NOT_CURRENT,EFFECT_UNKNOWN,RESOURCES_RELEASED -> true;
            case CANCEL_REQUESTED,CANCEL_ACCEPTED,CANCEL_REJECTED,EXECUTION_EXITED,SUCCESSORS_CLOSED,NOT_EXECUTED -> kind==Kind.TASK_CANCEL;
            case MEMORY_CHANGED,SERIALIZED,FILE_COMMITTED,READBACK_VERIFIED,PRESERVATION_VERIFIED -> kind==Kind.STORAGE_COMMIT;
        };
    }
    public void validateFacts(EffectRecord record) {
        long mask=RecoveryWorkResult.facts(record);
        for(var fact:RecoveryWorkResult.Fact.values())if((mask&fact.bit)!=0&&!allows(fact))throw new IllegalArgumentException("fact belongs to another recovery work kind");
    }
}
