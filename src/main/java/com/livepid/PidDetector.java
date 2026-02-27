package com.livepid;

import java.util.Locale;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;

/**
 * PID detector based on known attack animation buckets and cast-to-hit timing.
 * Uses a strict single-attempt model: each new attack replaces the previous attempt.
 */
public class PidDetector
{
	private static final int MAX_SAMPLE_AGE_TICKS = 16;
	private static final int MAX_HIT_DELAY_TICKS = 12;
	private static final int EARLY_HITSPLAT_TOLERANCE_TICKS = 1;
	private static final int RECOIL_MIN_DAMAGE = 1;
	private static final int RECOIL_MAX_DAMAGE = 5;
	private static final String RING_OF_RECOIL_NAME = "ring of recoil";
	private static final String RING_OF_SUFFERING_PREFIX = "ring of suffering";

	private final Client client;

	private Player currentTarget;
	private volatile PidStatus currentPidStatus = PidStatus.UNKNOWN;

	private AttackSample pendingAttackSample;
	private OutgoingHitsplatSample pendingHitsplatSample;

	PidDetector(Client client)
	{
		this.client = client;
	}

	public void onGameTick()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			softReset();
			return;
		}

		Actor interacting = localPlayer.getInteracting();
		if (interacting instanceof Player)
		{
			currentTarget = (Player) interacting;
		}
		else if (!isSamePlayer(currentTarget, interacting) && !isValidTarget(currentTarget))
		{
			currentTarget = null;
		}

		int tick = client.getTickCount();
		if (pendingAttackSample != null && tick - pendingAttackSample.attackTick > MAX_SAMPLE_AGE_TICKS)
		{
			pendingAttackSample = null;
		}
		if (pendingHitsplatSample != null && tick - pendingHitsplatSample.hitTick > MAX_SAMPLE_AGE_TICKS)
		{
			pendingHitsplatSample = null;
		}
	}

	public void onAnimationChanged(AnimationChanged event)
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || event.getActor() != localPlayer)
		{
			return;
		}

		AttackAnimationBuckets.Bucket bucket = AttackAnimationBuckets.getBucket(localPlayer.getAnimation());
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

		String targetName = target.getName();
		if (targetName == null)
		{
			return;
		}

		int currentTick = client.getTickCount();
		currentTarget = target;
		AttackSample sample = new AttackSample(currentTick, targetName, distance, bucket);

		if (matchesPendingHitsplatSample(sample))
		{
			resolveSample(sample, pendingHitsplatSample.hitTick);
			pendingHitsplatSample = null;
			pendingAttackSample = null;
			return;
		}

		// New swing starts a fresh attempt. If this one never matches, keep last PID.
		pendingAttackSample = sample;
	}

	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!(event.getActor() instanceof Player))
		{
			return;
		}

		Player victim = (Player) event.getActor();
		String victimName = victim.getName();
		if (victimName == null)
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat == null)
		{
			return;
		}

		if (shouldIgnoreRecoilHitsplat(hitsplat))
		{
			return;
		}

		if (!isLocalOutgoingHitsplat(victim, hitsplat))
		{
			return;
		}

		int hitTick = client.getTickCount();
		if (matchesPendingAttackSample(victimName, hitTick))
		{
			resolveSample(pendingAttackSample, hitTick);
			pendingAttackSample = null;
			pendingHitsplatSample = null;
			return;
		}

		// Keep only the latest unmatched hitsplat for hitsplat-first order.
		pendingHitsplatSample = new OutgoingHitsplatSample(hitTick, victimName);
	}

	public void reset()
	{
		currentTarget = null;
		currentPidStatus = PidStatus.UNKNOWN;
		pendingAttackSample = null;
		pendingHitsplatSample = null;
	}

	private void softReset()
	{
		currentTarget = null;
		pendingAttackSample = null;
		pendingHitsplatSample = null;
	}

	public PidStatus getCurrentPidStatus()
	{
		return currentPidStatus;
	}

	private boolean matchesPendingAttackSample(String victimName, int hitTick)
	{
		if (pendingAttackSample == null)
		{
			return false;
		}

		int age = hitTick - pendingAttackSample.attackTick;
		if (age < 0 || age > MAX_HIT_DELAY_TICKS)
		{
			return false;
		}

		return pendingAttackSample.victimName.equalsIgnoreCase(victimName);
	}

	private boolean matchesPendingHitsplatSample(AttackSample attackSample)
	{
		if (pendingHitsplatSample == null)
		{
			return false;
		}

		if (!pendingHitsplatSample.victimName.equalsIgnoreCase(attackSample.victimName))
		{
			return false;
		}

		int rawTicksFromCastToHit = pendingHitsplatSample.hitTick - attackSample.attackTick;
		return rawTicksFromCastToHit >= -EARLY_HITSPLAT_TOLERANCE_TICKS && rawTicksFromCastToHit <= MAX_HIT_DELAY_TICKS;
	}

	private void resolveSample(AttackSample sample, int hitTick)
	{
		int rawTicksFromCastToHit = hitTick - sample.attackTick;
		int ticksFromCastToHit = normalizeTicksFromCastToHit(rawTicksFromCastToHit);
		if (ticksFromCastToHit < 0)
		{
			return;
		}

		PidStatus analyzedStatus = analyzeAttack(sample.bucket, sample.distance, ticksFromCastToHit);
		if (analyzedStatus != PidStatus.UNKNOWN)
		{
			currentPidStatus = analyzedStatus;
		}
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
				if (distance == 3 || distance == 4)
				{
					return 1;
				}
				return 1 + (int) Math.floor((3.0 + distance) / 6.0);
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

	private boolean isLocalOutgoingHitsplat(Player victim, Hitsplat hitsplat)
	{
		if (hitsplat.isMine())
		{
			return true;
		}

		int type = hitsplat.getHitsplatType();
		if (type == HitsplatID.DAMAGE_ME || type == HitsplatID.BLOCK_ME)
		{
			return true;
		}

		if (type != HitsplatID.DAMAGE_OTHER && type != HitsplatID.BLOCK_OTHER)
		{
			return false;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || victim == null)
		{
			return false;
		}

		Actor localInteracting = localPlayer.getInteracting();
		Actor victimInteracting = victim.getInteracting();
		return localInteracting == victim || victimInteracting == localPlayer;
	}

	private boolean shouldIgnoreRecoilHitsplat(Hitsplat hitsplat)
	{
		int amount = hitsplat.getAmount();
		if (amount < RECOIL_MIN_DAMAGE || amount > RECOIL_MAX_DAMAGE)
		{
			return false;
		}
		return isRecoilOrSufferingEquipped();
	}

	private boolean isRecoilOrSufferingEquipped()
	{
		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipmentContainer == null)
		{
			return false;
		}

		Item[] equipment = equipmentContainer.getItems();
		int ringSlot = EquipmentInventorySlot.RING.getSlotIdx();
		if (equipment == null || ringSlot < 0 || ringSlot >= equipment.length)
		{
			return false;
		}

		Item ringItem = equipment[ringSlot];
		if (ringItem == null)
		{
			return false;
		}

		return isRecoilOrSufferingRing(ringItem);
	}

	private boolean isRecoilOrSufferingRing(Item ringItem)
	{
		ItemComposition itemDefinition = client.getItemDefinition(ringItem.getId());
		if (itemDefinition == null || itemDefinition.getName() == null)
		{
			return false;
		}

		String normalizedName = itemDefinition.getName().toLowerCase(Locale.ROOT);
		return normalizedName.equals(RING_OF_RECOIL_NAME) || normalizedName.startsWith(RING_OF_SUFFERING_PREFIX);
	}

	private static int normalizeTicksFromCastToHit(int rawTicksFromCastToHit)
	{
		if (rawTicksFromCastToHit >= 0)
		{
			return rawTicksFromCastToHit;
		}
		if (rawTicksFromCastToHit >= -EARLY_HITSPLAT_TOLERANCE_TICKS)
		{
			return 0;
		}
		return -1;
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

	private static boolean isValidTarget(Player player)
	{
		return player != null && player.getName() != null && !player.isDead();
	}

	private static boolean isSamePlayer(Player a, Actor b)
	{
		return a != null && b instanceof Player && a == b;
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

	private static final class OutgoingHitsplatSample
	{
		private final int hitTick;
		private final String victimName;

		private OutgoingHitsplatSample(int hitTick, String victimName)
		{
			this.hitTick = hitTick;
			this.victimName = victimName;
		}
	}
}
