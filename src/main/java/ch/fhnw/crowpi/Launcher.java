package ch.fhnw.crowpi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

import ch.fhnw.crowpi.applications.DummyApp;

@Command(name = "CrowPi Example Launcher", synopsisSubcommandLabel = "COMMAND", mixinStandardHelpOptions = true)
public final class Launcher {
    private static final List<Application> APPLICATIONS = new ArrayList<>(Arrays.asList(
        new DummyApp()
    ));

    private final CommandLine cmdLine;
    private final Context pi4j;

    public static void main(String[] args) {
        final var launcher = new Launcher();
        System.exit(launcher.execute(args));
    }

    public Launcher() {
        // Initialize PicoCLI instance
        this.cmdLine = new CommandLine(this);

        // Initialize Pi4J context
        this.pi4j = Pi4J.newAutoContext();

        // Register application runners as subcommands
        this.registerApplicationRunners();
    }
    
    public int execute(String[] args) {
        return this.cmdLine.execute(args);
    }

    private void registerApplicationRunners() {
        for(Application app: APPLICATIONS) {
            final var runner = new ApplicationRunner(app);
            final var cmd = CommandSpec.forAnnotatedObject(runner).name(app.getName());
            cmd.usageMessage().description(app.getDescription());
            this.cmdLine.addSubcommand(cmd);
        }
    }

    @Command()
    private final class ApplicationRunner implements Runnable {
        private final Application app;

        public ApplicationRunner(Application app) {
            this.app = app;
        }

        @Override
        public void run() {
            this.app.execute(Launcher.this.pi4j);
        }
    }
}
