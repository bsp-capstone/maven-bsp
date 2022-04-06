package org.jetbrains.maven.project;

import lombok.Value;

@Value
public class ProjectId {
  private final String groupId;
  private final String artifactId;
  private final String version;
}
