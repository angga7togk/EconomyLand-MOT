package me.onebone.economyland.data;

import cn.nukkit.level.Position;
import lombok.Data;

@Data
public class LandSelection {
    public Position pos1 = null;
    public Position pos2 = null;
    public Long price = null;
}
