package com.feed_the_beast.ftbu.cmd.tp;

import com.feed_the_beast.ftbl.lib.cmd.CommandLM;
import com.feed_the_beast.ftbl.lib.math.BlockDimPos;
import com.feed_the_beast.ftbl.lib.util.LMServerUtils;
import com.feed_the_beast.ftbl.lib.util.LMStringUtils;
import com.feed_the_beast.ftbu.api.FTBULang;
import com.feed_the_beast.ftbu.world.FTBUUniverseData;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class CmdWarp extends CommandLM
{
    @Override
    public String getName()
    {
        return "warp";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender ics)
    {
        return '/' + getName() + " <ID>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if(args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, FTBUUniverseData.get().listWarps());
        }

        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        checkArgs(args, 1, "<warp>");

        args[0] = args[0].toLowerCase();

        if(args[0].equals("list"))
        {
            Collection<String> list = FTBUUniverseData.get().listWarps();
            sender.sendMessage(new TextComponentString(list.isEmpty() ? "-" : LMStringUtils.strip(list)));
            return;
        }

        EntityPlayerMP ep = getCommandSenderAsPlayer(sender);
        BlockDimPos p = FTBUUniverseData.get().getWarp(args[0]);
        if(p == null)
        {
            throw FTBULang.WARP_NOT_SET.commandError(args[0]);
        }

        LMServerUtils.teleportPlayer(ep, p);
        FTBULang.WARP_TP.printChat(sender, args[0]);
    }
}