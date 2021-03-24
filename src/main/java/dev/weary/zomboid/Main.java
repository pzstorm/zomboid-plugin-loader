package dev.weary.zomboid;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.lang.reflect.Field;

public class Main {
    static {
        addNativeLibraries("./natives");
        System.loadLibrary("attach" + System.getProperty("sun.arch.data.model"));
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
                vmDescriptor = VirtualMachine.list().stream()
                    .filter(vm -> vm.id().equals(targetId))
                    .findFirst().orElse(null);

                if (vmDescriptor == null) {
                    System.out.println("Can't find a VM with ID " + targetId);
                    System.out.println("There are currently " + VirtualMachine.list().size() + " VMs");
                    VirtualMachine.list().forEach(vm -> System.out.println("  " + vm.id() + ": " + vm.displayName()));
                    System.exit(1);
                }
            }
            else {
                vmDescriptor = VirtualMachine.list().stream()
                    .filter(vm -> vm.displayName().startsWith("zombie.gameStates.MainScreenState"))
                    .findFirst().orElse(null);

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
                if (!e.getMessage().equals("0")) {
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

    private static String getArgAfterLast(String[] programArgs, String argKey) {
        for (int i = programArgs.length - 1; i >= 0; i--) {
            if (programArgs[i].equals(argKey)) {
                return i + 1 < programArgs.length ? programArgs[i + 1] : "";
            }
        }

        return null;
    }

    private static void addNativeLibraries(String relativePath) {
        File libraryFolder = new File(relativePath);
        if (!libraryFolder.exists()) {
            throw new RuntimeException("Native library folder " + relativePath + " does not exist");
        }

        String librariesPath = libraryFolder.getAbsolutePath();
        if (System.getProperty("java.library.path") != null) {
            System.setProperty("java.library.path", librariesPath + System.getProperty("path.separator") + System.getProperty("java.library.path"));
        }
        else {
            System.setProperty("java.library.path", librariesPath);
        }

        try {
            Field fieldSysPaths = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPaths.setAccessible(true);
            fieldSysPaths.set(null, null);
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot clear java.library.path cache", e);
        }
    }
}
