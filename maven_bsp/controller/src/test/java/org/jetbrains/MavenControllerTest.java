package org.jetbrains;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MavenControllerTest {
  private BuildClient fakeClient;
  private ArgumentCaptor<TaskProgressParams> progressArgument;
  private ArgumentCaptor<ShowMessageParams> messageArgument;

  @BeforeEach
  public void initMock() {
    fakeClient = mock(BuildClient.class);
    progressArgument = ArgumentCaptor.forClass(TaskProgressParams.class);
    messageArgument = ArgumentCaptor.forClass(ShowMessageParams.class);
  }

  @BeforeAll
  private static void installJar() {
    MavenController controller = new MavenController(null);
    controller.installSilent("../event-listener/pom.xml");
  }

  @Test
  public void simpleMockTest() {
    TaskProgressParams prog = new TaskProgressParams(new TaskId("something not null"));
    fakeClient.onBuildTaskProgress(prog);
    Mockito.verify(fakeClient).onBuildTaskProgress(progressArgument.capture());
    Assertions.assertEquals(prog, progressArgument.getValue());

    ShowMessageParams msg = new ShowMessageParams(MessageType.INFORMATION, "str");
    fakeClient.onBuildShowMessage(msg);
    Mockito.verify(fakeClient).onBuildShowMessage(messageArgument.capture());
    Assertions.assertEquals(msg, messageArgument.getValue());
  }

  // @Test
  public void mockTest() {
    MavenController controller = new MavenController(fakeClient);
    controller.compile("src/test/mvn_test_project");

    Mockito.verify(fakeClient, times(4)).onBuildTaskProgress(progressArgument.capture());
    Mockito.verify(fakeClient, times(4)).onBuildShowMessage(messageArgument.capture());
    for (int i = 0; i < 4; i++) {
      System.out.println(messageArgument.getAllValues().get(i).getMessage());
      System.out.println(progressArgument.getAllValues().get(i).getMessage());
    }
  }
}
