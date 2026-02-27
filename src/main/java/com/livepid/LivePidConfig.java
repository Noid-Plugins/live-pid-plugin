package com.livepid;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(LivePidConfig.GROUP)
public interface LivePidConfig extends Config
{
	String GROUP = "livepid";

	@ConfigItem(
		keyName = "mode",
		name = "Display Mode",
		description = "Show PID as a square overlay box or above your head"
	)
	default PidIndicatorMode mode()
	{
		return PidIndicatorMode.OVERLAY;
	}

	@Range(min = 10, max = 32)
	@ConfigItem(
		keyName = "textSize",
		name = "Text Size",
		description = "Size of the PID label text"
	)
	default int textSize()
	{
		return 14;
	}

	@ConfigItem(
		keyName = "hideWhenOutOfCombat",
		name = "Hide Out Of Combat",
		description = "Hide PID after 25 ticks without detecting your own attack animation"
	)
	default boolean hideWhenOutOfCombat()
	{
		return true;
	}
}
