package me.onebone.economyland.provider;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;
import cn.nukkit.utils.Utils;
import me.onebone.economyland.EconomyLand;
import me.onebone.economyland.Land;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class YamlProvider implements Provider{
	private int landId = 0;
	
	private File file;
	private Map<Integer, Land> lands;
	private EconomyLand plugin;
	
	@SuppressWarnings("unchecked")
	public YamlProvider(EconomyLand plugin, File file){
		lands = new HashMap<>();
		
		this.plugin = plugin;
		this.file = file;
		
		try{
			File dataFile = new File(plugin.getDataFolder(), "LandData.json");
			if(dataFile.exists()){
				Map<String, Object> data = new GsonBuilder().create().fromJson(Utils.readFile(dataFile), new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
				this.landId = (int) Double.parseDouble(data.getOrDefault("landId", 0).toString());
			}
		}catch(JsonSyntaxException | IOException e){
			plugin.getLogger().critical(e.getMessage());
		}
		
		Yaml yaml = new Yaml();
		
		HashMap<String, Level> tmp = new HashMap<String, Level>();
		try {
			Map<Integer, LinkedHashMap<String, Object>> load = ((Map<Integer, LinkedHashMap<String, Object>>)yaml.load(Utils.readFile(file)));

			load.forEach((k, v) -> {
				if(!tmp.containsKey((String) v.get("level"))){
					tmp.put((String) v.get("level"), plugin.getServer().getLevelByName((String) v.get("level")));
				}
				
				lands.put(k, new Land(k, new Vector2((int) v.get("startX"), (int) v.get("startZ")), new Vector2((int) v.get("endX"), (int) v.get("endZ")), tmp.get((String) v.get("level")), (double) v.get("price"), (String) v.get("owner"), (Map<String, Object>)v.getOrDefault("options", new HashMap<String, Object>())));
			});
		}catch(FileNotFoundException e){
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void addLand(Vector2 start, Vector2 end, Level level, double price, String owner){
		lands.put(landId++, new Land(landId++, start, end, level, price, owner, new HashMap<String, Object>()));
	}

	@Override
	public boolean removeLand(int id){
		if(lands.containsKey(id)){
			lands.remove(id);
			
			return true;
		}
		return false;
	}

	@Override
	public Land getLand(int id){
		if(lands.containsKey(id)){
			return lands.get(id);
		}
		return null;
	}

	@Override
	public Land findLand(Position pos){
		for(Land land : lands.values()){
			if(land.check(pos)) return land;
		}
		return null;
	}
	
	@Override
	public Land checkOverlap(Position start, Position end){
		for(Land land : lands.values()){
			if(land.check(start) || land.check(end)) return land;
		}
		return null;
	}
	
	@Override
	public Map<Integer, Land> getAll(){
		return new HashMap<Integer, Land>(lands);
	}

	@SuppressWarnings("serial")
	@Override
	public void save(){
		HashMap<Integer, LinkedHashMap<String, Object>> saves = new LinkedHashMap<Integer, LinkedHashMap<String, Object>>();
		lands.values().forEach((land) -> {
			saves.put(land.getId(), new LinkedHashMap<String, Object>(){
				{
					Vector2 start = land.getStart();
					put("startX", (int) start.x);
					put("startZ", (int) start.y);
					
					Vector2 end = land.getEnd();
					put("endX", (int) end.x);
					put("endZ", (int) end.y);
					
					put("level", land.getLevel().getFolderName());
					
					put("price", land.getPrice());
					put("owner", land.getOwner());
					
					put("invitee", new ArrayList<String>());
					put("options", new LinkedHashMap<String, Object>());
				}
			});
		});
		
		try{
			DumperOptions option = new DumperOptions();
			option.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			
			Yaml yaml = new Yaml(option);
			Utils.writeFile(file, yaml.dump(saves));
			
			Map<String, Object> map = new HashMap<>();
			map.put("landId", landId);
			
			String content = new GsonBuilder().create().toJson(map);

			Utils.writeFile(new File(plugin.getDataFolder(), "LandData.json"), content);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void close(){
		this.save();
	}
}
