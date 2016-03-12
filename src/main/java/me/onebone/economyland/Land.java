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

import java.util.HashMap;
import java.util.Map;

import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;

public class Land{
	private int id;
	private Vector2 start, end;
	private Level level;
	private double price;
	private String owner;
	
	private Map<String, Object> options;
	
	public Land(int id, Vector2 start, Vector2 end, Level level, double price, String owner, Map<String, Object> options){
		this.id = id;
		
		this.price = price;
		this.owner = owner;
		
		start = start.floor();
		end = end.floor();
		
		if(start.x > end.x){
			double tmp = start.x;
			start.x = end.x;
			end.x = tmp;
		}
		
		if(start.y > end.y){
			double tmp = start.y;
			start.y = end.y;
			end.y = tmp;
		}
		
		this.start = start;
		this.end = end;
		this.level = level;
		
		options.put("pvp", options.getOrDefault("pvp", false));
		options.put("pickup", options.getOrDefault("pickup", false));
		options.put("access", options.getOrDefault("access", true));
		
		this.options = new HashMap<String, Object>(options);
	}
	
	public boolean check(Position pos){
		return pos.level == this.level
				&& (this.start.x <= pos.x && pos.x <= this.end.x)
				&& (this.start.y <= pos.z && pos.z <= this.end.y);
	}
	
	public Vector2 getStart(){
		return new Vector2(start.x, start.y);
	}
	
	public Vector2 getEnd(){
		return new Vector2(end.x, end.y);
	}
	
	public Level getLevel(){
		return this.level;
	}
	
	public double getPrice(){
		return this.price;
	}
	
	public String getOwner(){
		return this.owner;
	}
	
	public int getId(){
		return this.id;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getOption(String key, T defaultValue){
		return (T) this.options.getOrDefault(key, defaultValue);
	}
}
