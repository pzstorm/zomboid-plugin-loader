package dev.weary.zomboid;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;


public class Main {
    static {
        loadLibraryFrom("./natives", "attach");
    }

    // Launched as a regular Java program
    public static void main(String[] args) {
        try {
            File thisJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!thisJar.getPath().endsWith(".jar")) {
                String agentPath = getArgAfterLast(args, "--agent");
                if (agentPath == null) {
                    System.out.println("Can't find the .jar file of this program\n");
                    System.out.println("Make sure you're launching a .jar file (and not .class files!)");
                    System.out.println("  Incorrect: java -cp * " + Main.class.getName());
                    System.out.println("  Correct:   java -jar pz-launcher.jar\n");
                    System.out.println("If there is no .jar file (e.g when running from IDE), you can pass the");
                    System.out.println("path to a prebuilt .jar artifact as an argument to this program");
                    System.out.println("  Example: java -cp * " + Main.class.getName() + " --agent ../build/pz-launcher.jar");
                    System.exit(1);
                    return;
                }

                File agentJar = new File(agentPath);
                if (!agentJar.getPath().endsWith(".jar")) {
                    System.out.println("The file you want to launch does not end with .jar");
                    System.out.println("  " + agentJar.getAbsolutePath());
                    System.exit(1);
                    return;
                }

                if (!agentJar.exists()) {
                    System.out.println("The .jar file you want to launch does not exist");
                    System.out.println("  " + agentJar.getAbsolutePath());
                    System.exit(1);
                    return;
                }

                thisJar = agentJar;
            }

            VirtualMachineDescriptor vmDescriptor;
            String targetId = getArgAfterLast(args, "--pid");
            if (targetId != null) {
                vmDescriptor = findFirstVm(vm -> vm.id().equals(targetId));

                if (vmDescriptor == null) {
                    System.out.println("Can't find a VM with ID " + targetId);
                    System.out.println("There are currently " + VirtualMachine.list().size() + " VMs");
                    VirtualMachine.list().forEach(vm -> System.out.println("  " + vm.id() + ": " + vm.displayName()));
                    System.exit(1);
                }
            }
            else {
                // TODO: Add server signature
                vmDescriptor = findFirstVm(vm -> vm.displayName().startsWith("zombie.gameStates.MainScreenState"));

                if (vmDescriptor == null) {
                    System.out.println("Can't find a running Project Zomboid process\n");
                    System.out.println("If you're sure it's running, you can force launch");
                    System.out.println("by passing the PID as an argument to this program");
                    System.out.println("  Example: java -jar " + thisJar.getName() + " --pid 1337\n");
                    System.out.println("There are currently " + VirtualMachine.list().size() + " VMs");
                    VirtualMachine.list().forEach(vm -> System.out.println("  " + vm.id() + ": " + vm.displayName()));
                    System.exit(1);
                    return;
                }
            }

            boolean didAttach = false;
            VirtualMachine targetVm;
            try {
                targetVm = VirtualMachine.attach(vmDescriptor);
            }
            catch (Exception e) {
                System.out.println("Can't attach to VM with ID " + vmDescriptor.id() + ": " + e.getMessage());
                System.exit(1);
                return;
            }

            try {
                targetVm.loadAgent(thisJar.getAbsolutePath(), "");
                didAttach = true;
            }
            catch (Exception e) {

                // Not an error, see https://stackoverflow.com/a/54454418
                if (e.getMessage().equals("0")) {
                    didAttach = true;
                }
                else {
                    System.out.println("Can't load into VM with ID " + vmDescriptor.id() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            try {
                targetVm.detach();
            }
            catch (Exception e) {
                System.out.println("Can't detach from VM with ID " + vmDescriptor.id() + ": " + e.getMessage());
            }

            if (!didAttach) {
                System.out.println("Did not attach successfully");
                System.exit(1);
                return;
            }

            System.exit(0);
            System.out.println("Attached successfully");
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static VirtualMachineDescriptor findFirstVm(Predicate<VirtualMachineDescriptor> vmPredicate) {
        VirtualMachineDescriptor vmDescriptor;

        // Silence OpenJDK 11 fake exception printing to stderr
        // TODO: Submit to https://github.com/AdoptOpenJDK/openjdk-support/issues
        PrintStream oldPrintStream = redirectStdErr(EMPTY_STREAM);
        vmDescriptor = VirtualMachine.list().stream().filter(vmPredicate).findFirst().orElse(null);
        redirectStdErr(oldPrintStream);

        return vmDescriptor;
    }

    private static String getArgAfterLast(String[] programArgs, String argKey) {
        for (int i = programArgs.length - 1; i >= 0; i--) {
            if (programArgs[i].equals(argKey)) {
                return i + 1 < programArgs.length ? programArgs[i + 1] : "";
            }
        }

        return null;
    }

    private static void loadLibraryFrom(String librariesFolderPath, String libraryName) {
        File librariesFolder = new File(librariesFolderPath);
        if (!librariesFolder.exists()) {
            throw new RuntimeException("Native libraries folder " + librariesFolderPath + " does not exist");
        }

        String platformName = System.mapLibraryName(libraryName);
        File platformLibrary = new File(librariesFolder, platformName);
        if (!platformLibrary.exists()) {
            File[] filesWithSimilarName = librariesFolder.listFiles(file -> file.getName().contains(libraryName));
            boolean similarLibraryExists = filesWithSimilarName != null && filesWithSimilarName.length != 0;
            if (!similarLibraryExists) {
                throw new RuntimeException("Native library " + platformName + " does not exist in " + librariesFolder.getPath());
            }
            else {
                throw new RuntimeException("No native library " + platformName + " for this platform in " + librariesFolder.getPath() +
                        " (found " + Arrays.stream(filesWithSimilarName).map(File::getName).collect(Collectors.joining(
                                ", ")) + " instead)");
            }
        }

        System.load(platformLibrary.getAbsolutePath());
    }

    private static final PrintStream EMPTY_STREAM = new PrintStream(new OutputStream() {
        public void write(int b) {}
    });

    private static PrintStream redirectStdErr(PrintStream newPrintStream) {
        PrintStream oldPrintStream = System.err;
        System.setErr(newPrintStream);
        return oldPrintStream;
    }
}
