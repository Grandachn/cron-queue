package io.github.grandachn.cronqueue.util;

import lombok.extern.log4j.Log4j;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.util.Date;

@Log4j
public class CronUtils {
    public static Date getNextExecTime(String cronPattern) {
        CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
        try {
            cronTriggerImpl.setCronExpression(cronPattern);
        } catch(ParseException e) {
            log.error("cronJob getNextExecTime error", e);
        }
        return TriggerUtils.computeFireTimes(cronTriggerImpl, null, 1).get(0);
    }

}
