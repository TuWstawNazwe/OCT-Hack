package me.zeroeightsix.kami.command.commands;

import me.zeroeightsix.kami.KamiMod;
import me.zeroeightsix.kami.command.Command;
import me.zeroeightsix.kami.command.syntax.ChunkBuilder;
import me.zeroeightsix.kami.command.syntax.parsers.DependantParser;
import me.zeroeightsix.kami.command.syntax.parsers.EnumParser;
import me.zeroeightsix.kami.gui.kami.KamiGUI;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static me.zeroeightsix.kami.util.MessageSendHelper.sendChatMessage;

/**
 * Created by 086 on 14/10/2018.
 */
public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", new ChunkBuilder()
                .append("mode", true, new EnumParser(new String[]{"reload", "save", "path"}))
                .append("path", true, new DependantParser(0, new DependantParser.Dependency(new String[][]{{"path", "path"}}, "")))
                .build(), "cfg");
        setDescription("Change where your config is saved or manually save and reload your config");
    }

    @Override
    public void call(String[] args) {
        if (args[0] == null) {
            sendChatMessage("Missing argument &bmode&r: Choose from reload, save or path");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reload();
                break;
            case "save":
                try {
                    KamiMod.saveConfigurationUnsafe();
                    sendChatMessage("Saved configuration!");
                } catch (IOException e) {
                    e.printStackTrace();
                    sendChatMessage("Failed to save! " + e.getMessage());
                }
                break;
            case "path":
                if (args[1] == null) {
                    Path file = Paths.get(KamiMod.getConfigName());
                    sendChatMessage("Path to configuration: &b" + file.toAbsolutePath().toString());
                } else {
                    String newPath = args[1];
                    if (!KamiMod.isFilenameValid(newPath)) {
                        sendChatMessage("&b" + newPath + "&r is not a valid path");
                        break;
                    }
                    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("OCTLastConfig.txt"))) {
                        writer.write(newPath);
                        reload();
                        sendChatMessage("Configuration path set to &b" + newPath + "&r!");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendChatMessage("Couldn't set path: " + e.getMessage());
                        break;
                    }
                }
                break;
            default:
                sendChatMessage("Incorrect mode, please choose from: reload, save or path");
        }
    }

    private void reload() {
        KamiMod.getInstance().guiManager.initializeGUI();
        KamiMod.loadConfiguration();
        sendChatMessage("Configuration reloaded!");
    }

}
