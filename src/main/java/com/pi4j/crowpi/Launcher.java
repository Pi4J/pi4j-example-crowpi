package com.pi4j.crowpi;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.crowpi.applications.*;
import com.pi4j.crowpi.helpers.CrowPiPlatform;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalInputProvider;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalOutputProvider;
import com.pi4j.plugin.pigpio.provider.i2c.PiGpioI2CProvider;
import com.pi4j.plugin.pigpio.provider.pwm.PiGpioPwmProvider;
import com.pi4j.plugin.pigpio.provider.serial.PiGpioSerialProvider;
import com.pi4j.plugin.pigpio.provider.spi.PiGpioSpiProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

import java.util.*;

@Command(name = "CrowPi Example Launcher", mixinStandardHelpOptions = true)
public final class Launcher implements Runnable {
    /**
     * This list must contain all applications which should be executable through the launcher.
     * Each class instance must implement the Application interface and gets automatically added as a subcommand.
     */
    private static final List<Application> APPLICATIONS = new ArrayList<>(Arrays.asList(
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

    private final CommandLine cmdLine;
    private final Context pi4j;
    private final List<Application> applications;
    private final List<ApplicationRunner> runners = new ArrayList<>();

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

        // Initialize PiGPIO
        final var piGpio = PiGpio.newNativeInstance();

        // Initialize Pi4J context by manually specifying the desired platform and providers
        // FIXME: This can probably be replaced by `.newAutoContext()` once https://github.com/Pi4J/pi4j-v2/issues/17 has been resolved
        this.pi4j = Pi4J.newContextBuilder()
            .noAutoDetect()
            .add(new CrowPiPlatform())
            .add(
                PiGpioDigitalInputProvider.newInstance(piGpio),
                PiGpioDigitalOutputProvider.newInstance(piGpio),
                PiGpioPwmProvider.newInstance(piGpio),
                PiGpioI2CProvider.newInstance(piGpio),
                PiGpioSerialProvider.newInstance(piGpio),
                PiGpioSpiProvider.newInstance(piGpio)
            )
            .build();

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
        // Print informational header that no application has been specified
        System.out.println("> No application has been specified, defaulting to manual selection");
        System.out.println("> Run this launcher with --help for further information");

        // Print list of possible choices
        System.out.println("> The following applications can be launched:");
        for (int i = 0; i < this.runners.size(); i++) {
            final var runner = this.runners.get(i);
            final var appName = runner.getApp().getName();
            final var appDescription = runner.getApp().getDescription();

            System.out.println((i + 1) + ") " + appName + " (" + appDescription + ")");
        }

        // Read stdin until user has made a valid choice
        // We have to use println() here due to buffering in exec-maven-plugin
        final var in = new Scanner(System.in);
        var choice = 0;
        while (choice < 1 || choice > this.runners.size()) {
            System.out.println("> Please choose your desired application by typing the appropriate number:");
            try {
                choice = in.nextInt();
            } catch (InputMismatchException ignored) {
                in.next();
            }
        }

        // Launch chosen application
        final var runner = this.runners.get(choice - 1);
        final var appName = runner.getApp().getName();
        System.out.println("> Launching application " + appName + "...");
        runner.run();
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
