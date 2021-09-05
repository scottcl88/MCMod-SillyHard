package com.example.examplemod;


import net.minecraft.data.loot.PiglinBarterLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import net.minecraft.world.level.levelgen.feature.VillageFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("examplemod")
public class ExampleMod
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public ExampleMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
//        // Register the enqueueIMC method for modloading
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
//        // Register the processIMC method for modloading
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
//
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::checkSpawn);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::entityJoinWorld);
//
        MinecraftForge.EVENT_BUS.addListener(this::addDimensionalSpacing);
        MinecraftForge.EVENT_BUS.addListener(this::biomeModification);
        MinecraftForge.EVENT_BUS.addListener(this::lootModifier);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onStructureSpawnListGatherEvent);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }    
    public void biomeModification(final BiomeLoadingEvent event) {
        /*
         * Add our structure to all biomes including other modded biomes.
         * You can skip or add only to certain biomes based on stuff like biome category,
         * temperature, scale, precipitation, mod id, etc. All kinds of options!
         *
         * You can even use the BiomeDictionary as well! To use BiomeDictionary, do
         * RegistryKey.getOrCreateKey(Registry.BIOME_KEY, event.getName()) to get the biome's
         * registrykey. Then that can be fed into the dictionary to get the biome's types.
         */
//        LOGGER.info("biomeModification 1: "+event.getName());
        int strongholdIndex = -1;
        Supplier<ConfiguredStructureFeature<?,?>> strongholdFeature = null;
        List<Supplier<ConfiguredStructureFeature<?,?>>> structList =  event.getGeneration().getStructures();
        for(int i = 0; i < structList.size(); i ++) {
            String featureName = structList.get(i).get().feature.getFeatureName();
//            LOGGER.info("biomeModification 2: "+featureName);
//            LOGGER.info("biomeModification 3: "+StrongholdFeature.STRONGHOLD.getFeatureName());
            if(featureName.equals(StrongholdFeature.STRONGHOLD.getFeatureName())) {
            	strongholdFeature = structList.get(i);
//                LOGGER.info("biomeModification 5: "+strongholdFeature.get().config);
//            	strongholdIndex = i;
//                event.getGeneration().getStructures().remove(i);
            }
        }
        if(strongholdFeature != null) {
            event.getGeneration().getStructures().clear();
            event.getGeneration().getStructures().add(strongholdFeature);
        }
        
        List<SpawnerData> spawns = event.getSpawns().getSpawner(MobCategory.MONSTER);

        // Remove existing spawn information
        spawns.removeIf(e -> e.type == EntityType.ENDERMAN);
        spawns.removeIf(e -> e.type == EntityType.ZOMBIE);

        // Make spawns more frequent in all biomes
        spawns.add(new SpawnerData(EntityType.ENDERMAN, 200, 1, 4));
        spawns.add(new SpawnerData(EntityType.ZOMBIE, 400, 1, 4));
    }

    public void entityModifier(final LivingUpdateEvent event) {
//    	event.getEntityLiving().startAutoSpinAttack(0);
   }	

    public void lootModifier(final LootTableLoadEvent event) {
    	LootTable table = event.getTable();
//    	LOGGER.info("lootModifier 1: "+event.getTable().getLootTableId());
        String tableId = event.getTable().getLootTableId().getPath();
//    	LOGGER.info("lootModifier 2: "+tableId);
        if(tableId.equals("chests/woodland_mansion")) {
        	LOGGER.info("lootModifier 3: "+table.getParamSet());
        	LootTables manager = event.getLootTableManager();
        }
   }
    public void addDimensionalSpacing(final WorldEvent.Load event) {

//        LOGGER.info("addDimensionalSpacing: "+event.getWorld());
//        LOGGER.info("addDimensionalSpacing feature:  "+StrongholdFeature.STRONGHOLD.getRegistryName());        
//        ServerLevel level = (ServerLevel)event.getWorld();
//        LOGGER.info("addDimensionalSpacing level:  "+level);        
//        LOGGER.info("addDimensionalSpacing level getStructureManager:  "+level.getStructureManager());        
//        StructureTemplate temp = level.getStructureManager().get(StrongholdFeature.STRONGHOLD.getRegistryName()).get();
//        LOGGER.info("temp: "+temp);
//        level.getStructureManager().remove(new ResourceLocation(""));
        
//        if(serverWorld.getChunkSource().getGenerator() instanceof FlatChunkGenerator &&
//                serverWorld.dimension().equals(World.OVERWORLD)){
//                return;
//            }
   }
    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

//    private void enqueueIMC(final InterModEnqueueEvent event)
//    {
//        // some example code to dispatch IMC to another mod
//        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
//    }
//
//    private void processIMC(final InterModProcessEvent event)
//    {
//        // some example code to receive and process InterModComms from other mods
//        LOGGER.info("Got IMC {}", event.getIMCStream().
//                map(m->m.messageSupplier().get()).
//                collect(Collectors.toList()));
//    }
    private void checkSpawn (CheckSpawn event) {
    	LOGGER.info("checkSpawn checking...."+event.getSpawnReason());
    	Entity entity = event.getEntity();
    	ResourceLocation entityResource = entity.getType().getRegistryName();
    	String path = entityResource.getPath();
        if(path.equals("villager") || path.equals("wandering_trader") || path.equals("trader_llama")) {
//        	LOGGER.info("checkSpawn denied entity!!");
            event.getEntity().kill();
        	event.getEntity().remove(RemovalReason.DISCARDED);
            event.setResult(Result.DENY);
            return;
        }          
    }
    private void entityJoinWorld (EntityJoinWorldEvent event) {
    	Entity entity = event.getEntity();
    	ResourceLocation entityResource = entity.getType().getRegistryName();
    	String path = entityResource.getPath();
    	LOGGER.info("entityJoinWorld checking: "+path);
        if(path.equals("villager") || path.equals("wandering_trader") || path.equals("trader_llama")) {
//        	LOGGER.info("entityJoinWorld denied entity!!");
            event.getEntity().kill();
            event.getEntity().remove(RemovalReason.DISCARDED);
            event.setCanceled(true);
            LOGGER.info("Forcefully removed {}.", event.getEntity());
            return;
        }         
    }
    public void onStructureSpawnListGatherEvent(StructureSpawnListGatherEvent event) {
//    	Entity entity = event.addEntitySpawn(MobCategory.MONSTER, new SpawnerData(new EntityType()));
//    	ResourceLocation entityResource = entity.getType().getRegistryName();
//    	String path = entityResource.getPath();
//    	LOGGER.info("onStructureSpawnListGatherEvent checking: "+path);
//    	event.setResult(Result.ALLOW);
//    	return;
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
//    @SubscribeEvent
//    public void onServerStarting(FMLServerStartingEvent event) {
//        // do something when the server starts
//        LOGGER.info("HELLO from server starting");
//    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
//    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
//    public static class RegistryEvents {
//        @SubscribeEvent
//        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
//            // register a new block here
//            LOGGER.info("HELLO from Register Bl)ock");
//        }
//    }
}


