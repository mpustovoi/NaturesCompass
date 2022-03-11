package com.chaosthedude.naturescompass.workers;

import com.chaosthedude.naturescompass.NaturesCompass;
import com.chaosthedude.naturescompass.config.NaturesCompassConfig;
import com.chaosthedude.naturescompass.items.NaturesCompassItem;
import com.chaosthedude.naturescompass.utils.BiomeUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class BiomeSearchWorker implements WorldWorkerManager.IWorker {

	public final int sampleSpace;
	public final int maxSamples;
	public final int maxRadius;
	public World world;
	public Biome biome;
	public Identifier biomeKey;
	public BlockPos startPos;
	public int samples;
	public int nextLength;
	public Direction direction;
	public ItemStack stack;
	public PlayerEntity player;
	public int x;
	public int y;
	public int z;
	public int length;
	public boolean finished;
	public int lastRadiusThreshold;

	public BiomeSearchWorker(World world, PlayerEntity player, ItemStack stack, Biome biome, BlockPos startPos) {
		this.world = world;
		this.player = player;
		this.stack = stack;
		this.biome = biome;
		this.startPos = startPos;
		x = startPos.getX();
		y = startPos.getY();
		z = startPos.getZ();
		sampleSpace = NaturesCompassConfig.sampleSpaceModifier * BiomeUtils.getBiomeSize(world);
		maxSamples = NaturesCompassConfig.maxSamples;
		maxRadius = NaturesCompassConfig.radiusModifier * BiomeUtils.getBiomeSize(world);
		nextLength = sampleSpace;
		length = 0;
		samples = 0;
		direction = Direction.UP;
		finished = false;
		biomeKey = BiomeUtils.getIdentifierForBiome(world, biome);
		lastRadiusThreshold = 0;
	}

	public void start() {
		if (!stack.isEmpty() && stack.getItem() == NaturesCompass.NATURES_COMPASS_ITEM) {
			if (maxRadius > 0 && sampleSpace > 0) {
				NaturesCompass.LOGGER.info("Starting search: " + sampleSpace + " sample space, " + maxSamples + " max samples, " + maxRadius + " max radius");
				WorldWorkerManager.addWorker(this);
			} else {
				finish(false);
			}
		}
	}

	@Override
	public boolean hasWork() {
		return !finished && getRadius() <= maxRadius && samples <= maxSamples;
	}

	@Override
	public boolean doWork() {
		if (hasWork()) {
			if (direction == Direction.NORTH) {
				z -= sampleSpace;
			} else if (direction == Direction.EAST) {
				x += sampleSpace;
			} else if (direction == Direction.SOUTH) {
				z += sampleSpace;
			} else if (direction == Direction.WEST) {
				x -= sampleSpace;
			}

			final BlockPos pos = new BlockPos(x, y, z);
			final Biome biomeAtPos = world.getBiomeAccess().getBiomeForNoiseGen(pos).value();
			final Identifier biomeAtPosID = BiomeUtils.getIdentifierForBiome(world, biomeAtPos);
			if (biomeAtPosID != null && biomeAtPosID.equals(biomeKey)) {
				finish(true);
				return false;
			}

			samples++;
			length += sampleSpace;
			if (length >= nextLength) {
				if (direction != Direction.UP) {
					nextLength += sampleSpace;
					direction = direction.rotateYClockwise();
				} else {
					direction = Direction.NORTH;
				}
				length = 0;
			}
			int radius = getRadius();
 			if (radius > 500 && radius / 500 > lastRadiusThreshold) {
 				if (!stack.isEmpty() && stack.getItem() == NaturesCompass.NATURES_COMPASS_ITEM) {
 					((NaturesCompassItem) stack.getItem()).setSearchRadius(stack, roundRadius(radius, 500), player);
 				}
 				lastRadiusThreshold = radius / 500;
 			}
		}
		if (hasWork()) {
			return true;
		}
		finish(false);
		return false;
	}

	private void finish(boolean found) {
		if (!stack.isEmpty() && stack.getItem() == NaturesCompass.NATURES_COMPASS_ITEM) {
			if (found) {
				NaturesCompass.LOGGER.info("Search succeeded: " + getRadius() + " radius, " + samples + " samples");
				((NaturesCompassItem) stack.getItem()).setFound(stack, x, z, samples, player);
				((NaturesCompassItem) stack.getItem()).setDisplayCoordinates(stack, NaturesCompassConfig.displayCoordinates);
			} else {
				NaturesCompass.LOGGER.info("Search failed: " + getRadius() + " radius, " + samples + " samples");
				((NaturesCompassItem) stack.getItem()).setNotFound(stack, player, roundRadius(getRadius(), 500), samples);
			}
		} else {
			NaturesCompass.LOGGER.error("Invalid compass after search");
		}
		finished = true;
	}

	private int getRadius() {
		return BiomeUtils.getDistanceToBiome(startPos, x, z);
	}
	
	private int roundRadius(int radius, int roundTo) {
 		return ((int) radius / roundTo) * roundTo;
 	}

}
