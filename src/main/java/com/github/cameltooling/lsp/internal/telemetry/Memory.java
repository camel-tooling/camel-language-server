package com.github.cameltooling.lsp.internal.telemetry;

/**
 * JVM memory information
 *
 * Mostly duplicated from Eclipse Lemminx, the xml language server
 */
public class Memory {

	private final long free;

	private final long total;

	private final long max;

	Memory() {
		super();
		this.free = Runtime.getRuntime().freeMemory();
		this.total = Runtime.getRuntime().totalMemory();
		this.max = Runtime.getRuntime().maxMemory();
	}

	public long getFree() {
		return free;
	}

	public long getTotal() {
		return total;
	}

	public long getMax() {
		return max;
	}

}
