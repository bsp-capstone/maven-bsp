package maven_controller;

public class MavenControllerDemo {
    public static void main(String[] args) {
        MavenController controller = new MavenController("mvn_test_project/");
        controller.install();
        controller.compile();
    }
}
