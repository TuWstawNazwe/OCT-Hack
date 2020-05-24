package me.zeroeightsix.kami.module.modules.combat;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import me.zeroeightsix.kami.event.events.PacketEvent;
import me.zeroeightsix.kami.event.events.RenderEvent;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.util.EntityUtil;
import me.zeroeightsix.kami.util.Friends;
import me.zeroeightsix.kami.util.KamiTessellator;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module.Info(name = "OctCA", category = Module.Category.COMBAT, description = "Places End Crystals to kill enemies")
public class AOccultHackCA extends Module {
    private Setting<Boolean> place;
    private Setting<Boolean> raytrace;
    private Setting<Boolean> autoSwitch;
    private Setting<Boolean> antiStuck;
    private Setting<Boolean> multiPlace;
    private Setting<Boolean> alert;
    private Setting<Boolean> antiSui;
    private Setting<Integer> attackSpeed;
    private Setting<Integer> placeDelay;
    private Setting<Integer> enemyRange;
    private Setting<Integer> minDamage;
    private Setting<Integer> facePlace;
    private Setting<Integer> multiPlaceSpeed;
    private Setting<Integer> placeRange;
    private Setting<Integer> breakRange;
    private BlockPos render;
    public boolean isActive = false;
    private Entity renderEnt;
    private long placeSystemTime;
    private long breakSystemTime;
    private long chatSystemTime;
    private long multiPlaceSystemTime;
    private long antiStuckSystemTime;
    private static boolean togglePitch;
    private boolean switchCooldown;
    private int newSlot;
    private int placements;
    private static boolean isSpoofingAngles;
    private static double yaw;
    private static double pitch;
    private EntityPlayer target;
    private Setting<Integer> Red = register(
            Settings.integerBuilder("Red").withMinimum(1).withMaximum(255).withValue(255));
    private Setting<Integer> Green = register(
            Settings.integerBuilder("Green").withMinimum(1).withMaximum(255).withValue(255));
    private Setting<Integer> Blue = register(
            Settings.integerBuilder("Blue").withMinimum(1).withMaximum(255).withValue(255));
    private Setting<Boolean> rainbow = register(Settings.b("Rainbow", true));

    @EventHandler
    private Listener<PacketEvent.Send> packetListener;

    public AOccultHackCA() {
        this.attackSpeed = this.register((Setting<Integer>) Settings.integerBuilder("AttackSpeed").withMinimum(0).withMaximum(20).withValue(10).build());
        this.placeDelay = this.register((Setting<Integer>) Settings.integerBuilder("PlaceDelay").withMinimum(0).withMaximum(50).withValue(0).build());
        this.enemyRange = this.register((Setting<Integer>) Settings.integerBuilder("EnemyRange").withMinimum(1).withMaximum(13).withValue(6).build());
        this.minDamage = this.register((Setting<Integer>) Settings.integerBuilder("MinDamage").withMinimum(0).withMaximum(16).withValue(5).build());
        this.facePlace = this.register((Setting<Integer>) Settings.integerBuilder("DPS").withMinimum(0).withMaximum(16).withValue(3).build());
        this.multiPlaceSpeed = this.register((Setting<Integer>) Settings.integerBuilder("MultiPlaceSpeed").withMinimum(1).withMaximum(10).withValue(0).build());
        this.placeRange = this.register((Setting<Integer>) Settings.integerBuilder("PlaceRange").withMinimum(1).withMaximum(6).withValue(6).build());
        this.breakRange = this.register((Setting<Integer>) Settings.integerBuilder("BreakRange").withMinimum(1).withMaximum(6).withValue(6).build());
        this.raytrace = this.register(Settings.b("RayTrace", false));
        this.antiStuck = this.register(Settings.b("AntiStuck", true));
        this.multiPlace = this.register(Settings.b("MultiPlace", false));
        this.place = this.register(Settings.b("Place", true));
        this.alert = this.register(Settings.b("ChatAlerts", true));
        this.autoSwitch = this.register(Settings.b("AutoSwitch", true));
        this.antiSui = this.register(Settings.b("AntiSuicide", false));

        this.placeSystemTime = -1L;
        this.breakSystemTime = -1L;
        this.chatSystemTime = -1L;
        this.multiPlaceSystemTime = -1L;
        this.antiStuckSystemTime = -1L;
        this.switchCooldown = false;
        this.placements = 0;
        final Packet[] packet = new Packet[1];
        this.packetListener = new Listener<PacketEvent.Send>(event -> {
            packet[0] = event.getPacket();
            if (packet[0] instanceof CPacketPlayer && AOccultHackCA.isSpoofingAngles) {
                ((CPacketPlayer) packet[0]).yaw = (float) AOccultHackCA.yaw;
                ((CPacketPlayer) packet[0]).pitch = (float) AOccultHackCA.pitch;
            }
        }, (Predicate<PacketEvent.Send>[]) new Predicate[0]);
    }

    @Override
    public void onUpdate() {
        final EntityEnderCrystal crystal = (EntityEnderCrystal) AOccultHackCA.mc.world.loadedEntityList.stream().filter(entity -> entity instanceof EntityEnderCrystal).map(entity -> entity).min(Comparator.comparing(c -> AOccultHackCA.mc.player.getDistance(c))).orElse(null);
        if (crystal != null && AOccultHackCA.mc.player.getDistance((Entity) crystal) <= this.breakRange.getValue()) {
            if (System.nanoTime() / 1000000L - this.breakSystemTime >= 420 - this.attackSpeed.getValue() * 20) {
                this.lookAtPacket(crystal.posX, crystal.posY, crystal.posZ, (EntityPlayer) AOccultHackCA.mc.player);
                AOccultHackCA.mc.playerController.attackEntity((EntityPlayer) AOccultHackCA.mc.player, (Entity) crystal);
                AOccultHackCA.mc.player.swingArm(EnumHand.MAIN_HAND);
                this.breakSystemTime = System.nanoTime() / 1000000L;
            }
            if (this.multiPlace.getValue()) {
                if (System.nanoTime() / 1000000L - this.multiPlaceSystemTime >= 20 * this.multiPlaceSpeed.getValue() && System.nanoTime() / 1000000L - this.antiStuckSystemTime <= 400 + (400 - this.attackSpeed.getValue() * 20)) {
                    this.multiPlaceSystemTime = System.nanoTime() / 1000000L;
                    return;
                }
            } else if (System.nanoTime() / 1000000L - this.antiStuckSystemTime <= 400 + (400 - this.attackSpeed.getValue() * 20)) {
                return;
            }
        } else {
            resetRotation();
        }
        int crystalSlot = (AOccultHackCA.mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL) ? AOccultHackCA.mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (AOccultHackCA.mc.player.inventory.getStackInSlot(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }
        boolean offhand = false;
        if (mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            offhand = true;
        } else if (crystalSlot == -1) {
            return;
        }
        Entity ent = null;
        Entity lastTarget = null;

        BlockPos finalPos = null;
        final List<BlockPos> blocks = this.findCrystalBlocks();
        final List<Entity> entities = new ArrayList<Entity>();
        entities.addAll((Collection<? extends Entity>) AOccultHackCA.mc.world.playerEntities.stream().filter(entityPlayer -> !Friends.isFriend(entityPlayer.getName())).collect(Collectors.toList()));
        double damage = 0.5;
        for (final Entity entity2 : entities) {
            if (entity2 != AOccultHackCA.mc.player) {
                if (((EntityLivingBase) entity2).getHealth() <= 0.0f) {
                    continue;

                }
                if (AOccultHackCA.mc.player.getDistanceSq(entity2) > this.enemyRange.getValue() * this.enemyRange.getValue()) {
                    continue;
                }
                for (final BlockPos blockPos : blocks) {
                    if (!canBlockBeSeen(blockPos) && AOccultHackCA.mc.player.getDistanceSq(blockPos) > 25.0 && this.raytrace.getValue()) {
                        continue;
                    }
                    final double b = entity2.getDistanceSq(blockPos);
                    if (b > 56.2) {
                        continue;
                    }
                    final double d = calculateDamage(blockPos.x + 0.5, blockPos.y + 1, blockPos.z + 0.5, entity2);
                    if (d < this.minDamage.getValue() && ((EntityLivingBase) entity2).getHealth() + ((EntityLivingBase) entity2).getAbsorptionAmount() > this.facePlace.getValue()) {
                        continue;
                    }
                    if (d <= damage) {
                        continue;
                    }
                    final double self = calculateDamage(blockPos.x + 0.5, blockPos.y + 1, blockPos.z + 0.5, (Entity) AOccultHackCA.mc.player);
                    if (this.antiSui.getValue()) {
                        if (AOccultHackCA.mc.player.getHealth() + AOccultHackCA.mc.player.getAbsorptionAmount() - self <= 7.0) {
                            continue;
                        }
                        if (self > d) {
                            continue;
                        }
                    }
                    damage = d;
                    finalPos = blockPos;
                    ent = entity2;
                    lastTarget = entity2;

                }
            }
        }
        if (damage == 0.5) {
            this.render = null;
            this.renderEnt = null;
            resetRotation();
            return;
        }
        this.render = finalPos;
        this.renderEnt = ent;

        if (this.place.getValue()) {
            if (!offhand && AOccultHackCA.mc.player.inventory.currentItem != crystalSlot) {
                if (this.autoSwitch.getValue()) {
                    AOccultHackCA.mc.player.inventory.currentItem = crystalSlot;
                    resetRotation();
                    this.switchCooldown = true;
                }
                return;
            }
            this.lookAtPacket(finalPos.x + 0.5, finalPos.y - 0.5, finalPos.z + 0.5, (EntityPlayer) AOccultHackCA.mc.player);
            final RayTraceResult result = AOccultHackCA.mc.world.rayTraceBlocks(new Vec3d(AOccultHackCA.mc.player.posX, AOccultHackCA.mc.player.posY + AOccultHackCA.mc.player.getEyeHeight(), AOccultHackCA.mc.player.posZ), new Vec3d(finalPos.x + 0.5, finalPos.y - 0.5, finalPos.z + 0.5));
            EnumFacing f;
            if (result == null || result.sideHit == null) {
                f = EnumFacing.UP;
            } else {
                f = result.sideHit;
            }
            if (this.switchCooldown) {
                this.switchCooldown = false;
                return;
            }
            if (System.nanoTime() / 1000000L - this.placeSystemTime >= this.placeDelay.getValue() * 2) {
                AOccultHackCA.mc.player.connection.sendPacket((Packet) new CPacketPlayerTryUseItemOnBlock(finalPos, f, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
                ++this.placements;
                this.antiStuckSystemTime = System.nanoTime() / 1000000L;
                this.placeSystemTime = System.nanoTime() / 1000000L;
            }
        }
        if (AOccultHackCA.isSpoofingAngles) {
            if (AOccultHackCA.togglePitch) {
                final EntityPlayerSP player = AOccultHackCA.mc.player;
                player.rotationPitch += (float) 4.0E-4;
                AOccultHackCA.togglePitch = false;
            } else {
                final EntityPlayerSP player2 = AOccultHackCA.mc.player;
                player2.rotationPitch -= (float) 4.0E-4;
                AOccultHackCA.togglePitch = true;
            }
        }
    }







    @Override
    public void onWorldRender(final RenderEvent event) {
        if (this.render != null) {
            final float[] hue = {(System.currentTimeMillis() % (360 * 32)) / (360f * 32)};
            int rgb = Color.HSBtoRGB(hue[0], 1, 1);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (rainbow.getValue()) {
                KamiTessellator.prepare(7);
            KamiTessellator.drawBox(this.render, r, g, b, 77, 63);
            KamiTessellator.release();
            KamiTessellator.prepare(7);
            KamiTessellator.drawBoundingBoxBlockPos(this.render, 1.00f,  r, g, b, 255);
            } else {
                KamiTessellator.prepare(7);
                KamiTessellator.drawBox(this.render, this.Red.getValue(), this.Green.getValue(), this.Blue.getValue(), 77, 63);
                KamiTessellator.release();
                KamiTessellator.prepare(7);
                KamiTessellator.drawBoundingBoxBlockPos(this.render, 1.00f, this.Red.getValue(), this.Green.getValue(), this.Blue.getValue(), 244);
            }
                KamiTessellator.release();
        }

    }

    private void lookAtPacket(final double px, final double py, final double pz, final EntityPlayer me) {
        final double[] v = EntityUtil.calculateLookAt(px, py, pz, me);
        setYawAndPitch((float)v[0], (float)v[1]);
    }

    private boolean canPlaceCrystal(final BlockPos blockPos) {
        final BlockPos boost = blockPos.add(0, 1, 0);
        final BlockPos boost2 = blockPos.add(0, 2, 0);
        return (AOccultHackCA.mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || AOccultHackCA.mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && AOccultHackCA.mc.world.getBlockState(boost).getBlock() == Blocks.AIR && AOccultHackCA.mc.world.getBlockState(boost2).getBlock() == Blocks.AIR && AOccultHackCA.mc.world.getEntitiesWithinAABB((Class)Entity.class, new AxisAlignedBB(boost)).isEmpty() && AOccultHackCA.mc.world.getEntitiesWithinAABB((Class)Entity.class, new AxisAlignedBB(boost2)).isEmpty();
    }

    public static BlockPos getPlayerPos() {
        return new BlockPos(Math.floor(AOccultHackCA.mc.player.posX), Math.floor(AOccultHackCA.mc.player.posY), Math.floor(AOccultHackCA.mc.player.posZ));
    }

    private List<BlockPos> findCrystalBlocks() {
        NonNullList positions = NonNullList.create();
        positions.addAll((Collection)this.getSphere(AOccultHackCA.getPlayerPos(), this.placeRange.getValue().floatValue(), this.placeRange.getValue(), false, true, 0).stream().filter(this::canPlaceCrystal).collect(Collectors.toList()));
        return (List<BlockPos>)positions;
    }

    public List<BlockPos> getSphere(final BlockPos loc, final float r, final int h, final boolean hollow, final boolean sphere, final int plus_y) {
        final List<BlockPos> circleblocks = new ArrayList<BlockPos>();
        final int cx = loc.getX();
        final int cy = loc.getY();
        final int cz = loc.getZ();
        for (int x = cx - (int)r; x <= cx + r; ++x) {
            for (int z = cz - (int)r; z <= cz + r; ++z) {
                for (int y = sphere ? (cy - (int)r) : cy; y < (sphere ? (cy + r) : ((float)(cy + h))); ++y) {
                    final double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? ((cy - y) * (cy - y)) : 0);
                    if (dist < r * r && (!hollow || dist >= (r - 1.0f) * (r - 1.0f))) {
                        final BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                }
            }
        }
        return circleblocks;
    }

    public static float calculateDamage(final double posX, final double posY, final double posZ, final Entity entity) {
        final float doubleExplosionSize = 12.0f;
        final double distancedsize = entity.getDistance(posX, posY, posZ) / doubleExplosionSize;
        final Vec3d vec3d = new Vec3d(posX, posY, posZ);
        final double blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        final double v = (1.0 - distancedsize) * blockDensity;
        final float damage = (float)(int)((v * v + v) / 2.0 * 7.0 * doubleExplosionSize + 1.0);
        double finald = 1.0;
        if (entity instanceof EntityLivingBase) {
            finald = getBlastReduction((EntityLivingBase)entity, getDamageMultiplied(damage), new Explosion((World) AOccultHackCA.mc.world, (Entity)null, posX, posY, posZ, 6.0f, false, true));
        }
        return (float)finald;
    }

    public static float getBlastReduction(final EntityLivingBase entity, float damage, final Explosion explosion) {
        if (entity instanceof EntityPlayer) {
            final EntityPlayer ep = (EntityPlayer)entity;
            final DamageSource ds = DamageSource.causeExplosionDamage(explosion);
            damage = CombatRules.getDamageAfterAbsorb(damage, (float)ep.getTotalArmorValue(), (float)ep.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
            final int k = EnchantmentHelper.getEnchantmentModifierDamage(ep.getArmorInventoryList(), ds);
            final float f = MathHelper.clamp((float)k, 0.0f, 20.0f);
            damage *= 1.0f - f / 25.0f;
            if (entity.isPotionActive(Potion.getPotionById(11))) {
                damage -= damage / 4.0f;
            }
            return damage;
        }
        damage = CombatRules.getDamageAfterAbsorb(damage, (float)entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        return damage;
    }

    private static float getDamageMultiplied(final float damage) {
        final int diff = AOccultHackCA.mc.world.getDifficulty().getId();
        return damage * ((diff == 0) ? 0.0f : ((diff == 2) ? 1.0f : ((diff == 1) ? 0.5f : 1.5f)));
    }

    public static float calculateDamage(final EntityEnderCrystal crystal, final Entity entity) {
        return calculateDamage(crystal.posX, crystal.posY, crystal.posZ, entity);
    }

    public static boolean canBlockBeSeen(final BlockPos blockPos) {
        return AOccultHackCA.mc.world.rayTraceBlocks(new Vec3d(AOccultHackCA.mc.player.posX, AOccultHackCA.mc.player.posY + AOccultHackCA.mc.player.getEyeHeight(), AOccultHackCA.mc.player.posZ), new Vec3d((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ()), false, true, false) == null;
    }

    private static void setYawAndPitch(final float yaw1, final float pitch1) {
        AOccultHackCA.yaw = yaw1;
        AOccultHackCA.pitch = pitch1;
        AOccultHackCA.isSpoofingAngles = true;
    }

    private static void resetRotation() {
        if (AOccultHackCA.isSpoofingAngles) {
            AOccultHackCA.yaw = AOccultHackCA.mc.player.rotationYaw;
            AOccultHackCA.pitch = AOccultHackCA.mc.player.rotationPitch;
            AOccultHackCA.isSpoofingAngles = false;
        }
    }

    @Override
    protected int onEnable() {

        if (this.alert.getValue() && AOccultHackCA.mc.world != null) {
            sendRawChatMessage("\u00A7aAutoCrystal ON");
        }
        return 0;
    }

    public int onDisable() {
        if (this.alert.getValue() && AOccultHackCA.mc.world != null) {
            sendRawChatMessage("\u00A7cAutoCrystal" + ChatFormatting.RED.toString() + "OFF");
        }
        this.render = null;
        resetRotation();
        return 0;
    }

    private void sendRawChatMessage(String s) {
    }

    static {
        AOccultHackCA.togglePitch = false;
    }
}
