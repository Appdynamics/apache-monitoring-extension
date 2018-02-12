/*
 * Copyright 2013. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.apache;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/21/14
 * Time: 11:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheStatusMonitorTest {
    public static final Logger logger = LoggerFactory.getLogger(ApacheStatusMonitorTest.class);

    @Test
    public void testBigDecimalToString() {
        ApacheMonitoringTask.toBigDecimal("33227844900").toString().equals("33227844900");
        ApacheMonitoringTask.toBigDecimal("332278.900").toString().equals("332278.900");
        ApacheMonitoringTask.toBigDecimal("33").toString().equals("33");
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidNumberToBigDecimal() {
        ApacheMonitoringTask.toBigDecimal("23asf").toString().equals("23asf");
    }
}
