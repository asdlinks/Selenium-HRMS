package com.mywehr.dataproviders;

import com.mywehr.data.model.Credential;
import com.mywehr.data.reader.JsonDataReader;
import com.mywehr.enums.Persona;
import org.testng.annotations.DataProvider;

/**
 * Every DataProvider in the suite.
 *
 * Most datasets are iterated inside a single test (see the TC_AUTH_02 and
 * TC_EMP_03 loops) so one session covers every scenario with soft assertions.
 * The providers left here split a test per persona, because each persona
 * needs its own sign-in anyway.
 */
public final class TestDataProviders {

    private TestDataProviders() {
    }

    // ----------------------------------------------------------- personas

    @DataProvider(name = "allPersonas")
    public static Object[][] allPersonas() {
        return new Object[][]{{Persona.ADMIN}, {Persona.HR}};
    }

    @DataProvider(name = "validCredentials")
    public static Object[][] validCredentials() {
        return new Object[][]{
                {Persona.ADMIN, JsonDataReader.credentialFor(Persona.ADMIN.key())},
                {Persona.HR, JsonDataReader.credentialFor(Persona.HR.key())}
        };
    }
}
