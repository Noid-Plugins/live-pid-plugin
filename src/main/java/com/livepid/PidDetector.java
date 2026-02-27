package com.livepid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;

/**
 * PID detector based on attack animation bucket + cast-to-hit timing.
 * Supports melee, ranged, and magic timing profiles.
 */
public class PidDetector
{
	private static final int MAX_PENDING_SAMPLES = 24;
	private static final int MAX_SAMPLE_AGE_TICKS = 16;
	private static final int MAX_HIT_DELAY_TICKS = 12;

	private final Client client;
	private final Deque<AttackSample> pendingSamples = new ArrayDeque<>();

	private Player currentTarget;
	private volatile PidStatus currentPidStatus = PidStatus.UNKNOWN;

	PidDetector(Client client)
	{
		this.client = client;
	}

	public void onGameTick()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			reset();
			return;
		}

		Actor interacting = localPlayer.getInteracting();
		if (interacting instanceof Player)
		{
			currentTarget = (Player) interacting;
		}
		else if (!isSamePlayer(currentTarget, localPlayer.getInteracting()))
		{
			if (!isValidTarget(currentTarget))
			{
				currentTarget = null;
				currentPidStatus = PidStatus.UNKNOWN;
			}
		}

		pruneExpiredSamples(client.getTickCount());
		if (currentTarget == null && pendingSamples.isEmpty())
		{
			currentPidStatus = PidStatus.UNKNOWN;
		}
	}

	public void onAnimationChanged(AnimationChanged event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || event.getActor() != localPlayer)
		{
			return;
		}

		int animationId = localPlayer.getAnimation();
		AttackAnimationBuckets.Bucket bucket = AttackAnimationBuckets.getBucket(animationId);
		if (bucket == null)
		{
			return;
		}

		Player target = resolveTarget(localPlayer);
		if (target == null)
		{
			return;
		}

		int distance = calculateDistance(localPlayer, target);
		if (distance < 0)
		{
			return;
		}

		currentTarget = target;
		if (pendingSamples.size() >= MAX_PENDING_SAMPLES)
		{
			pendingSamples.removeFirst();
		}

		pendingSamples.addLast(new AttackSample(
			client.getTickCount(),
			target.getName(),
			distance,
			bucket
		));
	}

	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (pendingSamples.isEmpty() || !(event.getActor() instanceof Player))
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat == null || !isLocalOutgoingHitsplat(hitsplat))
		{
			return;
		}

		Player victim = (Player) event.getActor();
		String victimName = victim.getName();
		if (victimName == null)
		{
			return;
		}

		int hitTick = client.getTickCount();
		AttackSample sample = findMatchingSample(victimName, hitTick);
		if (sample == null)
		{
			return;
		}

		int ticksFromCastToHit = hitTick - sample.attackTick;
		currentPidStatus = analyzeAttack(sample.bucket, sample.distance, ticksFromCastToHit);
	}

	private PidStatus analyzeAttack(AttackAnimationBuckets.Bucket bucket, int distance, int ticksFromCastToHit)
	{
		if (ticksFromCastToHit < 0 || distance < 0)
		{
			return PidStatus.UNKNOWN;
		}

		int expectedOnPid = expectedOnPidDelay(bucket, distance);
		if (ticksFromCastToHit == expectedOnPid)
		{
			return PidStatus.ON_PID;
		}

		if (ticksFromCastToHit == expectedOnPid + 1)
		{
			return PidStatus.OFF_PID;
		}
		return PidStatus.UNKNOWN;
	}

	public void reset()
	{
		currentTarget = null;
		currentPidStatus = PidStatus.UNKNOWN;
		pendingSamples.clear();
	}

	private AttackSample findMatchingSample(String victimName, int hitTick)
	{
		Iterator<AttackSample> iterator = pendingSamples.iterator();
		while (iterator.hasNext())
		{
			AttackSample sample = iterator.next();
			int age = hitTick - sample.attackTick;
			if (age > MAX_SAMPLE_AGE_TICKS)
			{
				iterator.remove();
				continue;
			}

			if (age < 0 || age > MAX_HIT_DELAY_TICKS)
			{
				continue;
			}

			if (!sample.victimName.equalsIgnoreCase(victimName))
			{
				continue;
			}

			iterator.remove();
			return sample;
		}
		return null;
	}

	private int expectedOnPidDelay(AttackAnimationBuckets.Bucket bucket, int distance)
	{
		switch (bucket)
		{
			case MELEE:
				return 0;
			case MAGIC:
				return 1 + (int) Math.floor((1.0 + distance) / 3.0);
			case RANGED_THROWN:
				return 1 + (int) Math.floor(distance / 6.0);
			case RANGED_BALLISTA:
				return distance <= 4 ? 2 : 3;
			case RANGED_STANDARD:
			default:
				return 1 + (int) Math.floor((3.0 + distance) / 6.0);
		}
	}

	private Player resolveTarget(Player localPlayer)
	{
		Actor interacting = localPlayer.getInteracting();
		if (interacting instanceof Player)
		{
			return (Player) interacting;
		}
		if (isValidTarget(currentTarget))
		{
			return currentTarget;
		}
		return null;
	}

	private boolean isValidTarget(Player player)
	{
		return player != null && player.getName() != null && !player.isDead();
	}

	private void pruneExpiredSamples(int currentTick)
	{
		Iterator<AttackSample> iterator = pendingSamples.iterator();
		while (iterator.hasNext())
		{
			AttackSample sample = iterator.next();
			if (currentTick - sample.attackTick > MAX_SAMPLE_AGE_TICKS)
			{
				iterator.remove();
			}
		}
	}

	private static int calculateDistance(Player attacker, Player target)
	{
		if (attacker == null || target == null || attacker.getWorldLocation() == null || target.getWorldLocation() == null)
		{
			return -1;
		}
		int dx = Math.abs(attacker.getWorldLocation().getX() - target.getWorldLocation().getX());
		int dy = Math.abs(attacker.getWorldLocation().getY() - target.getWorldLocation().getY());
		return Math.max(dx, dy);
	}

	private static boolean isLocalOutgoingHitsplat(Hitsplat hitsplat)
	{
		if (hitsplat.isMine())
		{
			return true;
		}
		int type = hitsplat.getHitsplatType();
		return type == HitsplatID.DAMAGE_ME || type == HitsplatID.BLOCK_ME;
	}

	private static boolean isSamePlayer(Player a, Actor b)
	{
		return a != null && b instanceof Player && a == b;
	}

	public PidStatus getCurrentPidStatus()
	{
		return currentPidStatus;
	}

	private static final class AttackSample
	{
		private final int attackTick;
		private final String victimName;
		private final int distance;
		private final AttackAnimationBuckets.Bucket bucket;

		private AttackSample(int attackTick, String victimName, int distance, AttackAnimationBuckets.Bucket bucket)
		{
			this.attackTick = attackTick;
			this.victimName = victimName;
			this.distance = distance;
			this.bucket = bucket;
		}
	}
}
