package me.onebone.economyland;

import cn.nukkit.scheduler.PluginTask;

public class AutoSaveTask extends PluginTask<EconomyLand>{
	public AutoSaveTask(EconomyLand plugin){
		super(plugin);
	}

	@Override
	public void onRun(int currentTick){
		this.getOwner().save();
	}
}
