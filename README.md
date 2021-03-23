# Zomboid Plugin Loader

Zomboid Plugin Loader is a Project Zomboid modding tool that allows users to load Java plugins that can alter game code at runtime.

## For developers

- Go to project structure and set project SDK to version 1.8.

- Create `local.properties` file in project root directory and declare the following properties:

	```properties
	gameDir=<absolute_path_to_game_dir>
	jdkDir=<absolute_path_to_jdk_8_dir>
	```

- Run `generateLaunchRunConfigs` to generate run configurations.

- Run `shadowJar` to generate the **agent** jar used by the loader.