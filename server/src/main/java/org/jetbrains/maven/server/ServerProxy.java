package org.jetbrains.maven.server;

import ch.epfl.scala.bsp4j.BuildClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Build tool command (argv JSON field in the connection file) to start the BSP server. Client
 * listens to system output and writes to system input.
 *
 * <p>todo Resul: Add intermediary class which will generate connection file.
 */
public class ServerProxy {

  public static void main(String[] args) throws IOException {
    ExecutorService threadPool = Executors.newCachedThreadPool();
    MavenBSPServer mavenBSPServer = new MavenBSPServer();

    PrintStream stdout = System.out;
    InputStream stdin = System.in;

    Launcher<BuildClient> launcher =
        new Launcher.Builder<BuildClient>()
            .setLocalService(mavenBSPServer)
            .setOutput(stdout)
            .setInput(stdin)
            .setRemoteInterface(BuildClient.class)
            .setExecutorService(threadPool)
            .create();

    mavenBSPServer.setClient(launcher.getRemoteProxy());
    launcher.startListening();
  }
}
