package dinglydell.tftechness.tileentities.machine;

import java.util.Random;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import cofh.core.network.PacketCoFHBase;
import cofh.core.util.fluid.FluidTankAdv;
import cofh.lib.util.helpers.ServerHelper;

import com.bioxx.tfc.Core.Metal.MetalRegistry;
import com.bioxx.tfc.api.HeatIndex;
import com.bioxx.tfc.api.HeatRegistry;
import com.bioxx.tfc.api.Metal;
import com.bioxx.tfc.api.TFC_ItemHeat;
import com.bioxx.tfc.api.Enums.EnumFuelMaterial;

import dinglydell.tftechness.TFTechness;
import dinglydell.tftechness.block.machine.BlockTFTMachine;
import dinglydell.tftechness.config.MetalConfig;
import dinglydell.tftechness.fluid.TFTFluids;
import dinglydell.tftechness.gui.GuiRFForge;
import dinglydell.tftechness.gui.container.ContainerRFForge;

public class TileRFForge extends TileTemperature {
	
	protected static final int[] tankCapacity = {
			4000, 8000, 16000, 32000
	};
	protected static final float specificHeat = 1.5f;
	protected static final int mass = 1200;
	protected static final float exposedSurfaceArea = 0.01f;
	protected static final float coolingExposedSurfaceArea = 1;
	
	// protected float internalTemperature = TFTechness.baseTemp;
	protected float targetTemperature = EnumFuelMaterial.COAL.burnTempMax;
	protected FluidTankAdv tankA = new FluidTankAdv(tankCapacity[0]);
	protected FluidTankAdv tankB = new FluidTankAdv(tankCapacity[0]);
	private boolean isCooling;
	
	public TileRFForge() {
		inventory = new ItemStack[6];
		internalTemperature = TFTechness.baseTemp;
	}
	
	@Override
	protected boolean shouldActivate() {
		return true;
	}
	
	@Override
	protected boolean shouldDeactivate() {
		return false;
	}
	
	@Override
	protected void onActivate() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onDeactivate() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public PacketCoFHBase getGuiPacket() {
		PacketCoFHBase packet = super.getGuiPacket();
		packet.addFluidStack(tankA.getFluid());
		packet.addFluidStack(tankB.getFluid());
		return packet;
		
	}
	
	@Override
	public void handleGuiPacket(PacketCoFHBase packet) {
		super.handleGuiPacket(packet);
		tankA.setFluid(packet.getFluidStack());
		tankB.setFluid(packet.getFluidStack());
		
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!ServerHelper.isClientWorld(this.worldObj)) {
			for (int i = 0; i < inventory.length; i++) {
				ItemStack is = inventory[i];
				if (is != null) {
					HeatIndex index = HeatRegistry.getInstance().findMatchingIndex(is);
					if (index != null && index.hasOutput()) {
						float temp = TFC_ItemHeat.getTemp(is);
						if (internalTemperature > temp) {
							temp += TFC_ItemHeat.getTempIncrease(is);
						} else
							temp -= TFC_ItemHeat.getTempDecrease(is);
						TFC_ItemHeat.setTemp(is, temp);
						
						if (temp > index.meltTemp) {
							Metal m = MetalRegistry.instance.getMetalFromItem(index.getOutputItem());
							if (m != null) {
								Fluid f = TFTFluids.metal.get(m.name);
								int amt = index.getOutput(new Random()).stackSize * MetalConfig.ingotFluidmB;
								FluidTankAdv tank;
								if (i < 3) {
									tank = tankA;
								} else {
									tank = tankB;
								}
								FluidStack fs = new FluidStack(f, amt);
								if (tank.fill(fs, false) == amt) {
									tank.fill(fs, true);
								}
								inventory[i] = null;
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected int spendEnergy(int rf) {
		float j = rf * TFTechness.rfToJoules;
		float tmp = j / (mass * specificHeat);
		if (internalTemperature + tmp > targetTemperature) {
			tmp = Math.max(0, targetTemperature - internalTemperature);
			internalTemperature += tmp;
			
			isCooling = tmp == 0;
			
			return (int) Math.ceil((tmp * mass * specificHeat) / TFTechness.rfToJoules);
		}
		isCooling = false;
		internalTemperature += tmp;
		return rf;
	}
	
	@Override
	public Object getGuiClient(InventoryPlayer inv) {
		return new GuiRFForge(inv, this);
		
	}
	
	@Override
	public Object getGuiServer(InventoryPlayer inv) {
		return new ContainerRFForge(inv, this);
	}
	
	@Override
	protected SideConfig getSideConfig() {
		
		SideConfig cfg = new SideConfig();
		cfg.numConfig = 8;
		cfg.slotGroups = new int[][] {
				new int[0], {
						0, 1
				}, {
						2, 3
				}, {
					4
				}, {
						2, 3, 4
				}, {
					0
				}, {
					1
				}, {
						0, 1, 2, 3, 4
				}
		};
		cfg.allowInsertionSide = new boolean[] {
				false, true, false, false, false, true, true, true
		};
		cfg.allowExtractionSide = new boolean[] {
				false, true, true, true, true, false, false, true
		};
		cfg.allowInsertionSlot = new boolean[] {
				true, true, false, false, false, false
		};
		cfg.allowExtractionSlot = new boolean[] {
				true, true, true, true, true, false
		};
		cfg.sideTex = new int[] {
				0, 1, 2, 3, 4, 5, 6, 7
		};
		cfg.defaultSides = new byte[] {
				3, 1, 2, 2, 2, 2
		};
		return cfg;
	}
	
	@Override
	protected EnergyConfig getEnergyConfig() {
		energyConsumption = 80;
		EnergyConfig cfg = new EnergyConfig();
		cfg.maxEnergy = 96000;
		cfg.maxPower = 500;
		return cfg;
	}
	
	@Override
	public int getType() {
		return BlockTFTMachine.Types.RFFORGE.ordinal();
	}
	
	@Override
	protected void onLevelChange() {
		super.onLevelChange();
		tankA.setCapacity(tankCapacity[level]);
		tankB.setCapacity(tankCapacity[level]);
	}
	
	@Override
	protected float getSurfaceArea() {
		return isCooling ? coolingExposedSurfaceArea : exposedSurfaceArea;
	}
	
	@Override
	protected float getSpecificHeat() {
		return specificHeat;
	}
	
	@Override
	protected int getMass() {
		return mass;
	}
	
	public FluidTankAdv getTankA() {
		return tankA;
	}
	
	public FluidTankAdv getTankB() {
		return tankB;
	}
}
