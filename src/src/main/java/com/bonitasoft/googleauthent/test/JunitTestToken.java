package com.bonitasoft.googleauthent.test;

import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class JunitTestToken {

    private final String GOOGLE_SERVER_CLIENT_ID = "494358836642-hnu0gufcrur2tupb2cq4cgigc51l3g00.apps.googleusercontent.com";

    public Logger logger = Logger.getLogger(JunitTestToken.class.getName());

    @Test
    public void test() {
        final String idTokenGoogle = null;

        final NetHttpTransport transport = new NetHttpTransport();
        final GsonFactory jsonFactory = new GsonFactory();

        final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Arrays.asList(GOOGLE_SERVER_CLIENT_ID))
                // To learn about getting a Server Client ID, see this link
                // https://developers.google.com/identity/sign-in/android/start
                // And follow step 4
                // .setIssuer("https://accounts.google.com")
                .build();
        //.setIssuer("http://localhost:8080/bonita").build();

        try {
            logger.info("filterGoogle: Call verifier now");

            final GoogleIdToken idToken = verifier.verify(idTokenGoogle);
            logger.info("filterGoogle: get a idToken ? " + (idToken == null ? "No" : "Yes !!"));
            if (idToken != null) {
                final Payload payload = idToken.getPayload();

                logger.info("filterGoogle: User[" + (String) payload.get("name") + "] UserId[" + payload.getSubject() + "]");
                final String userName = (String) payload.get("name");
            }
        } catch (final Exception e)
        {
            e.printStackTrace();

        }

    }

}
