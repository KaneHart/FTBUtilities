package ftb.utils.mod.client.gui.claims;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.TextureCoords;
import ftb.lib.api.GuiLang;
import ftb.lib.api.MouseButton;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.client.GlStateManager;
import ftb.lib.api.gui.GuiIcons;
import ftb.lib.api.gui.GuiLM;
import ftb.lib.api.gui.widgets.ButtonLM;
import ftb.lib.api.gui.widgets.PanelLM;
import ftb.utils.mod.FTBULang;
import ftb.utils.mod.client.FTBUClient;
import ftb.utils.net.MessageAreaRequest;
import ftb.utils.net.MessageClaimChunk;
import ftb.utils.net.MessageRequestSelfUpdate;
import ftb.utils.world.LMPlayerClient;
import ftb.utils.world.LMPlayerClientSelf;
import ftb.utils.world.LMWorldClient;
import ftb.utils.world.claims.ChunkType;
import latmod.lib.MathHelperLM;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiClaimChunks extends GuiLM implements GuiYesNoCallback // implements IClientActionGui
{
	public static final int tiles_tex = 16;
	public static final int tiles_gui = 15;
	public static final double UV = (double) tiles_gui / (double) tiles_tex;
	public static final ResourceLocation tex_area = new ResourceLocation("ftbu", "textures/map/minimap_area.png");
	public static final ResourceLocation tex_map_entity = new ResourceLocation("ftbu", "textures/map/entity.png");
	public static final TextureCoords[][][][] tex_area_coords = new TextureCoords[2][2][2][2];
	
	private static TextureCoords getAreaCoords(int i)
	{ return new TextureCoords(tex_area, (i % 4) * 64, (i / 4) * 64, 64, 64); }
	
	static
	{
		tex_area_coords[0][0][0][0] = getAreaCoords(0);
		tex_area_coords[1][1][1][1] = getAreaCoords(1);
		tex_area_coords[1][0][1][0] = getAreaCoords(2);
		tex_area_coords[0][1][0][1] = getAreaCoords(3);
		tex_area_coords[1][0][0][0] = getAreaCoords(4);
		tex_area_coords[0][1][0][0] = getAreaCoords(5);
		tex_area_coords[0][0][1][0] = getAreaCoords(6);
		tex_area_coords[0][0][0][1] = getAreaCoords(7);
		tex_area_coords[1][1][0][0] = getAreaCoords(8);
		tex_area_coords[0][1][1][0] = getAreaCoords(9);
		tex_area_coords[0][0][1][1] = getAreaCoords(10);
		tex_area_coords[1][0][0][1] = getAreaCoords(11);
		tex_area_coords[0][1][1][1] = getAreaCoords(12);
		tex_area_coords[1][0][1][1] = getAreaCoords(13);
		tex_area_coords[1][1][0][1] = getAreaCoords(14);
		tex_area_coords[1][1][1][0] = getAreaCoords(15);
	}
	
	public static int textureID = -1;
	public static ByteBuffer pixelBuffer = null;
	
	public final long adminToken;
	public final LMPlayerClientSelf playerLM;
	public final int currentDim, startX, startY;
	
	public final ButtonLM buttonRefresh, buttonClose, buttonUnclaimAll;
	public final List<MapButton> mapButtons;
	public final PanelLM panelButtons;
	
	public ThreadReloadArea thread = null;
	
	public GuiClaimChunks(long token)
	{
		super(null, null);
		mainPanel.width = mainPanel.height = tiles_gui * 16;
		
		adminToken = token;
		playerLM = LMWorldClient.inst.clientPlayer;
		startX = MathHelperLM.chunk(mc.thePlayer.posX) - (int) (tiles_gui * 0.5D);
		startY = MathHelperLM.chunk(mc.thePlayer.posZ) - (int) (tiles_gui * 0.5D);
		currentDim = FTBLibClient.getDim();
		
		buttonClose = new ButtonLM(this, 0, 0, 16, 16)
		{
			@Override
			public void onClicked(MouseButton button)
			{
				FTBLibClient.playClickSound();
				gui.close(null);
			}
		};
		
		buttonRefresh = new ButtonLM(this, 0, 16, 16, 16)
		{
			@Override
			public void onClicked(MouseButton button)
			{
				thread = new ThreadReloadArea(mc.theWorld, GuiClaimChunks.this);
				thread.start();
				new MessageAreaRequest(startX, startY, tiles_gui, tiles_gui).sendToServer();
				new MessageRequestSelfUpdate().sendToServer();
				FTBLibClient.playClickSound();
			}
		};
		
		buttonRefresh.title = GuiLang.button_refresh.format();
		
		buttonUnclaimAll = new ButtonLM(this, 0, 32, 16, 16)
		{
			@Override
			public void onClicked(MouseButton button)
			{
				FTBLibClient.playClickSound();
				String s = isShiftKeyDown() ? FTBULang.button_claims_unclaim_all_q.format() : FTBULang.button_claims_unclaim_all_dim_q.format(FTBLibClient.mc.theWorld.provider.getDimensionName());
				FTBLibClient.openGui(new GuiYesNo(GuiClaimChunks.this, s, "", isShiftKeyDown() ? 1 : 0));
			}
			
			@Override
			public void addMouseOverText(List<String> l)
			{
				if(isShiftKeyDown()) l.add(FTBULang.button_claims_unclaim_all.format());
				else
					l.add(FTBULang.button_claims_unclaim_all_dim.format(FTBLibClient.mc.theWorld.provider.getDimensionName()));
			}
		};
		
		panelButtons = new PanelLM(this, 0, 0, 16, 0)
		{
			@Override
			public void addWidgets()
			{
				add(buttonClose);
				add(buttonRefresh);
				
				if(adminToken == 0L)
				{
					add(buttonUnclaimAll);
				}
				
				height = widgets.size() * 16;
			}
			
			@Override
			public int getAX()
			{ return gui.getGui().width - 16; }
			
			@Override
			public int getAY()
			{ return 0; }
		};
		
		mapButtons = new ArrayList<>(tiles_gui * tiles_gui);
		for(int i = 0; i < tiles_gui * tiles_gui; i++)
			mapButtons.add(new MapButton(this, 0, 0, i));
	}
	
	@Override
	public void initLMGui()
	{
		buttonRefresh.onClicked(MouseButton.LEFT);
	}
	
	@Override
	public void addWidgets()
	{
		mainPanel.addAll(mapButtons);
		mainPanel.add(panelButtons);
	}
	
	@Override
	public void drawBackground()
	{
		if(currentDim != FTBLibClient.getDim() || !FTBLibClient.isIngameWithFTBU())
		{
			mc.thePlayer.closeScreen();
			return;
		}
		
		if(pixelBuffer != null)
		{
			if(textureID == -1)
			{
				textureID = TextureUtil.glGenTextures();
				new MessageAreaRequest(startX, startY, tiles_gui, tiles_gui).sendToServer();
			}
			
			//boolean hasBlur = false;
			//int filter = hasBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
			GlStateManager.bindTexture(textureID);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, tiles_tex * 16, tiles_tex * 16, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
			pixelBuffer = null;
			thread = null;
		}
		
		GlStateManager.color(0F, 0F, 0F, 1F);
		drawBlankRect(mainPanel.posX - 2, mainPanel.posY - 2, zLevel, mainPanel.width + 4, mainPanel.height + 4);
		//drawBlankRect((xSize - 128) / 2, (ySize - 128) / 2, zLevel, 128, 128);
		GlStateManager.color(1F, 1F, 1F, 1F);
		
		if(textureID != -1 && thread == null)
		{
			GlStateManager.bindTexture(textureID);
			drawTexturedRectD(mainPanel.posX, mainPanel.posY, zLevel, tiles_gui * 16, tiles_gui * 16, 0D, 0D, UV, UV);
		}
		
		super.drawBackground();
		
		GlStateManager.color(1F, 1F, 1F, 1F);
		//setTexture(tex);
		
		renderMinimap();
		
		GlStateManager.color(1F, 1F, 1F, 1F);
		for(MapButton b : mapButtons)
			b.renderWidget();
		GlStateManager.color(1F, 1F, 1F, 1F);
		
		buttonRefresh.render(GuiIcons.refresh);
		buttonClose.render(GuiIcons.accept);
		
		if(adminToken == 0L)
		{
			buttonUnclaimAll.render(GuiIcons.remove);
		}
	}
	
	@Override
	public void drawText(List<String> l)
	{
		String s = FTBULang.label_cchunks_count.format(playerLM.claimedChunks + " / " + playerLM.maxClaimedChunks);
		fontRendererObj.drawString(s, width - fontRendererObj.getStringWidth(s) - 4, height - 12, 0xFFFFFFFF);
		s = FTBULang.label_lchunks_count.format(playerLM.loadedChunks + " / " + playerLM.maxLoadedChunks);
		fontRendererObj.drawString(s, width - fontRendererObj.getStringWidth(s) - 4, height - 24, 0xFFFFFFFF);
		
		super.drawText(l);
	}
	
	@Override
	public void onLMGuiClosed()
	{
	}
	
	@SuppressWarnings("unchecked")
	public void renderMinimap()
	{
		FTBLibClient.setTexture(tex_area);
		
		for(int y = 0; y < tiles_gui; y++)
			for(int x = 0; x < tiles_gui; x++)
			{
				int cx = x + startX;
				int cy = y + startY;
				
				ChunkType type = ClaimedAreasClient.getType(cx, cy);
				if(type.drawGrid())
				{
					boolean a = type.equals(ClaimedAreasClient.getType(cx, cy - 1));
					boolean b = type.equals(ClaimedAreasClient.getType(cx + 1, cy));
					boolean c = type.equals(ClaimedAreasClient.getType(cx, cy + 1));
					boolean d = type.equals(ClaimedAreasClient.getType(cx - 1, cy));
					
					TextureCoords tc = tex_area_coords[a ? 1 : 0][b ? 1 : 0][c ? 1 : 0][d ? 1 : 0];
					
					FTBLibClient.setGLColor(type.getAreaColor(playerLM), 255);
					GuiLM.drawTexturedRectD(mainPanel.posX + x * 16, mainPanel.posY + y * 16, zLevel, 16, 16, tc.minU, tc.minV, tc.maxU, tc.maxV);
				}
			}
		
		if(!FTBLibClient.mc.theWorld.playerEntities.isEmpty())
		{
			ArrayList<EntityPlayer> list = new ArrayList<>();
			list.addAll(FTBLibClient.mc.theWorld.playerEntities);
			
			for(int i = 0; i < list.size(); i++)
			{
				EntityPlayer ep = list.get(i);
				if(ep.dimension == currentDim && !ep.isInvisible())
				{
					int cx = MathHelperLM.chunk(ep.posX);
					int cy = MathHelperLM.chunk(ep.posZ);
					
					if(cx >= startX && cy >= startY && cx < startX + tiles_gui && cy < startY + tiles_gui)
					{
						double x = ((cx - startX) * 16D + MathHelperLM.wrap(ep.posX, 16D));
						double y = ((cy - startY) * 16D + MathHelperLM.wrap(ep.posZ, 16D));
						
						GlStateManager.pushMatrix();
						GlStateManager.translate(mainPanel.posX + x, mainPanel.posY + y, 0D);
						GlStateManager.pushMatrix();
						//GlStateManager.rotate((int)((ep.rotationYaw + 180F) / (180F / 8F)) * (180F / 8F), 0F, 0F, 1F);
						GlStateManager.rotate(ep.rotationYaw + 180F, 0F, 0F, 1F);
						FTBLibClient.setTexture(tex_map_entity);
						GlStateManager.color(1F, 1F, 1F, ep.isSneaking() ? 0.4F : 0.7F);
						GuiLM.drawTexturedRectD(-8, -8, zLevel, 16, 16, 0D, 0D, 1D, 1D);
						GlStateManager.popMatrix();
						GuiLM.drawPlayerHead(ep.getCommandSenderName(), -2, -2, 4, 4, zLevel);
						GlStateManager.popMatrix();
					}
				}
			}
			
			GlStateManager.color(1F, 1F, 1F, 1F);
		}
	}
	
	@Override
	public void confirmClicked(boolean set, int id)
	{
		if(set && adminToken == 0L)
		{
			new MessageClaimChunk((id == 1) ? MessageClaimChunk.ID.UNCLAIM_ALL_DIMS : MessageClaimChunk.ID.UNCLAIM_ALL, GuiClaimChunks.this.adminToken, GuiClaimChunks.this.currentDim, 0, 0).sendToServer();
			new MessageAreaRequest(startX, startY, tiles_gui, tiles_gui).sendToServer();
		}
		
		FTBLibClient.openGui(this);
		refreshWidgets();
	}
	
	public static class MapButton extends ButtonLM
	{
		public final GuiClaimChunks gui;
		public final int chunkX, chunkY;
		
		public MapButton(GuiClaimChunks g, int x, int y, int i)
		{
			super(g, x, y, 16, 16);
			gui = g;
			posX += (i % tiles_gui) * width;
			posY += (i / tiles_gui) * height;
			chunkX = g.startX + (i % tiles_gui);
			chunkY = g.startY + (i / tiles_gui);
		}
		
		@Override
		public void onClicked(MouseButton button)
		{
			if(gui.panelButtons.mouseOver()) return;
			if(gui.adminToken != 0L && button.isLeft()) return;
			boolean ctrl = FTBUClient.loaded_chunks_space_key.getAsBoolean() ? Keyboard.isKeyDown(Keyboard.KEY_SPACE) : isCtrlKeyDown();
			new MessageClaimChunk(button.isLeft() ? (ctrl ? MessageClaimChunk.ID.LOAD : MessageClaimChunk.ID.CLAIM) : (ctrl ? MessageClaimChunk.ID.UNLOAD : MessageClaimChunk.ID.UNCLAIM), gui.adminToken, gui.currentDim, chunkX, chunkY).sendToServer();
			FTBLibClient.playClickSound();
		}
		
		@Override
		public void addMouseOverText(List<String> l)
		{
			ChunkType type = ClaimedAreasClient.getType(chunkX, chunkY);
			
			if(type != null)
			{
				l.add(type.getChatColor(null) + type.langKey.format());
				
				if(type.isClaimed())
				{
					ChunkType.PlayerClaimed pc = (ChunkType.PlayerClaimed) type;
					
					LMPlayerClient owner = pc.chunk.getOwnerC();
					
					if(owner != null)
					{
						l.add(type.getChatColor(owner) + owner.getProfile().getName());
						if(pc.chunk.isChunkloaded)
						{
							l.add(I18n.format("ftbu.chunktype.chunkloaded"));
						}
					}
				}
			}
		}
		
		@Override
		public void renderWidget()
		{
			if(mouseOver())
			{
				GlStateManager.color(1F, 1F, 1F, 0.27F);
				drawBlankRect(getAX(), getAY(), gui.getZLevel(), 16, 16);
				GlStateManager.color(1F, 1F, 1F, 1F);
			}
		}
	}
}