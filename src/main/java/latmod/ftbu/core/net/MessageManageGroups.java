package latmod.ftbu.core.net;
import static net.minecraft.util.EnumChatFormatting.*;
import io.netty.buffer.ByteBuf;
import latmod.ftbu.core.*;
import latmod.ftbu.core.world.*;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.network.simpleimpl.*;

public class MessageManageGroups extends MessageLM<MessageManageGroups>
{
	public static final int C_ADD_FRIEND = 1;
	public static final int C_REM_FRIEND = 2;
	
	public int playerID;
	public int code;
	public int user;
	
	public MessageManageGroups() { }
	
	public MessageManageGroups(LMPlayer p, int c, int u)
	{
		playerID = p.playerID;
		code = c;
		user = u;
	}
	
	public void fromBytes(ByteBuf bb)
	{
		playerID = bb.readInt();
		code = bb.readByte();
		user = bb.readInt();
	}
	
	public void toBytes(ByteBuf bb)
	{
		bb.writeInt(playerID);
		bb.writeByte(code);
		bb.writeInt(user);
	}
	
	public IMessage onMessage(MessageManageGroups m, MessageContext ctx)
	{
		LMPlayerServer owner = LMWorld.server.getPlayer(m.playerID);
		EntityPlayerMP ep = ctx.getServerHandler().playerEntity;
		//EntityPlayerMP ep = owner.getPlayerMP();
		
		if(owner == null || !ep.getUniqueID().equals(owner.getUUID())) return null;
		
		if(m.code > 0)
		{
			LMPlayerServer p = LMWorld.server.getPlayer(m.user);
			if(p == null || p.equalsPlayer(owner)) return null;
			
			if(m.code == C_ADD_FRIEND)
			{
				if(!owner.friends.contains(p.playerID))
				{
					owner.friends.add(p.playerID);
					owner.sendUpdate(LMPlayer.ACTION_GROUPS_CHANGED, true);
					p.sendUpdate(LMPlayer.ACTION_GROUPS_CHANGED, true);
					LatCoreMC.notifyPlayer(ep, new Notification(GREEN + "Added friend", p.getName(), null, 800));
				}
			}
			else if(m.code == C_REM_FRIEND)
			{
				if(owner.friends.contains(p.playerID))
				{
					owner.friends.removeValue(p.playerID);
					owner.sendUpdate(LMPlayer.ACTION_GROUPS_CHANGED, true);
					p.sendUpdate(LMPlayer.ACTION_GROUPS_CHANGED, true);
					LatCoreMC.notifyPlayer(ep, new Notification(RED + "Removed friend", p.getName(), null, 800));
				}
			}
		}
		
		return null;
	}
}