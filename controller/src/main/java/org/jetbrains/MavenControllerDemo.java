package org.jetbrains;

import java.io.IOException;

public class MavenControllerDemo {
    public static void main(String[] args) throws IOException {
        MavenController controller = new MavenController("mvn_test_project/");
        controller.install();
        controller.compile();
    }
}
