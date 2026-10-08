package dev.ronova.coremod.network;

import dev.ronova.coremod.engine.JudgmentMark;
import java.io.*;
import java.security.*;
import java.util.*;

/** Canonical bounded view digest; protocol evidence, not proof of arbitrary rendered pixels. */
public final class ProjectionDigest {
    private ProjectionDigest() {}
    public static String of(Collection<JudgmentMark> marks) {
        if(marks.size()>ProjectionFrame.MAX_MARKS)throw new IllegalArgumentException("projection digest capacity");
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            try(var out=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),digest))) {
                out.writeInt(1);out.writeInt(marks.size());
                for(var mark:marks.stream().sorted(Comparator.comparing(JudgmentMark::uuid)).toList()) {
                    out.writeLong(mark.uuid().getMostSignificantBits());out.writeLong(mark.uuid().getLeastSignificantBits());
                    out.writeUTF(mark.type());out.writeUTF(mark.dimension());out.writeDouble(mark.x());out.writeDouble(mark.y());out.writeDouble(mark.z());
                    out.writeDouble(mark.radius());out.writeInt(mark.scope().ordinal());
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch(IOException|NoSuchAlgorithmException failure){throw new IllegalStateException("projection digest unavailable",failure);}
    }
}
