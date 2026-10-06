package cn.myrealm.customarcheology.hooks.craftengine;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.mechanics.cores.BlockMode;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CraftEngineResourceInstallerTest {
    @TempDir
    Path temporaryFolder;

    @Test
    void installsDefinitionsAndTexturesIntoCraftEngineResources() throws Exception {
        Path pluginFolder = temporaryFolder.resolve("CustomArcheology");
        Path craftEngineFolder = temporaryFolder.resolve("CraftEngine");
        Files.createDirectories(pluginFolder.resolve("blocks"));
        Files.createDirectories(pluginFolder.resolve("tools"));
        Files.createDirectories(pluginFolder.resolve("textures/blocks"));
        Files.createDirectories(pluginFolder.resolve("textures/tools"));
        Files.createDirectories(pluginFolder.resolve("pack/assets/example"));
        Files.createDirectories(pluginFolder.resolve("pack/assets/minecraft/models/item"));
        Files.writeString(pluginFolder.resolve("pack/pack.mcmeta"), "{}");
        Files.writeString(pluginFolder.resolve("pack/assets/minecraft/models/item/brush.json"),
                "legacy dispatcher");
        Files.writeString(pluginFolder.resolve("blocks/suspicious_stone.yml"), """
                general:
                  display-name: "&fSuspicious Stone"
                  replace-block: mypack:ancient_stone
                states:
                  default:
                    texture: suspicious_stone_0
                    hardness: 2.5
                  state_1:
                    texture: suspicious_stone_1
                  finished:
                    material: stone
                """);
        Files.writeString(pluginFolder.resolve("tools/diamond_brush.yml"), """
                display-name: "&bDiamond Brush"
                lore:
                  - "&7A Diamond Brush"
                texture: diamond_brush
                """);
        byte[] firstTexture = {1, 2, 3};
        byte[] secondTexture = {4, 5, 6};
        byte[] toolTexture = {7, 8, 9};
        Files.write(pluginFolder.resolve("textures/blocks/suspicious_stone_0.png"), firstTexture);
        Files.write(pluginFolder.resolve("textures/blocks/suspicious_stone_1.png"), secondTexture);
        Files.write(pluginFolder.resolve("textures/tools/diamond_brush.png"), toolTexture);
        Files.writeString(pluginFolder.resolve("pack/assets/example/custom.txt"), "copied");

        CustomArcheology plugin = mock(CustomArcheology.class);
        Plugin craftEngine = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getDataFolder()).thenReturn(pluginFolder.toFile());
        when(plugin.getBlockMode()).thenReturn(BlockMode.CRAFTENGINE);
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("settings.block-material", "STONE");
        when(plugin.getConfig()).thenReturn(pluginConfig);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("CraftEngine")).thenReturn(craftEngine);
        when(craftEngine.getDataFolder()).thenReturn(craftEngineFolder.toFile());
        CustomArcheology.plugin = plugin;

        int installed = new CraftEngineResourceInstaller(plugin).install();

        Path packFolder = craftEngineFolder.resolve("resources/customarcheology");
        Path generated = packFolder.resolve("configuration/customarcheology-generated.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(generated.toFile());
        assertEquals(1, installed);
        assertTrue(Files.isRegularFile(packFolder.resolve("pack.yml")));
        assertEquals("block_item", config.getString(
                "items.customarcheology:suspicious_stone.behavior.type"));
        assertEquals("<white>Suspicious Stone", config.getString(
                "items.customarcheology:suspicious_stone.data.item_name"));
        assertEquals("<aqua>Diamond Brush", config.getString(
                "items.customarcheology:diamond_brush.data.item_name"));
        assertEquals("minecraft:range_dispatch", config.getString(
                "items.customarcheology:diamond_brush.model.type"));
        assertEquals("customarcheology:brushable", config.getString(
                "blocks.customarcheology:suspicious_stone.behavior.type"));
        assertEquals("mypack:ancient_stone", config.getString(
                "blocks.customarcheology:suspicious_stone.behavior.replace-block"));
        assertEquals(2.5, config.getDouble(
                "blocks.customarcheology:suspicious_stone.settings.hardness"));
        assertEquals("0~1", config.getString(
                "blocks.customarcheology:suspicious_stone.states.properties.dusted.range"));
        assertArrayEquals(firstTexture, Files.readAllBytes(packFolder.resolve(
                "resourcepack/assets/customarcheology/textures/block/suspicious_stone_0.png")));
        assertArrayEquals(secondTexture, Files.readAllBytes(packFolder.resolve(
                "resourcepack/assets/customarcheology/textures/block/suspicious_stone_1.png")));
        assertArrayEquals(toolTexture, Files.readAllBytes(packFolder.resolve(
                "resourcepack/assets/customarcheology/textures/item/diamond_brush.png")));
        assertTrue(Files.isRegularFile(packFolder.resolve(
                "resourcepack/assets/customarcheology/models/item/diamond_brush_10.json")));
        assertEquals("copied", Files.readString(packFolder.resolve(
                "resourcepack/assets/example/custom.txt")));
        assertEquals("{}", Files.readString(packFolder.resolve("resourcepack/pack.mcmeta")));
        assertTrue(Files.notExists(packFolder.resolve(
                "resourcepack/assets/minecraft/models/item/brush.json")));
    }

    @Test
    void doesNothingInLegacyMode() throws Exception {
        CustomArcheology plugin = mock(CustomArcheology.class);
        when(plugin.getBlockMode()).thenReturn(BlockMode.LEGACY);

        assertEquals(0, new CraftEngineResourceInstaller(plugin).install());
    }
}
