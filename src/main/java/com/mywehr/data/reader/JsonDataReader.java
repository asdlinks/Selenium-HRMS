package com.mywehr.data.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mywehr.data.model.Credential;
import com.mywehr.data.model.EmployeeData;
import com.mywehr.exceptions.FrameworkException;
import com.mywehr.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads datasets from src/test/resources/testdata.
 *
 * Files are read once and cached: a DataProvider is invoked per test method,
 * and re-parsing the same JSON dozens of times per suite is pure waste.
 * The cache is concurrent because DataProviders run on test threads.
 */
public final class JsonDataReader {

    private static final String TEST_DATA_ROOT = "testdata/";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    private JsonDataReader() {
    }

    /** Reads a JSON array file into a typed list. */
    @SuppressWarnings("unchecked")
    public static <T> List<T> readList(String fileName, Class<T> type) {
        String cacheKey = fileName + "#" + type.getName();
        return (List<T>) CACHE.computeIfAbsent(cacheKey, key -> {
            try (InputStream stream = open(fileName)) {
                List<T> values = MAPPER.readValue(stream,
                        MAPPER.getTypeFactory().constructCollectionType(List.class, type));
                Log.info("Loaded " + values.size() + " record(s) from " + fileName);
                return values;
            } catch (IOException e) {
                throw new FrameworkException("Unable to parse test data file: " + fileName, e);
            }
        });
    }

    /** Reads a JSON object file keyed by scenario name. */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> readMap(String fileName, Class<T> type) {
        String cacheKey = fileName + "#map#" + type.getName();
        return (Map<String, T>) CACHE.computeIfAbsent(cacheKey, key -> {
            try (InputStream stream = open(fileName)) {
                return MAPPER.readValue(stream,
                        MAPPER.getTypeFactory().constructMapType(Map.class, String.class, type));
            } catch (IOException e) {
                throw new FrameworkException("Unable to parse test data file: " + fileName, e);
            }
        });
    }

    public static Map<String, Object> readRaw(String fileName) {
        try (InputStream stream = open(fileName)) {
            return MAPPER.readValue(stream, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new FrameworkException("Unable to parse test data file: " + fileName, e);
        }
    }

    private static InputStream open(String fileName) {
        InputStream stream = JsonDataReader.class.getClassLoader()
                .getResourceAsStream(TEST_DATA_ROOT + fileName);
        if (stream == null) {
            throw new FrameworkException(
                    "Test data file not found on classpath: " + TEST_DATA_ROOT + fileName);
        }
        return stream;
    }

    // ------------------------------------------------------ named datasets

    public static Map<String, Credential> credentials() {
        return readMap("credentials.json", Credential.class);
    }

    public static Credential credentialFor(String personaKey) {
        Credential credential = credentials().get(personaKey);
        if (credential == null) {
            throw new FrameworkException("No credential entry for persona key: " + personaKey);
        }
        return credential;
    }

    public static List<Credential> invalidLoginScenarios() {
        return readList("invalid-logins.json", Credential.class);
    }

    public static List<EmployeeData> employeeValidationScenarios() {
        return readList("employee-validation.json", EmployeeData.class);
    }
}
