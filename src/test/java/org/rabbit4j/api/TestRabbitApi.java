package org.rabbit4j.api;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestRabbitApi {


    @Test
    public void testIssueEvent() throws Exception {
        RabbitApi api = new RabbitApi("https://onlinehelp.cyberlogitec.com", "username", "password");
        assertNotNull(api.getUserApi());
    }

}
