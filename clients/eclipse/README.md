How to debug Camel Language Server from Eclipse client
======================================================

1. Add debug arguments to the list org.apache.camel.lsp.eclipse.client.CamelLSPStreamConnectionProvider.computeCommands()

You will end up with something like:

	private static List<String> computeCommands() {
		List<String> commands = new ArrayList<>();
		commands.add("java");
		commands.addAll(debugArguments());
		commands.add("-jar");
		commands.add(computeCamelLanguageServerJarPath());
		return commands;
	}

	private static List<String> debugArguments() {
		return Arrays.asList("-Xdebug","-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=3000");
	}
	
	
2. Create a Remote Java Application Debug Launch configuration in Eclipse