package com.livepid;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class LivePidIndicatorOverlay extends Overlay
{
	private static final String LABEL = "PID";

	private final LivePidPlugin plugin;
	private final LivePidConfig config;

	@Inject
	LivePidIndicatorOverlay(LivePidPlugin plugin, LivePidConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.mode() != PidIndicatorMode.ABOVE_HEAD || !plugin.shouldShowIndicator())
		{
			return null;
		}

		Player local = plugin.getClient().getLocalPlayer();
		if (local == null)
		{
			return null;
		}

		Point basePoint = local.getCanvasTextLocation(graphics, "", local.getLogicalHeight() + 80);
		if (basePoint == null)
		{
			return null;
		}

		Font originalFont = graphics.getFont();
		Font sizedFont = originalFont.deriveFont((float) config.textSize());
		graphics.setFont(sizedFont);

		FontMetrics fm = graphics.getFontMetrics();
		int labelWidth = fm.stringWidth(LABEL);
		int labelHeight = fm.getAscent();

		int x = basePoint.getX() - labelWidth / 2;
		int y = basePoint.getY() - labelHeight + 2;

		Color textColor = plugin.getPidColor(plugin.getPidStatus());
		drawOutlinedText(graphics, LABEL, x, y, textColor);
		graphics.setFont(originalFont);
		return null;
	}

	private static void drawOutlinedText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}
}
