package thunder.hack.core.impl;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.IManager;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.modules.Module;
import thunder.hack.modules.combat.Aura;

public class PlayerManager implements IManager {
    public float serverYaw, serverPitch;
    private float yaw, pitch;
    public float lastYaw, lastPitch;
    public double currentPlayerSpeed;
    public int ticksElytraFlying;
    public int serverSideSlot = 0;

    //Мы можем зайти в инвентарь, и сервер этого не узнает, пока мы не начнем кликать
    //Юзать везде!
    public boolean inInventory;

    @EventHandler
    public void onSync(EventSync event) {
        if (Module.fullNullCheck()) return;

        yaw = MC.player.getYaw();
        pitch = MC.player.getPitch();
        lastYaw = ((IClientPlayerEntity) MC.player).getLastYaw();
        lastPitch = ((IClientPlayerEntity) MC.player).getLastPitch();

        if (MC.currentScreen == null) inInventory = false;
        if (MC.player.isFallFlying() && MC.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            ticksElytraFlying++;
        } else ticksElytraFlying = 0;
    }

    @EventHandler
    public void onTick(EventTick e) {
        currentPlayerSpeed = Math.hypot(MC.player.getX() - MC.player.prevX, MC.player.getZ() - MC.player.prevZ);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void postSync(EventPostSync event) {
        if (Module.fullNullCheck()) return;
        MC.player.setYaw(this.yaw);
        MC.player.setPitch(this.pitch);
    }

    @EventHandler
    public void onSyncWithServer(PacketEvent.@NotNull Send event) {
        if (event.getPacket() instanceof ClickSlotC2SPacket) {
            inInventory = true;
        }
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket slot) {
            serverSideSlot = slot.getSelectedSlot();
        }
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket) {
            inInventory = false;
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket move) {
            serverYaw = move.getYaw(serverYaw);
            serverPitch = move.getPitch(serverPitch);
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.PositionAndOnGround move) {
            serverYaw = move.getYaw(serverYaw);
            serverPitch = move.getPitch(serverPitch);
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround move) {
            serverYaw = move.getYaw(serverYaw);
            serverPitch = move.getPitch(serverPitch);
        }
        if (event.getPacket() instanceof PlayerMoveC2SPacket.Full move) {
            serverYaw = move.getYaw(serverYaw);
            serverPitch = move.getPitch(serverPitch);
        }
    }

    @EventHandler
    public void getServerPosLook(PacketEvent.@NotNull Receive event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket posLook) {
            serverYaw = posLook.getYaw();
            serverPitch = posLook.getPitch();
        }
    }

    public boolean checkRtx(float yaw, float pitch, float distance, boolean ignoreWalls, Aura.RayTrace rt) {
        Entity targetedEntity;
        HitResult result = ignoreWalls ? null : rayTrace(distance, yaw, pitch);
        Vec3d vec3d = MC.player.getPos().add(0, MC.player.getEyeHeight(MC.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);
        if (result != null) distancePow2 = result.getPos().squaredDistanceTo(vec3d);
        Vec3d vec3d2 = getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        Box box = MC.player.getBoundingBox().stretch(vec3d2.multiply(distance)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(MC.player, vec3d, vec3d3, box, (entity) -> !entity.isSpectator(), distancePow2);
        if (entityHitResult != null) {
            Entity entity2 = entityHitResult.getEntity();
            Vec3d vec3d4 = entityHitResult.getPos();
            double g = vec3d.squaredDistanceTo(vec3d4);
            if (g < distancePow2 || result == null) {
                targetedEntity = entity2;
                if (targetedEntity == Aura.target || rt == Aura.RayTrace.OnlyTarget) return true;

            }
        }
        return false;
    }

    public Entity getRtxTarger(float yaw, float pitch, float distance, boolean ignoreWalls) {
        Entity targetedEntity = null;
        HitResult result = ignoreWalls ? null : rayTrace(distance, yaw, pitch);
        Vec3d vec3d = MC.player.getPos().add(0, MC.player.getEyeHeight(MC.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);
        if (result != null) distancePow2 = result.getPos().squaredDistanceTo(vec3d);
        Vec3d vec3d2 = getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        Box box = MC.player.getBoundingBox().stretch(vec3d2.multiply(distance)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(MC.player, vec3d, vec3d3, box, (entity) -> !entity.isSpectator(), distancePow2);
        if (entityHitResult != null) {
            Entity entity2 = entityHitResult.getEntity();
            Vec3d vec3d4 = entityHitResult.getPos();
            double g = vec3d.squaredDistanceTo(vec3d4);
            if (g < distancePow2 || result == null) {
                if (entity2 instanceof LivingEntity) {
                    targetedEntity = entity2;
                    return targetedEntity;
                }
            }
        }
        return targetedEntity;
    }

    public HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3d vec3d = MC.player.getCameraPosVec(MC.getTickDelta());
        Vec3d vec3d2 = getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return MC.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, MC.player));
    }

    public static float[] calcAngle(Vec3d to) {
        if (to == null) return null;
        double difX = to.x - MC.player.getEyePos().x;
        double difY = (to.y - MC.player.getEyePos().y) * -1.0;
        double difZ = to.z - MC.player.getEyePos().z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))};
    }

    public static Vec2f calcAngleVec(Vec3d to) {
        if (to == null) return null;
        double difX = to.x - MC.player.getEyePos().x;
        double difY = (to.y - MC.player.getEyePos().y) * -1.0;
        double difZ = to.z - MC.player.getEyePos().z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        return new Vec2f((float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))));
    }

    private Vec3d getRotationVector(float yaw, float pitch) {
        return new Vec3d(MathHelper.sin(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F), -MathHelper.sin(yaw * 0.017453292F), MathHelper.cos(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F));
    }

    public static float[] calcAngle(Vec3d from, Vec3d to) {
        if (to == null) return null;
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist)))};
    }
}