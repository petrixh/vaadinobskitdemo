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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.io.IOException;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) 
@Tag("playwright") 
public class PlaywrightTest {

    // This will be injected with the random free port
    // number that was allocated
    @LocalServerPort 
    private int port; // = 8080;

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

        //Verify grafana has data... 
        page.navigate("http://hostmachine:" + 3000 + "/");
        



        
        
        //Take screenshot and save it in the target folder
        takeScreenshot("Screenshot-"+imageCounter++ +".png", page); 

        page.getByPlaceholder("email or username").fill("admin");
        page.getByLabel("Password input field").fill("admin");
        
        takeScreenshot("Screenshot-"+imageCounter++ +".png", page); 

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login button")).click();

        
        page.navigate("http://hostmachine:" + 3000 + "/d/6_bNYpGVy/vaadin-dashboard-3-1-0?orgId=1&refresh=5s");


        try{
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
		}  

        takeScreenshot("Screenshot-"+imageCounter++ +".png", page); 
        page.getByText("Traces").isVisible(); 

          // Verify Traces panel has data
        assertThat(page.getByText("Traces")).isVisible();
        assertThat(page.locator("[data-testid='data-testid Panel header Traces'] + div table tbody tr").first()).isVisible();
        
        // Verify Logs panel has data  
        assertThat(page.getByText("Logs")).isVisible();
        assertThat(page.locator("[data-testid='data-testid Panel header Logs'] + div table tbody tr").first()).isVisible();
        
        // Verify Memory Usage graph has data points
        //assertThat(page.locator("canvas").filter(new Page.FilterOptions().setHasText("Memory"))).isVisible();
        
        // Verify CPU Utilization graph has data points
        //assertThat(page.locator("canvas").filter(new Page.FilterOptions().setHasText("CPU"))).isVisible();
        
        // Additional verification - check if graphs contain actual data by looking for graph elements
        //assertThat(page.locator("[data-testid*='graph'] svg path")).first().isVisible();


        //http://localhost:3000/d/6_bNYpGVy/vaadin-dashboard-3-1-0?orgId=1&refresh=5s

        





    }

    private int smokeTest(int counter) {
        page.navigate("http://hostmachine:" + port + "/");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        assertThat(page.getByText("Service health")).isVisible(); 
        
        page.navigate("http://hostmachine:" + port + "/hello");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        assertThat(page.getByText("Custom span/attribute example")).isVisible(); 
        
        page.navigate("http://hostmachine:" + port + "/about");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        page.navigate("http://hostmachine:" + port + "/image-list");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        page.navigate("http://hostmachine:" + port + "/master-detail-slow");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 

        page.navigate("http://hostmachine:" + port + "/opt-master-detail");
        takeScreenshot("Screenshot-"+counter++ +".png", page); 
        assertThat(page.getByText("Optimized Master-Detail")).isVisible();
        return counter;
    }

    private void takeScreenshot(String name, Page page){

        try{
            Thread.sleep(250); 
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
		}  

        page.screenshot();
        byte[] screenshot = page.screenshot();
        try {
			java.nio.file.Files.write(java.nio.file.Paths.get("/workspaces/vaadinobskitdemo-1/target/" + name), screenshot);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}     
    }
}
