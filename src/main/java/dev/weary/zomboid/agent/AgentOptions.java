package dev.weary.zomboid.agent;

import java.io.File;

import dev.weary.zomboid.util.Result;
import dev.weary.zomboid.util.Util;

class AgentOptions {
	private AgentOptions() {}

	File thisJar;
	File pluginsDir;

	static Result<AgentOptions, String> fromArgs(String argString) {
		AgentOptions agentOptions = new AgentOptions();
		String[] argList = argString.split(" ");

		// TODO: Better argument parsing
		String pathToThisJar = Util.getArgAfterLast(argList, "--this-jar");
		if (pathToThisJar == null) {
			return Result.ofError("Argument --this-jar <path> is required");
		}

		agentOptions.thisJar = new File(pathToThisJar);
		if (!agentOptions.thisJar.exists()) {
			return Result.ofError("File at --this-jar (" + agentOptions.thisJar.getPath() + ") does not exist");
		}

		if (!agentOptions.thisJar.isFile()) {
			return Result.ofError("File at --this-jar (" + agentOptions.thisJar.getPath() + ") is not a file");
		}

		// TODO: Parse jar file?

		String pathToPluginsDir = Util.getArgAfterLast(argList, "--plugins-dir");
		if (pathToPluginsDir == null) {
			return Result.ofError("Argument --plugins-folder <path> is required");
		}

		agentOptions.pluginsDir = new File(pathToPluginsDir);
		if (!agentOptions.pluginsDir.exists()) {
			return Result.ofError("Directory at --plugins-dir (" + agentOptions.pluginsDir.getPath() + ") does " +
					"not exist");
		}

		if (!agentOptions.pluginsDir.isDirectory()) {
			return Result.ofError("Directory at --plugins-dir (" + agentOptions.pluginsDir.getPath() + ") is not a " +
					"directory");
		}

		return Result.ofValue(agentOptions);
	}
}
