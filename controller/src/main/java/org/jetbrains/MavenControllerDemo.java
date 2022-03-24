package org.jetbrains;

public class MavenControllerDemo {
    public static void main(String[] args) {
        MavenController controller = new MavenController();
        controller.install("mvn_test_project/");
        controller.compile("mvn_test_project/");
    }
}
