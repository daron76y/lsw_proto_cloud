package com.lsw_proto_cloud.battle.api;

import org.springframework.web.client.RestTemplate;

public class RestTemplateProvider {
    private static final RestTemplate INSTANCE = new RestTemplate();

    public static RestTemplate get() {
        return INSTANCE;
    }
}