package com.livepid;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

final class AttackAnimationBuckets
{
	enum Bucket
	{
		MELEE,
		RANGED_STANDARD,
		RANGED_THROWN,
		RANGED_BALLISTA,
		MAGIC
	}

	private static final Map<Integer, Bucket> BUCKETS = new HashMap<>();

	static
	{
		registerMelee(
			245, 376, 381, 386, 390, 393, 395, 400, 401, 406, 407, 414, 419, 422, 423, 428, 429, 440,
			923, 1058, 1060, 1062, 1132, 1203, 1378, 1658, 1665, 1667, 1710, 1711, 1872, 2062, 2066, 2067,
			2068, 2078, 2080, 2081, 2082, 2661, 2890, 3157, 3297, 3298, 3299, 3852, 4198, 4503, 5247, 5865,
			5870, 6118, 6147, 7004, 7045, 7054, 7055, 7511, 7514, 7515, 7516, 7638, 7639, 7640, 7641, 7642,
			7643, 7644, 7645, 8010, 8056, 8145, 8288, 8289, 8290, 9171, 9471, 9544, 9963, 10172, 10173, 10989,
			11124
		);

		registerRangedStandard(
			426, 1074, 2075, 4230, 7552, 7557, 9166, 9168, 9206, 9858, 9964, 10914, 10923
		);

		registerRangedThrown(
			929, 1068, 5061, 7521, 7554, 7617, 7618, 8194, 8195, 8291, 8292, 10656, 11057, 11060
		);

		registerRangedBallista(7218, 7555, 7556);

		registerMagic(
			708, 710, 724, 811, 1161, 1163, 1164, 1165, 1166, 1168, 1169, 1576, 8532, 8972, 8977, 9144,
			9145, 9493, 9961, 10091, 10092, 10501, 11423, 11429, 11430, 12394,
			711, 727, 1162, 1167, 1978, 1979
		);
	}

	private AttackAnimationBuckets()
	{
	}

	@Nullable
	static Bucket getBucket(int animationId)
	{
		return BUCKETS.get(animationId);
	}

	private static void registerMelee(int... animationIds)
	{
		register(Bucket.MELEE, animationIds);
	}

	private static void registerRangedStandard(int... animationIds)
	{
		register(Bucket.RANGED_STANDARD, animationIds);
	}

	private static void registerRangedThrown(int... animationIds)
	{
		register(Bucket.RANGED_THROWN, animationIds);
	}

	private static void registerRangedBallista(int... animationIds)
	{
		register(Bucket.RANGED_BALLISTA, animationIds);
	}

	private static void registerMagic(int... animationIds)
	{
		register(Bucket.MAGIC, animationIds);
	}

	private static void register(Bucket bucket, int... animationIds)
	{
		for (int animationId : animationIds)
		{
			BUCKETS.put(animationId, bucket);
		}
	}
}
