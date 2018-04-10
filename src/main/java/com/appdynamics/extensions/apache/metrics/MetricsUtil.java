/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache.metrics;

import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class MetricsUtil {

    private static final Logger logger = LoggerFactory.getLogger(MetricsUtil.class);

    protected Map<String, String> fetchResponse(Map<String, String> requestMap, String endpoint, CloseableHttpClient httpClient, Pattern splitPattern){

         String url = UrlBuilder.builder(requestMap).path(endpoint).build();
         List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url);
         return parse(responseAsLines, splitPattern);
    }

    protected Map<String, String> parse(List<String> lines, Pattern splitPattern) {

            Map<String, String> valueMap = new HashMap<String, String>();
            for (String line : lines) {
             String[] kv = splitPattern.split(line);
             if (kv.length == 2) {
              String metricName = kv[0].trim();
              String metricValue = kv[1].trim();
              valueMap.put(metricName, metricValue);
             }
            }
            logger.debug("The extracted metrics are " + valueMap);
            return valueMap;
       }
}
