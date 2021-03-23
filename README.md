# Zomboid Plugin Loader

Zomboid Plugin Loader is a Project Zomboid modding tool that allows users to load Java plugins that can alter game code at runtime.

## For developers

- Create `local.properties` file in project root directory and declare the following properties:

	```properties
	gameDir=<absolute_path_to_game_dir>
	jdkDir=<absolute_path_to_jdk_8_dir>
	```

- Run `generateLaunchRunConfigs` to generate run configurations.

