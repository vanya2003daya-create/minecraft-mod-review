package com.adam.firemagic.upgrade;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

public class PickaxeResourcesData {
    private int coal;
    private int iron;
    private int gold;
    private int diamond;
    private int emerald;

    public static PickaxeResourcesData fromNbt(NbtCompound nbt) {
        PickaxeResourcesData data = new PickaxeResourcesData();
        data.coal = nbt.getInt("coal");
        data.iron = nbt.getInt("iron");
        data.gold = nbt.getInt("gold");
        data.diamond = nbt.getInt("diamond");
        data.emerald = nbt.getInt("emerald");
        return data;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("coal", coal);
        nbt.putInt("iron", iron);
        nbt.putInt("gold", gold);
        nbt.putInt("diamond", diamond);
        nbt.putInt("emerald", emerald);
        return nbt;
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(coal);
        buf.writeInt(iron);
        buf.writeInt(gold);
        buf.writeInt(diamond);
        buf.writeInt(emerald);
    }

    public static PickaxeResourcesData read(PacketByteBuf buf) {
        PickaxeResourcesData data = new PickaxeResourcesData();
        data.coal = buf.readInt();
        data.iron = buf.readInt();
        data.gold = buf.readInt();
        data.diamond = buf.readInt();
        data.emerald = buf.readInt();
        return data;
    }

    public int getResource(String category) {
        return switch (category) {
            case "coal" -> coal;
            case "iron" -> iron;
            case "gold" -> gold;
            case "diamond" -> diamond;
            case "emerald" -> emerald;
            default -> 0;
        };
    }

    public void setResource(String category, int value) {
        switch (category) {
            case "coal" -> coal = value;
            case "iron" -> iron = value;
            case "gold" -> gold = value;
            case "diamond" -> diamond = value;
            case "emerald" -> emerald = value;
        }
    }
}