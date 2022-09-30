package org.jetbrains.maven.server;

import org.apache.maven.eventspy.AbstractEventSpy;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Named("Intellij Idea Maven Embedded Event Spy")
@Singleton
public class IntellijMavenSpy extends AbstractEventSpy {
  private EventClient client;

  @Override
  public void init(Context context) throws Exception {
    System.out.println("Project Directory: " + context.getData().get("workingDirectory"));
    client = new EventClient("localhost", System.getenv("BSP_EVENT_PORT"));
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

  @Override
  public void onEvent(Object event) {
    try {
      client.send(event);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
