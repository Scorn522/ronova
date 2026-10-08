package dev.ronova.coremod.root.api;

import dev.ronova.coremod.extension.VersionedExtensionCodec;

/** 7.17 codecs must encode the SubjectKey and per-subject generation; legacy values are unqualified. */
public interface TargetEpochExtension extends VersionedExtensionCodec<TargetEpoch> {}
