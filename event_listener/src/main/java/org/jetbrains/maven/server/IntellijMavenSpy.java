package org.jetbrains.maven.server;

import lombok.extern.log4j.Log4j2;
import org.apache.maven.eventspy.AbstractEventSpy;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Named("Intellij Idea Maven Embedded Event Spy")
@Singleton
@Log4j2
public class IntellijMavenSpy extends AbstractEventSpy {
    private EventClient client;

    @Override
    public void init(Context context) throws Exception {
        log.info("Project Directory: " + context.getData().get("workingDirectory") + "\n");
        client = new EventClient("localhost", System.getenv("BSP_EVENT_PORT"));
    }

    @Override
    public void close() throws Exception {
        log.info("IntellijMavenSpy::close");
        client.close();
    }

    @Override
    public void onEvent(Object event) {
        log.info("IntellijMavenSpy::onEvent");
        try {
            client.send(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
