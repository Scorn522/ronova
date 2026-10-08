package com.mojang.logging;

import org.slf4j.Logger;

/** Compile/test-only logging stub; never packaged. */
public final class LogUtils {
    private static final Logger LOGGER = new Logger() {};

    private LogUtils() {}

    public static Logger getLogger() { return LOGGER; }
}
