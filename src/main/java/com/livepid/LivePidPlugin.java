package com.livepid;

import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Live Pid",
	description = "PID label next to your name using melee, ranged, and magic timing",
	tags = {"pvp", "pid", "timing", "combat"}
)
public class LivePidPlugin extends Plugin
{
	private static final int COMBAT_HIDE_TIMEOUT_TICKS = 25;
	private static final Color PID_ON_COLOR = new Color(67, 160, 71);
	private static final Color PID_OFF_COLOR = new Color(229, 57, 53);
	private static final Color PID_UNKNOWN_COLOR = new Color(255, 193, 7);

	@Inject
	private Client client;

	@Inject
	private LivePidConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LivePidIndicatorOverlay indicatorOverlay;

	@Inject
	private LivePidBoxOverlay boxOverlay;

	private PidDetector pidDetector;
	private volatile int lastOwnAttackAnimationTick = Integer.MIN_VALUE;

	@Override
	protected void startUp()
	{
		ensureEnumConfigValue("mode", PidIndicatorMode.class, PidIndicatorMode.OVERLAY);
		pidDetector = new PidDetector(client);
		lastOwnAttackAnimationTick = Integer.MIN_VALUE;
		overlayManager.add(indicatorOverlay);
		overlayManager.add(boxOverlay);
		log.debug("Live Pid started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(indicatorOverlay);
		overlayManager.remove(boxOverlay);
		if (pidDetector != null)
		{
			pidDetector.reset();
			pidDetector = null;
		}
		lastOwnAttackAnimationTick = Integer.MIN_VALUE;
		log.debug("Live Pid stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pidDetector == null)
		{
			return;
		}
		pidDetector.onGameTick();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (pidDetector == null)
		{
			return;
		}
		pidDetector.onAnimationChanged(event);

		Player local = client.getLocalPlayer();
		if (local != null && event.getActor() == local && AttackAnimationBuckets.getBucket(local.getAnimation()) != null)
		{
			markOwnAttackAnimation();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (pidDetector == null)
		{
			return;
		}
		pidDetector.onHitsplatApplied(event);
	}

	PidDetector getPidDetector()
	{
		return pidDetector;
	}

	Client getClient()
	{
		return client;
	}

	PidStatus getPidStatus()
	{
		return pidDetector == null ? PidStatus.UNKNOWN : pidDetector.getCurrentPidStatus();
	}

	Color getPidColor(PidStatus status)
	{
		switch (status)
		{
			case ON_PID:
				return PID_ON_COLOR;
			case OFF_PID:
				return PID_OFF_COLOR;
			default:
				return PID_UNKNOWN_COLOR;
		}
	}

	boolean shouldShowIndicator()
	{
		if (!config.hideWhenOutOfCombat())
		{
			return true;
		}
		if (lastOwnAttackAnimationTick == Integer.MIN_VALUE)
		{
			return false;
		}
		return client.getTickCount() - lastOwnAttackAnimationTick <= COMBAT_HIDE_TIMEOUT_TICKS;
	}

	private void markOwnAttackAnimation()
	{
		lastOwnAttackAnimationTick = client.getTickCount();
	}

	private <E extends Enum<E>> void ensureEnumConfigValue(String keyName, Class<E> enumType, E fallback)
	{
		String configured = configManager.getConfiguration(LivePidConfig.GROUP, keyName);
		if (configured == null || configured.trim().isEmpty())
		{
			configManager.setConfiguration(LivePidConfig.GROUP, keyName, fallback.name());
			return;
		}

		try
		{
			Enum.valueOf(enumType, configured);
		}
		catch (IllegalArgumentException ex)
		{
			configManager.setConfiguration(LivePidConfig.GROUP, keyName, fallback.name());
		}
	}

	@Provides
	LivePidConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LivePidConfig.class);
	}
}
