package com.feed_the_beast.ftbu.cmd.tp;

import com.feed_the_beast.ftbl.lib.cmd.CommandLM;
import com.feed_the_beast.ftbl.lib.math.BlockDimPos;
import com.feed_the_beast.ftbl.lib.util.LMServerUtils;
import com.feed_the_beast.ftbl.lib.util.LMStringUtils;
import com.feed_the_beast.ftbu.api.FTBULang;
import com.feed_the_beast.ftbu.world.FTBUPlayerData;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.command.CommandTreeBase;

public class CmdAdminHome extends CommandTreeBase
{
    public static class CmdTP extends CommandLM
    {
        @Override
        public String getName()
        {
            return "tp";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP ep = getCommandSenderAsPlayer(sender);
            checkArgs(args, 2, "<player> <home>");
            args[1] = args[1].toLowerCase();
            FTBUPlayerData data = FTBUPlayerData.get(getForgePlayer(args[0]));

            if(data == null)
            {
                return;
            }

            BlockDimPos pos = data.getHome(args[1]);

            if(pos != null)
            {
                LMServerUtils.teleportPlayer(ep, pos);
                FTBULang.WARP_TP.printChat(sender, args[1]);
            }

            throw FTBULang.HOME_NOT_SET.commandError(args[1]);
        }
    }

    public static class CmdList extends CommandLM
    {
        @Override
        public String getName()
        {
            return "list";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            checkArgs(args, 1, "<player>");
            FTBUPlayerData data = FTBUPlayerData.get(getForgePlayer(args[0]));
            sender.sendMessage(new TextComponentString(LMStringUtils.strip(data.listHomes())));
        }
    }

    public static class CmdRem extends CommandLM
    {
        @Override
        public String getName()
        {
            return "remove";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            checkArgs(args, 2, "<player> <home>");
            FTBUPlayerData data = FTBUPlayerData.get(getForgePlayer(args[0]));

            if(data == null)
            {
                return;
            }

            args[1] = args[1].toLowerCase();
            BlockDimPos pos = data.getHome(args[1]);

            if(pos != null)
            {
                if(data.setHome(args[1], null))
                {
                    FTBULang.HOME_DEL.printChat(sender, args[1]);
                }
            }

            throw FTBULang.HOME_NOT_SET.commandError(args[1]);
        }
    }

    public CmdAdminHome()
    {
        addSubcommand(new CmdTP());
        addSubcommand(new CmdList());
        addSubcommand(new CmdRem());
    }

    @Override
    public String getName()
    {
        return "admin_home";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "command.ftb.admin_home.usage";
    }
}