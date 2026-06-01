package controller;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFxmlTest {
    @Test
    void shouldDefineLoginAndRegistrationViewInFxml() throws Exception {
        try (InputStream input = LoginFxmlTest.class.getResourceAsStream("/view/fxml/login.fxml")) {
            assertNotNull(input);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            NodeList nodes = document.getElementsByTagName("*");

            Set<String> ids = Set.of(
                    "authTabs",
                    "usernameField",
                    "passwordField",
                    "roleBox",
                    "fullNameField",
                    "registerUserField",
                    "registerPasswordField",
                    "registerRoleBox",
                    "loginButton",
                    "registerButton"
            );
            int matchedIds = 0;
            boolean hasLoginHandler = false;
            boolean hasRegisterHandler = false;
            for (int index = 0; index < nodes.getLength(); index++) {
                String id = nodes.item(index).getAttributes().getNamedItem("fx:id") == null
                        ? null
                        : nodes.item(index).getAttributes().getNamedItem("fx:id").getNodeValue();
                if (id != null && ids.contains(id)) {
                    matchedIds++;
                }
                String action = nodes.item(index).getAttributes().getNamedItem("onAction") == null
                        ? null
                        : nodes.item(index).getAttributes().getNamedItem("onAction").getNodeValue();
                hasLoginHandler |= "#handleLogin".equals(action);
                hasRegisterHandler |= "#handleRegister".equals(action);
            }

            assertEquals(ids.size(), matchedIds);
            assertTrue(hasLoginHandler);
            assertTrue(hasRegisterHandler);
        }
    }
}
