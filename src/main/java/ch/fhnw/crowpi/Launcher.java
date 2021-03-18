package ch.fhnw.crowpi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

import ch.fhnw.crowpi.applications.BuzzerApp;

@Command(name = "CrowPi Example Launcher", synopsisSubcommandLabel = "COMMAND", mixinStandardHelpOptions = true)
public final class Launcher {
    /**
     * This list must contain all applications which should be executable through the launcher.
     * Each class instance must implement the Application interface and gets automatically added as a subcommand.
     */
    private static final List<Application> APPLICATIONS = new ArrayList<>(Arrays.asList(
        new BuzzerApp()
    ));

    private final CommandLine cmdLine;
    private final Context pi4j;

    public static void main(String[] args) {
        final var launcher = new Launcher();
        System.exit(launcher.execute(args));
    }

    /**
     * Creates a new launcher with an eagerly initialized Pi4J context and PicoCLI instance.
     * All applications specified in APPLICATIONS are being automatically registered.
     */
    public Launcher() {
        // Initialize PicoCLI instance
        this.cmdLine = new CommandLine(this);

        // Initialize Pi4J context
        this.pi4j = Pi4J.newAutoContext();

        // Register application runners as subcommands
        this.registerApplicationRunners();
    }

    /**
     * Uses PicoCLI to parse the given command line arguments and returns an appropriate POSIX exit code.
     * @param args List of command line arguments to parse, usually provided by the main entrypoint of a Java application
     * @return Exit code after running the requested command
     */
    public int execute(String[] args) {
        return this.cmdLine.execute(args);
    }

    /**
     * Registers all applications as sub-commands by wrapping them with an ApplicationRunner instance.
     * This must only be called once to avoid duplicating sub-commands.
     */
    private void registerApplicationRunners() {
        for (Application app : APPLICATIONS) {
            // Create new application runner for the given instance.
            // This is required to represent the application as an actual Runnable.
            final var runner = new ApplicationRunner(app);

            // Create command specification with custom options for the runner.
            final var cmd = CommandSpec.forAnnotatedObject(runner).name(app.getName());
            cmd.usageMessage().description(app.getDescription());

            // Add command specification as sub-command.
            this.cmdLine.addSubcommand(cmd);
        }
    }

    /**
     * Helper class which wraps around an application to implement the Runnable interface.
     * This can not be done directly in the interface as it will need to pass the Pi4J context of the parent class.
     */
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
