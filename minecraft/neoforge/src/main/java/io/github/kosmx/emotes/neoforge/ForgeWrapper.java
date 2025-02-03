package io.github.kosmx.emotes.neoforge;

import io.github.kosmx.emotes.arch.ClientCommands;
import io.github.kosmx.emotes.mc.ServerCommands;
import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.neoforge.executor.ForgeEmotesMain;
import io.github.kosmx.emotes.main.MainLoader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

@Mod("emotecraft")
public class ForgeWrapper {
    public static final Logger logger = LoggerFactory.getLogger(CommonData.MOD_ID);

    public ForgeWrapper() {
        EmoteInstance.instance = new ForgeEmotesMain();

        MainLoader.main(null);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void commandRegister(RegisterCommandsEvent event) {
        ServerCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public void clientCommandRegister(RegisterClientCommandsEvent event) {
        ClientCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    public static void log(Level level, String msg){
        if (level.intValue() <= Level.INFO.intValue()) {
            logger.info(msg);
        } else if (level.intValue() <= Level.WARNING.intValue()) {
            logger.warn(msg);
        } else {
            logger.error(msg);
        }
    }

    public static void log(Level level, String msg, Throwable t){
        if (level.intValue() <= Level.INFO.intValue()) {
            logger.info(msg, t);
        } else if (level.intValue() <= Level.WARNING.intValue()) {
            logger.warn(msg, t);
        } else {
            logger.error(msg, t);
        }
    }
}
