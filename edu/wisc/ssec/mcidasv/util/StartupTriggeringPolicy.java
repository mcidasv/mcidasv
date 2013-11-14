package edu.wisc.ssec.mcidasv.util;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RolloverFailure;

/**
 * This is a Logback {@literal "triggering policy"} that forces a log
 * {@literal "roll"} upon starting McIDAS-V.
 *
 * <p>All credit for this belongs to
 * <a href="http://stackoverflow.com/a/12408445">this StackOverflow post</a>.</p>
 *
 */
@NoAutoStart
public class StartupTriggeringPolicy<E>
        extends DefaultTimeBasedFileNamingAndTriggeringPolicy<E> {

    @Override public void start() {
        super.start();
        nextCheck = 0L;
        isTriggeringEvent(null, null);
        try {
            tbrp.rollover();
        } catch (RolloverFailure e) {
            //Do nothing
        }
    }

}