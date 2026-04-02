package com.example.application;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
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

//TODO clean up one day.... 

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) 
@Tag("playwright") 
public class PlaywrightIT {

    // This will be injected with the random free port
    // number that was allocated
    @LocalServerPort 
    private int port; // = 8080;

    boolean takeScreenshots = true; 

    private Playwright playwright;
    private Browser browser;
    private Page page;

    private static int PLAYWRIGHT_TIMEOUT = 5000;  
    private static int PLAYWRIGHT_NAVIGATION_TIMEOUT = 5000;  

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
        

        //page = browser.newPage(); 


        //Grafana is a bit special... 
        var ctxOptions = new NewContextOptions(); 
        ctxOptions.setLocale("en-US"); 
        var browserCtx = browser.newContext(ctxOptions); 
        page = browserCtx.newPage(); 

        //Verify grafana has data (anonymous auth enabled, no login needed)
        page.navigate("http://hostmachine:" + 3000 + "/d/6_bNYpGV4/vaadin-dashboard-4-0-0?orgId=1&refresh=5s");




        System.out.println("...done watiting.");

        takeScreenshot("Screenshot-"+imageCounter++ +".png", page); 
        page.getByText("Traces").isVisible(); 

        // Verify the Grafana UI works by checking that the Traces panel has data (no "No data" message)
        assertThat(page.getByText("Traces")).isVisible();
        assertThat(page.locator("[data-testid='data-testid Panel header Traces']").locator("..").getByText("No data")).not().isVisible();

        
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
     * Verifies that the VOKS agent Vaadin instrumentation is actually applied
     * by navigating views and checking Tempo for spans with vaadin.flow.version
     * attribute. This catches silent muzzle failures where standard OTel traces
     * (JPA, Spring) work but Vaadin UI instrumentation is not applied due to
     * version incompatibility.
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

        // Give Tempo time to flush the WAL to searchable blocks
        // (trace_idle_period=5s + max_block_duration=30s)
        try { Thread.sleep(40_000); } catch (InterruptedException e) { e.printStackTrace(); }

        // Poll for up to 60s more after the initial wait
        boolean hasVaadinTraces = false;
        long start = System.currentTimeMillis();
        while (!hasVaadinTraces && (System.currentTimeMillis() - start < 60 * 1000)) {
            hasVaadinTraces = hasVaadinInstrumentedTraces();
            if (!hasVaadinTraces) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
        assertTrue(hasVaadinTraces,
                () -> "Tempo has no traces with vaadin.flow.version attribute after navigating views. " +
                        "VOKS Vaadin instrumentation is not applied - check agent version compatibility with Vaadin version.");
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
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            long nowSeconds = Instant.now().getEpochSecond();
            long sixtySecondsAgo = nowSeconds - 600;
            
            // Use the correct metric name with label filter (4.0.0 agent appends _ratio suffix)
            String query = URLEncoder.encode("jvm_cpu_recent_utilization_ratio{exported_job=\"vaadin\"}", StandardCharsets.UTF_8);
            String url = String.format("http://localhost:9090/api/v1/query_range?query=%s&start=%d&end=%d&step=15s", 
                                    query, sixtySecondsAgo, nowSeconds);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("CPU Metrics - Status code: " + response.statusCode());
            System.out.println("CPU Metrics - Response: " + response.body());
            
            return response.statusCode() == 200 && 
                   response.body().contains("\"values\":[") && 
                   !response.body().contains("\"values\":[]");
        } catch (Exception e) {
            System.out.println("CPU Metrics Error: ");
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasRecentJvmMemoryMetrics() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            long nowSeconds = Instant.now().getEpochSecond();
            long sixtySecondsAgo = nowSeconds - 600;
            
            // Try JVM memory with vaadin job filter (4.0.0 agent appends _bytes suffix)
            String query = URLEncoder.encode("jvm_memory_used_bytes{exported_job=\"vaadin\"}", StandardCharsets.UTF_8);
            String url = String.format("http://localhost:9090/api/v1/query_range?query=%s&start=%d&end=%d&step=15s", 
                                    query, sixtySecondsAgo, nowSeconds);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("JVM Memory - Status code: " + response.statusCode());
            System.out.println("JVM Memory - Response: " + response.body());
            
            return response.statusCode() == 200 && 
                   response.body().contains("\"values\":[") && 
                   !response.body().contains("\"values\":[]");
        } catch (Exception e) {
            System.out.println("JVM Memory Error: ");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks Tempo directly for traces that have the vaadin.flow.version
     * span attribute. This attribute is set by VOKS Vaadin UI instrumentation on
     * request handler spans and proves the bytecode instrumentation is actively applied.
     * Uses the Tempo HTTP API directly (port 3200 mapped to a dynamic host port)
     * via the Grafana proxy to avoid needing to discover the dynamic port.
     */
    public boolean hasVaadinInstrumentedTraces() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            long nowSeconds = Instant.now().getEpochSecond();
            long fiveMinutesAgo = nowSeconds - 300;

            // First check if Tempo has ANY traces at all (helps diagnose pipeline issues)
            String anyQuery = URLEncoder.encode("{}", StandardCharsets.UTF_8);
            String anyUrl = String.format(
                    "http://localhost:3000/api/datasources/proxy/uid/tempo/api/search?q=%s&limit=1&start=%d&end=%d",
                    anyQuery, fiveMinutesAgo, nowSeconds);
            HttpRequest anyRequest = HttpRequest.newBuilder().uri(URI.create(anyUrl)).GET().build();
            HttpResponse<String> anyResponse = client.send(anyRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Any Traces - Status: " + anyResponse.statusCode() + " Response: " + anyResponse.body());

            // Now check for vaadin-specific traces
            String query = URLEncoder.encode("{span.vaadin.flow.version!=\"\"}", StandardCharsets.UTF_8);
            String url = String.format(
                    "http://localhost:3000/api/datasources/proxy/uid/tempo/api/search?q=%s&limit=1&start=%d&end=%d",
                    query, fiveMinutesAgo, nowSeconds);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Vaadin Traces - Status code: " + response.statusCode());
            System.out.println("Vaadin Traces - Response: " + response.body());

            return response.statusCode() == 200
                    && response.body().contains("\"traceID\"");
        } catch (Exception e) {
            System.out.println("Vaadin Traces Error: ");
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasRecentMetricsViaGrafana() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            // Use the correct metric name for Grafana query (4.0.0 agent appends _ratio suffix)
            String query = URLEncoder.encode("jvm_cpu_recent_utilization_ratio{exported_job=\"vaadin\"}", StandardCharsets.UTF_8);
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
