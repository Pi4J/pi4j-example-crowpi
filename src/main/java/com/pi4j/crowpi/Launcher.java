package com.pi4j.crowpi;

import com.pi4j.context.Context;
import com.pi4j.crowpi.applications.*;
import com.pi4j.crowpi.helpers.CrowPiPlatform;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import java.util.*;
import java.util.stream.Collectors;

@Command(name = "CrowPi Example Launcher", version = "1.0.0", mixinStandardHelpOptions = true)
public final class Launcher implements Runnable {
    /**
     * This list must contain all applications which should be executable through the launcher.
     * Each class instance must implement the Application interface and gets automatically added as a subcommand.
     */
    public static final List<Application> APPLICATIONS = new ArrayList<>(Arrays.asList(
        new ButtonApp(),
        new ButtonMatrixApp(),
        new BuzzerApp(),
        new ExampleApp(),
        new HumiTempApp(),
        new IrReceiverApp(),
        new LcdDisplayApp(),
        new LedMatrixApp(),
        new LightSensorApp(),
        new RfidApp(),
        new PirMotionSensorApp(),
        new RelayApp(),
        new ServoMotorApp(),
        new SevenSegmentApp(),
        new SoundSensorApp(),
        new StepMotorApp(),
        new TiltSensorApp(),
        new TouchSensorApp(),
        new UltrasonicDistanceSensorApp(),
        new VibrationMotorApp()
    ));

    /**
     * Demo mode will keep the launcher running forever, allowing the consecutive execution of several applications.
     * This value gets dynamically set to {@code false} when the user selects to exit the launcher in the interactive menu.
     */
    @CommandLine.Option(names = {"-d", "--demo"}, description = "Enable demo mode to run multiple applications consecutively")
    private boolean demoMode = false;

    /**
     * PicoCLI command line instance used for parsing
     */
    private final CommandLine cmdLine;

    /**
     * Pi4J context for CrowPi platform
     */
    private Context pi4j;

    /**
     * List of available applications to be executed
     */
    private final List<Application> applications;

    /**
     * List of dynamically generated application runners
     */
    private final List<ApplicationRunner> runners = new ArrayList<>();

    /**
     * Main application entry point which executes the launcher and exits afterwards.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        final var launcher = new Launcher(APPLICATIONS);
        System.exit(launcher.execute(args));
    }

    /**
     * Creates a new launcher with an eagerly initialized Pi4J context and PicoCLI instance.
     * All applications specified in APPLICATIONS are being automatically registered.
     *
     * @param applications List of applications to register
     */
    public Launcher(List<Application> applications) {
        // Initialize PicoCLI instance
        this.cmdLine = new CommandLine(this);

        // Register application runners as subcommands
        this.applications = applications;
        this.registerApplicationRunners();
    }

    /**
     * When the user does not specify any sub-command and therefore does not explicitly state which application should be launched,
     * a manual console based selection menu of all known applications will be shown. This allows to easily run any desired application
     * without having to mess with the arguments passed to the launcher.
     */
    @Override
    public void run() {
        // Build list of targets only once for performance reasons
        final var targets = buildTargets();

        // Print informational header based on current operation mode
        if (demoMode) {
            System.out.println("> Launcher was started in demo mode and will not automatically exit");
        } else {
            System.out.println("> No application has been specified, defaulting to interactive selection");
            System.out.println("> Run this launcher with --help for further information");
        }

        // Interactively ask the user for a desired target and run it
        // This loop will either run only once or forever, depending on the state of `demoMode`
        do {
            // Initialize Pi4J context
            pi4j = CrowPiPlatform.buildNewContext();
            // Run the application
            getTargetInteractively(targets).run();
            // Clean up
            pi4j.shutdown();
        } while (demoMode);
    }

    /**
     * Presents the passed list of targets to the user as a numbered list and waits until a valid choice via stdin has been made.
     * If an invalid input occurs, this method will keep retrying until a valid value has been entered.
     *
     * @param targets List of targets to present as a choice
     * @return Selected target by user
     */
    private Target getTargetInteractively(List<Target> targets) {
        // Print numbered list of available targets starting at 1
        System.out.println("> The following launch targets are available:");
        for (int i = 0; i < targets.size(); i++) {
            System.out.println((i + 1) + ") " + targets.get(i).getLabel());
        }

        // Wait for valid choice of user via stdin
        final var in = new Scanner(System.in);
        int choice = 0;
        while (choice < 1 || choice > targets.size()) {
            System.out.println("> Please choose your desired launch target by typing its number:");
            try {
                choice = in.nextInt();
            } catch (InputMismatchException ignored) {
                in.next();
            }
        }

        // Return selected choice
        return targets.get(choice - 1);
    }

    /**
     * Builds a list of launcher targets based on static entries and available application runners
     *
     * @return List of targets
     */
    private List<Target> buildTargets() {
        final var targets = new ArrayList<Target>();

        // Append target for exiting launcher
        // This can be achieved by ensuring that demo mode is disabled, as the launcher will exit too once the application exits
        targets.add(new Target("Exit launcher without running application", () -> demoMode = false, true));

        // Append list of application targets
        targets.addAll(this.runners.stream()
            .map(runner -> {
                final var runnerApp = runner.getApp();
                final var runnerLabel = runnerApp.getName() + " (" + runnerApp.getDescription() + ")";
                return new Target(runnerLabel, runner);
            })
            .collect(Collectors.toList()));

        return targets;
    }

    /**
     * Uses PicoCLI to parse the given command line arguments and returns an appropriate POSIX exit code.
     *
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
        for (Application app : applications) {
            // Create new application runner for the given instance.
            // This is required to represent the application as an actual Runnable.
            final var runner = new ApplicationRunner(app);
            this.runners.add(runner);

            // Create command specification with custom options
            final var cmd = CommandSpec.forAnnotatedObject(runner).name(app.getName());
            cmd.usageMessage().description(app.getDescription());

            // Add command specification as sub-command
            this.cmdLine.addSubcommand(cmd);
        }
    }

    /**
     * Helper class for representing launcher targets which can be interactively chosen if the user does not specify a single app.
     */
    private static final class Target implements Runnable {
        private final String label;
        private final Runnable runnable;
        private final boolean isSilent;

        public Target(String label, Runnable runnable) {
            this(label, runnable, false);
        }

        public Target(String label, Runnable runnable, boolean isSilent) {
            this.label = label;
            this.runnable = runnable;
            this.isSilent = isSilent;
        }

        @Override
        public void run() {
            if (!isSilent) {
                System.out.println("> Launching target [" + getLabel() + "]");
            }
            runnable.run();
            if (!isSilent) {
                System.out.println("> Target [" + getLabel() + "] has exited");
            }
        }

        public String getLabel() {
            return label;
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

        private Application getApp() {
            return this.app;
        }
    }
}
