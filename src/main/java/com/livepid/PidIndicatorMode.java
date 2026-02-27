package com.livepid;

public enum PidIndicatorMode
{
	OVERLAY("Overlay"),
	ABOVE_HEAD("Above head");

	private final String label;

	PidIndicatorMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
