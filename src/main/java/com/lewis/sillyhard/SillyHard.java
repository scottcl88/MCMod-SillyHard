package com.lewis.sillyhard;

import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.NetherFortressFeature;
import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("sillyhard")
public class SillyHard {
	// Directly reference a log4j logger.
	private static final Logger LOGGER = LogManager.getLogger();

	public SillyHard() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::checkSpawn);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::entityJoinWorld);
		
		MinecraftForge.EVENT_BUS.addListener(this::biomeModification);

		MinecraftForge.EVENT_BUS.addListener(this::entityModifier);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void biomeModification(final BiomeLoadingEvent event) {
		Supplier<ConfiguredStructureFeature<?, ?>> strongholdFeature = null;
		Supplier<ConfiguredStructureFeature<?, ?>> fortressFeature = null;
		List<Supplier<ConfiguredStructureFeature<?, ?>>> structList = event.getGeneration().getStructures();
		for (int i = 0; i < structList.size(); i++) {
			String featureName = structList.get(i).get().feature.getFeatureName();
			if (featureName.equals(StrongholdFeature.STRONGHOLD.getFeatureName())) {
				strongholdFeature = structList.get(i);
			}
			if (featureName.equals(NetherFortressFeature.NETHER_BRIDGE.getFeatureName())) {
				fortressFeature = structList.get(i);
			}
		}
		event.getGeneration().getStructures().clear();
		if (strongholdFeature != null) {
			event.getGeneration().getStructures().add(strongholdFeature);
		}
		if (fortressFeature != null) {
			event.getGeneration().getStructures().add(fortressFeature);
		}

		List<SpawnerData> spawns = event.getSpawns().getSpawner(MobCategory.MONSTER);
		int endermanWeight = 0;
		int zombieWeight = 0;
		int skeletonWeight = 0;
		for (int i = 0; i < spawns.size(); i++) {
			if(spawns.get(i).type == EntityType.ENDERMAN) {
				endermanWeight = Math.round((float)(spawns.get(i).getWeight().asInt() * 1.5));
				LOGGER.info("biomeModification endermanWeight: "+endermanWeight);
			}
			if(spawns.get(i).type == EntityType.ZOMBIE) {
				zombieWeight = spawns.get(i).getWeight().asInt() * 4;
//				LOGGER.info("biomeModification 2: "+spawns.get(i).getWeight()+" | "+spawns.get(i).maxCount+" | "+spawns.get(i).minCount);
			}
			if(spawns.get(i).type == EntityType.SKELETON) {
				skeletonWeight = spawns.get(i).getWeight().asInt() * 2;
//				LOGGER.info("biomeModification 4: "+spawns.get(i).getWeight()+" | "+spawns.get(i).maxCount+" | "+spawns.get(i).minCount);
			}
		}
		// Remove existing spawn information		
		spawns.removeIf(e -> e.type == EntityType.ENDERMAN);
		spawns.removeIf(e -> e.type == EntityType.ZOMBIE);
		spawns.removeIf(e -> e.type == EntityType.SKELETON);

		// Make spawns more frequent in all biomes
		spawns.add(new SpawnerData(EntityType.ENDERMAN, endermanWeight, 1, 4));
		spawns.add(new SpawnerData(EntityType.ZOMBIE, zombieWeight, 1, 4));
		spawns.add(new SpawnerData(EntityType.SKELETON, skeletonWeight, 1, 4));
	}

	public void entityModifier(final EntityMountEvent event) {
		Entity entity = event.getEntityMounting();
		if (!(entity instanceof Player)) {
			event.setResult(Result.DENY);
			if (event.isCancelable()) {
				event.setCanceled(true);
			}
			return;
		}
	}

	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		LOGGER.info("HELLO FROM PREINIT");
		LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
	}

	private void checkSpawn(CheckSpawn event) {
//    	LOGGER.info("checkSpawn checking...."+event.getSpawnReason());
		Entity entity = event.getEntity();
		ResourceLocation entityResource = entity.getType().getRegistryName();
		String path = entityResource.getPath();
		if (path.equals("villager") || path.equals("wandering_trader") || path.equals("trader_llama")) {
//        	LOGGER.info("checkSpawn denied entity!!");
			event.getEntity().kill();
			event.getEntity().remove(RemovalReason.DISCARDED);
			event.setResult(Result.DENY);
			return;
		}
	}

	private void entityJoinWorld(EntityJoinWorldEvent event) {
		Entity entity = event.getEntity();
		ResourceLocation entityResource = entity.getType().getRegistryName();
		String path = entityResource.getPath();
//    	LOGGER.info("entityJoinWorld checking: "+path);
		if (path.equals("villager") || path.equals("wandering_trader") || path.equals("trader_llama")) {
//        	LOGGER.info("entityJoinWorld denied entity!!");
			event.getEntity().kill();
			event.getEntity().remove(RemovalReason.DISCARDED);
			event.setCanceled(true);
			LOGGER.info("Forcefully removed {}.", event.getEntity());
			return;
		}
	}
}
