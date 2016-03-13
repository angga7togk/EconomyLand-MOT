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

import java.util.Map;

import me.onebone.economyland.Land;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;

public interface Provider{
	public void addLand(Vector2 start, Vector2 end, Level level, double price, String owner);
	public boolean removeLand(int id);
	public Land getLand(int id);
	public Land findLand(Position pos);
	public Land checkOverlap(Position start, Position end);
	public Map<Integer, Land> getAll();
	
	public void save();
	public void close();
}
