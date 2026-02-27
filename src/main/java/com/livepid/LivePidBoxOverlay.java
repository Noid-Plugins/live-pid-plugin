package com.livepid;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.TitleComponent;

class LivePidBoxOverlay extends OverlayPanel
{
	private static final String LABEL = "PID";
	private static final int BOX_SIZE = 36;
	private static final Color BOX_COLOR = new Color(33, 33, 33, 210);

	private final LivePidPlugin plugin;
	private final LivePidConfig config;

	@Inject
	LivePidBoxOverlay(LivePidPlugin plugin, LivePidConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.mode() != PidIndicatorMode.OVERLAY || !plugin.shouldShowIndicator())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setBackgroundColor(BOX_COLOR);
		panelComponent.setPreferredSize(new Dimension(BOX_SIZE, BOX_SIZE));
		panelComponent.setBorder(new Rectangle(6, 8, 6, 6));
		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text(LABEL)
				.color(plugin.getPidColor(plugin.getPidStatus()))
				.build()
		);
		return super.render(graphics);
	}
}
