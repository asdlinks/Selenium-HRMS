package com.mywehr.utils;

import com.mywehr.driver.DriverManager;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Records the page's write requests (anything but GET) - method, URL, status
 * and the start of the response body - so a failure can say what the server
 * answered when the UI says nothing.
 *
 * Diagnostic only: requests pass through unchanged. Some apps reject a save
 * without showing an error on screen (a 409 behind a dialog that just stays
 * open); the recorded response is then the only place the reason exists.
 *
 *   NetworkRecorder.start();
 *   ... click Save ...
 *   Log.info("API writes: " + NetworkRecorder.recordedWrites());
 */
public final class NetworkRecorder {

    private NetworkRecorder() {
    }

    /** Clears earlier records and installs the fetch/XHR wrappers once per page load. */
    public static void start() {
        js().executeScript("""
                window.__apiWrites = [];
                if (window.__apiRecorderInstalled) return;
                window.__apiRecorderInstalled = true;
                const keep = (method, url, status, body) => {
                    if ((method || 'GET').toUpperCase() !== 'GET')
                        window.__apiWrites.push(method + ' ' + url + ' -> ' + status + ' ' + String(body).slice(0, 300));
                };
                const originalFetch = window.fetch;
                window.fetch = async (...args) => {
                    const response = await originalFetch(...args);
                    const method = (args[1] && args[1].method) || (args[0] && args[0].method) || 'GET';
                    const url = typeof args[0] === 'string' ? args[0] : args[0].url;
                    response.clone().text().then(t => keep(method, url, response.status, t)).catch(() => {});
                    return response;
                };
                const open = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function (method, url, ...rest) {
                    this.addEventListener('loadend', () => keep(method, url, this.status, this.responseText));
                    return open.call(this, method, url, ...rest);
                };
                """);
    }

    /** The writes recorded since {@link #start()}, one "METHOD url -> status body" line each. */
    public static String recordedWrites() {
        return String.valueOf(js().executeScript("return window.__apiWrites || [];"));
    }

    private static JavascriptExecutor js() {
        return (JavascriptExecutor) DriverManager.get();
    }
}
