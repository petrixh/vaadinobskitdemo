package com.example.application;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

// Uses the Maven spring-boot:start app instance (port 8080) rather than spawning a second
// uninstrumented instance via @SpringBootTest. Observability Kit 5 is a plain library on that
// app's classpath; the 'it' Maven profile starts it with the 'grafana' Spring profile, which
// points its OTLP exporters at the local collector on 4318.

@Tag("playwright")
public class PlaywrightIT {

    private int port = 8080;

    boolean takeScreenshots = true; 

    private Playwright playwright;
    private Browser browser;
    private Page page;

    private static int PLAYWRIGHT_TIMEOUT = 5000;  
    private static int PLAYWRIGHT_NAVIGATION_TIMEOUT = 5000;  

    /** Grafana dashboard provisioned by the observability-grafana-setup submodule. */
    private static final String DASHBOARD_UID = "vaadin-obskit-5";

    @BeforeEach
    public void setUp() {
        
        playwright = Playwright.create();
        browser = playwright.chromium().connect("ws://127.0.0.1:3001/");
        var ctxOptions = new NewContextOptions(); 
        ctxOptions.setLocale("en-US"); 
        var browserCtx = browser.newContext(ctxOptions); 
        page = browserCtx.newPage(); 
        page.setDefaultTimeout(PLAYWRIGHT_TIMEOUT);
        page.setDefaultNavigationTimeout(PLAYWRIGHT_NAVIGATION_TIMEOUT);
        PlaywrightAssertions.setDefaultAssertionTimeout(PLAYWRIGHT_TIMEOUT);
    }

    @AfterEach
    public void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    public void testClicking() {

        int imageCounter = 0; 

        // Smoke tests.. loop through views once.. 
        imageCounter = smokeTest(imageCounter); 

        //Grafana is a bit special... 
        var ctxOptions = new NewContextOptions(); 
        ctxOptions.setLocale("en-US"); 
        // Tall viewport on purpose: Grafana only renders panels that are in view, and the
        // Traces panel sits below the metric panels on the 5.0.0 dashboard.
        ctxOptions.setViewportSize(1280, 2400);
        var browserCtx = browser.newContext(ctxOptions); 
        page = browserCtx.newPage(); 
        page.setDefaultTimeout(PLAYWRIGHT_TIMEOUT);
        page.setDefaultNavigationTimeout(PLAYWRIGHT_NAVIGATION_TIMEOUT);

        //Verify grafana has data (anonymous auth enabled, no login needed)
        page.navigate("http://hostmachine:" + 3000 + "/d/" + DASHBOARD_UID
                + "/vaadin-dashboard-5-0-0?orgId=1&refresh=5s");

        var tracesPanel = page.locator("[data-testid='data-testid Panel header Traces']");
        tracesPanel.scrollIntoViewIfNeeded();

        System.out.println("...done watiting.");

        takeScreenshot("Screenshot-"+imageCounter++ +".png", page); 

        // Verify the Grafana UI works by checking that the Traces panel has data (no "No data" message)
        assertThat(tracesPanel).isVisible();
        assertThat(tracesPanel.locator("..").getByText("No data")).not().isVisible();

        
        //Grafana is a pain to test, so check the metrics through prometheus and make sure grafana also gets them... 
        boolean hasRecentCpuMetrics = false; 
        
        long start = System.currentTimeMillis(); 
        
        while ( (hasRecentCpuMetrics = hasRecentCpuMetrics()) == false && (System.currentTimeMillis() - start < 60 * 1000)) {
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        
        boolean hasRecentJvmMemoryMetrics = hasRecentJvmMemoryMetrics();

        assertTrue(hasRecentCpuMetrics, () -> "Prometheus did not get CPU telemetry");
        assertTrue(hasRecentJvmMemoryMetrics, () -> "Prometheus did not get JVM Memory telemetry");

        // This seems to take a while even though the data is there in prometheus...
        //assertTrue("Grafana did not get CPU telemetry", hasRecentMetricsViaGrafana());

    }

    /**
     * Verifies that Observability Kit's own Vaadin instrumentation is live, not just the
     * generic Spring/servlet tracing that Spring Boot would produce on its own.
     */
    @Test
    public void testVaadinInstrumentationActive() {
        // Navigate a few views to generate Vaadin-instrumented spans
        page.navigate("http://hostmachine:" + port + "/");
        assertThat(page.getByText("Service health")).isVisible();

        page.navigate("http://hostmachine:" + port + "/hello");
        assertThat(page.getByText("Custom span/attribute example")).isVisible();

        page.navigate("http://hostmachine:" + port + "/about");
        page.navigate("http://hostmachine:" + port + "/master-detail-slow");

        // Poll Tempo for a kit navigation span for the /hello route. Both halves of this query
        // are kit-only: the span is named "vaadin.navigation <route>" (the observation's
        // contextual name) and carries a "route" attribute. Generic Spring/servlet
        // instrumentation produces neither, so nothing else can satisfy the assertion. That
        // matters because the failure this test exists to catch is silent - with the kit
        // disabled the build passes and http.server spans keep flowing while the Vaadin spans
        // quietly disappear.
        //
        // The TraceQL filter only matches once Tempo has completed the block (see
        // max_block_duration in the Grafana setup's tempo.yaml), so this polls rather than
        // asking once - it typically resolves in ~30s, well inside the budget below.
        boolean instrumented = pollTempo(VAADIN_NAVIGATION_QUERY, 90_000);
        assertTrue(instrumented,
                () -> "No " + VAADIN_NAVIGATION_QUERY + " span reached Tempo within 90s of "
                        + "navigating views. Observability Kit's Vaadin instrumentation is not "
                        + "active - check that observability-kit-starter is on the classpath, that "
                        + "vaadin.observability.enabled is not false, and (in dev mode only) that a "
                        + "Vaadin license key is available: without one the kit logs 'No valid "
                        + "vaadin-observability-kit license found' and registers nothing.");
    }

    /**
     * Verifies that the demo's own instrumentation reaches the backend. Under Observability
     * Kit 4 these were {@code @WithSpan} methods and {@code GlobalOpenTelemetry} tracers, both
     * of which are inert under kit 5 - and inert without a single error in the log. This is the
     * canary for that class of silent failure.
     */
    @Test
    public void testCustomObservationsExported() {
        page.navigate("http://hostmachine:" + port + "/order-processing");
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Happy Path").setExact(true)).click();
        assertThat(page.getByText("Order fulfilled successfully")).isVisible();

        boolean parentSpan = pollTempo("{name=\"order.process\"}", 90_000);
        assertTrue(parentSpan,
                () -> "No order.process span reached Tempo within 90s of running the Happy Path "
                        + "scenario. The demo's @Observed annotations are not being applied - check "
                        + "that spring-boot-starter-aspectj is on the classpath and that "
                        + "management.observations.annotations.enabled=true.");

        boolean childSpan = pollTempo("{name=\"order.validate\"}", 60_000);
        assertTrue(childSpan,
                () -> "order.process reached Tempo but its child span order.validate did not, so "
                        + "@Observed is only partly working. Every annotated method must be called "
                        + "from another bean for the proxy-based aspect to fire.");

        // Proves vaadin.observability.database=true is doing something: the order steps persist
        // through Spring Data, and the kit wraps those JDBC calls in vaadin.db.query spans.
        boolean dbSpan = pollTempo("{name=\"vaadin.db.query\"}", 60_000);
        assertTrue(dbSpan,
                () -> "No vaadin.db.query span reached Tempo. Check "
                        + "vaadin.observability.database=true in application.properties.");

        // The Error Path exercises the other half of the port. PaymentService throws out of its
        // @Observed method, so the aspect marks that span errored; OrderProcessingService.fail
        // swallows the same exception and has to call obs.error(e) by hand for order.process.
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Error Path").setExact(true)).click();
        assertThat(page.getByText("Order failed at: Payment")).isVisible();

        boolean erroredSpan = pollTempo("{name=\"order.process_payment\" && status=error}", 90_000);
        assertTrue(erroredSpan,
                () -> "The Error Path scenario did not produce an errored order.process_payment "
                        + "span in Tempo. Observation.error(...) is what marks a span errored under "
                        + "Micrometer; Span.setStatus/recordException no longer exist.");
    }

    private int smokeTest(int counter) {

        page.navigate("http://hostmachine:" + port + "/");
        assertThat(page.getByText("Service health")).isVisible(); 
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/" + " navigation done");
        
        page.navigate("http://hostmachine:" + port + "/hello");
        assertThat(page.getByText("Custom span/attribute example")).isVisible(); 
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/hello" + " navigation done");
        
        page.navigate("http://hostmachine:" + port + "/about");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/about" + " navigation done");

        
        page.navigate("http://hostmachine:" + port + "/image-list");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/image-list" + " navigation done");
        
        page.navigate("http://hostmachine:" + port + "/master-detail-slow");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/master-detail-slow" + " navigation done");
        
        page.navigate("http://hostmachine:" + port + "/opt-master-detail");
        assertThat(page.getByText("Optimized Master-Detail")).isVisible();
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        System.out.println("View "+ "/opt-detail-slow" + " navigation done");
        return counter;
    }

    private void takeScreenshot(String name, Page page){

        
        try{
            Thread.sleep(250); 
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
		}  

        if(takeScreenshots == false){
            return; 
        }
        
        page.screenshot();
        byte[] screenshot = page.screenshot();
        try {
			java.nio.file.Files.write(java.nio.file.Paths.get("target/" + name), screenshot);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
    }

    // Grafana checks: 
    public boolean hasRecentCpuMetrics() {
        // Spring Boot Actuator's JVM binder. The kit 4 agent's jvm_cpu_recent_utilization_ratio
        // no longer exists.
        return hasRecentMetric("process_cpu_usage{exported_job=\"vaadin\"}", "CPU Metrics");
    }

    public boolean hasRecentJvmMemoryMetrics() {
        return hasRecentMetric("jvm_memory_used_bytes{exported_job=\"vaadin\"}", "JVM Memory");
    }

    private boolean hasRecentMetric(String promQl, String label) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            long nowSeconds = Instant.now().getEpochSecond();
            long tenMinutesAgo = nowSeconds - 600;

            String query = URLEncoder.encode(promQl, StandardCharsets.UTF_8);
            String url = String.format("http://localhost:9090/api/v1/query_range?query=%s&start=%d&end=%d&step=15s",
                                    query, tenMinutesAgo, nowSeconds);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(label + " - Status code: " + response.statusCode());
            System.out.println(label + " - Response: " + response.body());

            return response.statusCode() == 200 &&
                   response.body().contains("\"values\":[") &&
                   !response.body().contains("\"values\":[]");
        } catch (Exception e) {
            System.out.println(label + " Error: ");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * A kit navigation span for the /hello route. The span name is the observation's contextual
     * name, "vaadin.navigation " + route, and "route" is a kit-set span attribute.
     */
    private static final String VAADIN_NAVIGATION_QUERY =
            "{name=~\"vaadin.navigation.*\" && span.route=\"hello\"}";

    private static final String TEMPO_PROXY =
            "http://localhost:3000/api/datasources/proxy/uid/tempo";

    private String httpGet(String url) {
        try {
            HttpResponse<String> r = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) {
            System.out.println("Tempo request failed: " + e);
            return null;
        }
    }

    private String tempoSearch(String traceQl, int limit) {
        long now = Instant.now().getEpochSecond();
        return httpGet(String.format("%s/api/search?q=%s&limit=%d&start=%d&end=%d",
                TEMPO_PROXY, URLEncoder.encode(traceQl, StandardCharsets.UTF_8), limit, now - 300, now));
    }

    /**
     * Asks Tempo for a TraceQL match, retrying until it appears or the budget runs out. Filtered
     * queries only match spans in a block Tempo has already completed, so a fresh span can
     * legitimately be missing for the first ~30s.
     */
    private boolean pollTempo(String traceQl, long budgetMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < budgetMillis) {
            String body = tempoSearch(traceQl, 1);
            if (body != null && body.contains("\"traceID\"")) {
                System.out.println("Tempo " + traceQl + ": FOUND");
                return true;
            }
            System.out.println("Tempo " + traceQl + ": not yet");
            try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
        return false;
    }

    public boolean hasRecentMetricsViaGrafana() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String query = URLEncoder.encode("process_cpu_usage{exported_job=\"vaadin\"}", StandardCharsets.UTF_8);
            String url = String.format("http://localhost:3000/api/datasources/proxy/uid/prometheus/api/v1/query?query=%s", query);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Grafana - Status code: " + response.statusCode());
            System.out.println("Grafana - Response: " + response.body());
            
            return response.statusCode() == 200 && 
                   response.body().contains("\"result\":[") && 
                   !response.body().contains("\"result\":[]");
        } catch (Exception e) {
            System.out.println("Grafana Error: ");
            e.printStackTrace();
            return false;
        }
    }
}
