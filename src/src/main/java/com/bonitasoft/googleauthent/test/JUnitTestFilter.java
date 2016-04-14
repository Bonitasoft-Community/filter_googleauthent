package com.bonitasoft.googleauthent.test;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.Test;

import com.bonitasoft.googleauthent.FilterGoogle;

public class JUnitTestFilter {

    @Test
    public void testResolveTechnicalUser() {
        final FilterGoogle filterGoogle = new FilterGoogle();
        filterGoogle.addInListTechnicalUsers("user|patrick.gardenier");
        filterGoogle.addInListTechnicalUsers("group|/acme/sales"); // william.jobs / sale asia : misa.kumagai
        filterGoogle.addInListTechnicalUsers("role|qualityManager"); // thorsten.hartmann
        filterGoogle.addInListTechnicalUsers("profile|t3"); // => Reference walter.Bates and Role SpecialAdmin >  joseph.hovell

        final APISession apiSession = login();
        filterGoogle.resolveTechnicalUsers(apiSession, 1);
    }

    private APISession login()
    {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("server.url", "http://localhost:8080");
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

        // Set the username and password
        // final String username = "helen.kelly";
        final String username = "walter.bates";
        final String password = "bpm";

        // get the LoginAPI using the TenantAPIAccessor
        LoginAPI loginAPI;
        try {
            loginAPI = TenantAPIAccessor.getLoginAPI();
            // log in to the tenant to create a session
            final APISession apiSession = loginAPI.login(username, password);
            return apiSession;
        } catch (final Exception e)
        {
            System.out.println("Login error " + e.toString());
        }
        return null;
    }
}
