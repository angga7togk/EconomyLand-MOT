package me.onebone.economyland;

/*
 * EconomyLand: A plugin which allows your server to manage lands
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import cn.nukkit.network.protocol.UpdateBlockPacket.Entry;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;
import me.onebone.economyland.provider.*;
import me.onebone.economyapi.EconomyAPI;

public class EconomyLand extends PluginBase implements Listener{
	private EconomyAPI api;
	
	private Provider provider;
	
	private Map<Player, Position[]> players;
	private PlayerManager manager;
	private List<Player> removes;
	private Map<String, String> lang;
	
	public String getMessage(String key){
		return this.getMessage(key, new String[]{});
	}
	
	public String getMessage(String key, Object[] params){
		if(this.lang.containsKey(key)){
			return replaceMessage(this.lang.get(key), params);
		}
		return "Could not find message with " + key;
	}
	
	private String replaceMessage(String lang, Object[] params){
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < lang.length(); i++){
			char c = lang.charAt(i);
			if(c == '{'){
				int index;
				if((index = lang.indexOf('}', i)) != -1){
					try{
						String p = lang.substring(i + 1, index);
						if(p.equals("M")){
							i = index;
							
							builder.append(api.getMonetaryUnit());
							continue;
						}
						int param = Integer.parseInt(p);
						
						if(params.length > param){
							i = index;
							
							builder.append(params[param]);
							continue;
						}
					}catch(NumberFormatException e){}
				}
			}else if(c == '&'){
				char color = lang.charAt(++i);
				if((color >= '0' && color <= 'f') || color == 'r' || color == 'l' || color == 'o'){
					builder.append(TextFormat.ESCAPE);
					builder.append(color);
					continue;
				}
			}
			
			builder.append(c);
		}
		
		return builder.toString();
	}
	
	@Override
	public void onEnable(){
		this.saveDefaultConfig();
		
		players = new HashMap<>();
		removes = new ArrayList<Player>();
		
		manager = new PlayerManager();
		
		api = EconomyAPI.getInstance();
		
		InputStream is = this.getResource("lang_" + this.getConfig().get("langauge", "eng") + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_eng.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		this.provider = new YamlProvider(this, new File(this.getDataFolder(), "Land.yml"));
		
		this.getServer().getScheduler().scheduleDelayedRepeatingTask(new ShowBlockTask(this), 20, 20);
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable(){
		if(this.provider != null){
			this.provider.close();
		}
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("land")){
			if(args.length < 1){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			
			if(args[0].equals("pos1")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				Player player = (Player) sender;
				
				if(!players.containsKey(player)){
					players.put(player, new Position[2]);
				}
				players.get(player)[0] = new Position(player.x, player.y, player.z, player.level);
				
				sender.sendMessage(this.getMessage("pos1-set", new Object[]{
						(int) player.x, (int) player.y, (int) player.z
				}));
			}else if(args[0].equals("pos2")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				Player player = (Player) sender;
				
				if(!players.containsKey(player)){
					sender.sendMessage(this.getMessage("pos1-not-set"));
					return true;
				}
				
				Position pos1 = players.get(player)[0];
				if(pos1.level != player.level){
					sender.sendMessage(this.getMessage("must-one-world"));
					return true;
				}
				players.get(player)[1] = new Position(player.x, player.y, player.z, player.level);

				double price = (Math.abs(Math.floor(player.x) - Math.floor(pos1.x)) + 1) * (Math.abs(Math.floor(player.y) - Math.floor(pos1.y)) + 1) * this.getConfig().getDouble("price.per-block", 100D);
				
				sender.sendMessage(this.getMessage("pos2-set", new Object[]{
						(int) player.x, (int) player.y, (int) player.z, price
				}));
			}else if(args[0].equals("buy")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				Player player = (Player) sender;
				
				if(!players.containsKey(player)){
					sender.sendMessage(this.getMessage("pos-not-set"));
					return true;
				}
				
				Position pos1 = players.get(player)[0];
				Position pos2 = players.get(player)[1];
				
				if(pos1 == null || pos2 == null || pos1.level != pos2.level){
					sender.sendMessage(this.getMessage("pos-not-set"));
					return true;
				}
				
				// TODO: Check money
				
				double price = (Math.abs(Math.floor(player.x) - Math.floor(pos1.x)) + 1) * (Math.abs(Math.floor(player.y) - Math.floor(pos1.y)) + 1) * this.getConfig().getDouble("price.per-block", 100D);
				this.provider.addLand(new Vector2(pos1.x, pos1.z), new Vector2(pos2.x, pos2.z), pos1.level, price, player.getName());
				
				removes.add(player);
				sender.sendMessage(this.getMessage("bought-land"));
			}else if(args[0].equals("sell")){
				// TODO
			}else if(args[0].equals("here")){
				if(!(sender instanceof Player)){
					sender.sendMessage(TextFormat.RED + "Please run this command in-game.");
					return true;
				}
				
				Player player = (Player) sender;
				
				Land land = this.provider.findLand(player);
				if(land == null){
					player.sendMessage(this.getMessage("no-land-here"));
				}else{
					Vector2 start = land.getStart();
					Vector2 end = land.getEnd();
					
					player.sendMessage(this.getMessage("land-info", new Object[]{
						land.getId(), (Math.abs(Math.floor(end.x) - Math.floor(start.x)) + 1) * (Math.abs(Math.floor(end.y) - Math.floor(start.y)) + 1), land.getOwner()
					}));
				}
			}else if(args[0].equals("give")){
				// TODO
			}else if(args[0].equals("whose")){
				// TODO
			}else if(args[0].equals("list")){
				int page = 1;
				
				Map<Integer, Land> lands = this.provider.getAll();
				if(args.length > 1){
					page = Math.min(lands.size() / 5, Math.max(1, Integer.parseInt(args[1])));
				}
				
				StringBuilder builder = new StringBuilder(this.getMessage("land-list-header", new Object[]{page, lands.size() / 5}) + "\n");
				int i = 1;
				
				for(Land land : lands.values()){
					int current = (int)Math.ceil((double)(i++) / 5);
					
					if(current == page){
						Vector2 start = land.getStart();
						Vector2 end = land.getEnd();
						
						builder.append(this.getMessage("land-info", new Object[]{
								land.getId(), (Math.abs(Math.floor(end.x) - Math.floor(start.x)) + 1) * (Math.abs(Math.floor(end.y) - Math.floor(start.y)) + 1), land.getOwner()
						}) + "\n");
					}else if(current > page) break;
				}
				
				sender.sendMessage(builder.toString());
			}else if(args[0].equals("move")){
				// TODO
			}else{
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
			}
			return true;
		}
		
		return false;
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event){
		Player player = event.getPlayer();
		Block block = event.getBlock();
		
		Land land;
		if((land = this.provider.findLand(block)) != null){
			if(!(land.getOwner().toLowerCase().equals(player.getName().toLowerCase()) || player.hasPermission("economyland.admin.modify"))){
				player.sendMessage(this.getMessage("modify-forbidden"));
				
				event.setCancelled(true);
			}
		}else if(this.getConfig().getStringList("white-world-protection").contains(block.level.getFolderName()) || player.hasPermission("economyland.admin.modify")){
			player.sendMessage(this.getMessage("modify-whiteland"));
			
			event.setCancelled();
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event){
		Player player = event.getPlayer();
		Block block = event.getBlockReplace();
		
		Land land;
		if((land = this.provider.findLand(block)) != null){
			if(!(land.getOwner().toLowerCase().equals(player.getName().toLowerCase()) || player.hasPermission("economyland.admin.modify"))){
				event.setCancelled(true);
				
				player.sendMessage(this.getMessage("modify-forbidden"));
			}
		}else if(this.getConfig().getStringList("white-world-protection").contains(block.level.getFolderName()) || player.hasPermission("economyland.admin.modify")){
			event.setCancelled(true);
			
			player.sendMessage(this.getMessage("modify-whiteland"));
		}
	}
	
	@EventHandler
	public void onItemPickup(InventoryPickupItemEvent event){
		if(event.getInventory().getHolder() instanceof Player){
			Player player = (Player) event.getInventory().getHolder();
			EntityItem item = event.getItem();
			
			Land land;
			if((land = this.provider.findLand(item)) != null && !land.getOption("pickup", false)){
				if(!(land.getOwner().toLowerCase().equals(player.getName().toLowerCase()) || player.hasPermission("economyland.admin.pickup"))){
					event.setCancelled(true);
					
					if(this.manager.canShow(player)){
						player.sendMessage(this.getMessage("pickup-forbidden", new Object[]{
								land.getId(), land.getOwner()
						}));
						this.manager.setShown(player);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		
		if(this.manager.isMoved(player)){
			Land land;
			if((land = this.provider.findLand(player)) != null && !land.getOption("access", false)){
				if(!(land.getOwner().toLowerCase().equals(player.getName().toLowerCase()) || player.hasPermission("economyland.admin.access"))){
					player.teleport(this.manager.getLastPosition(player));
					
					if(this.manager.canShow(player)){
						player.sendMessage(this.getMessage("access-forbidden", new Object[]{
							land.getId(), land.getOwner()
						}));
						
						this.manager.setShown(player);
					}
					return;
				}
			}
			this.manager.setPosition(player);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		Player player = event.getPlayer();
		
		this.manager.unsetPlayer(player);
	}
	
	public void showBlocks(boolean show){
		UpdateBlockPacket pk = new UpdateBlockPacket();
		
		for(Player player : players.keySet()){
			Position[] pos = players.get(player);
			
			if(player == null) return; // If player is not in server
			
			Position pos1 = pos[0];
			Position pos2 = pos[1];
			
			Entry[] entries = new Entry[1];
			if(pos2 != null){
				entries = new UpdateBlockPacket.Entry[4];
			}
			
			if(pos1 != null){
				if(pos1.level == player.level){
					entries[0] = new Entry((int) pos1.x, (int) pos1.z, (int) pos1.y, 
							show ? Block.GLASS : player.level.getBlock(pos1).getId(), 0, UpdateBlockPacket.FLAG_ALL);
					
					if(pos2 != null){
						entries[1] = new Entry((int) pos2.x, (int) pos2.z, (int) pos2.y, 
								show ? Block.GLASS : player.level.getBlock(pos1).getId(), 0, UpdateBlockPacket.FLAG_ALL);
						entries[2] = new Entry((int) pos1.x, (int) pos2.z, (int) pos1.y, 
								show ? Block.GLASS : player.level.getBlock(pos1).getId(), 0, UpdateBlockPacket.FLAG_ALL);
						entries[3] = new Entry((int) pos2.x, (int) pos1.z, (int) pos1.y, 
								show ? Block.GLASS : player.level.getBlock(pos1).getId(), 0, UpdateBlockPacket.FLAG_ALL);
					}
					pk.records = entries;
				}
				
				player.dataPacket(pk);
			}
			
			if(!show && removes.contains(player)){
				players.remove(player);
				removes.remove(player);
			}
		}
	}
}
