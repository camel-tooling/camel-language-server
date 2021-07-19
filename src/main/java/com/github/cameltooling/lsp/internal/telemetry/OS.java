package com.github.cameltooling.lsp.internal.telemetry;

/**
 * OS information
 * 
 * Mostly duplicated from Eclipse Lemminx, the xml language server
 */
public class OS {

	private final String name;

	private final String version;

	private final String arch;

	private final transient boolean isWindows;

	public OS() {
		this.name = Platform.getSystemProperty("os.name");
		this.version = Platform.getSystemProperty("os.version");
		this.arch = Platform.getSystemProperty("os.arch");
		isWindows = name != null && name.toLowerCase().contains("win");
	}

	/**
	 * Returns the OS name.
	 *
	 * @return the OS name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the OS version.
	 *
	 * @return the OS version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the OS arch.
	 *
	 * @return the OS arch.
	 */
	public String getArch() {
		return arch;
	}

	/**
	 * Returns true if the operating system is Windows and false otherwise
	 *
	 * @return true if the operating system is Windows and false otherwise
	 */
	public boolean isWindows() {
		return isWindows;
	}
}
