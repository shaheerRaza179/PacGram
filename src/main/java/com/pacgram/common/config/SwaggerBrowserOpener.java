package com.pacgram.common.config;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SwaggerBrowserOpener {
    private static final Logger log = LoggerFactory.getLogger(SwaggerBrowserOpener.class);

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${pacgram.swagger.auto-open:true}")
    private boolean autoOpen;

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerWhenReady() {
        String url = "http://localhost:" + serverPort + "/swagger-ui/index.html";
        if (!autoOpen) {
            log.info("Automatic Swagger browser opening is disabled. Swagger UI is available at {}", url);
            return;
        }
        try {
            openInDefaultBrowser(URI.create(url));
            log.info("Requested the default browser to open Swagger UI at {}", url);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            log.warn("Could not open Swagger UI automatically at {}", url, exception);
        }
    }

    private void openInDefaultBrowser(URI uri) throws IOException {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (operatingSystem.contains("win")) {
            new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", uri.toString()).start();
            return;
        }
        if (operatingSystem.contains("mac")) {
            new ProcessBuilder("open", uri.toString()).start();
            return;
        }
        if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return;
        }
        new ProcessBuilder("xdg-open", uri.toString()).start();
    }
}
