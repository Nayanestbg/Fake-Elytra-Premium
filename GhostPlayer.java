package nevercry.larp.flight;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

/**
 * Copie de ton joueur qui n'existe que dans ton monde client (jamais envoyee au serveur).
 * Elle est toujours en pose de vol plane, avec une elytre visible sur le dos.
 */
public class GhostPlayer extends OtherClientPlayerEntity {

    /** Index du drapeau "vol plane" dans les flags d'entite. */
    private static final int GLIDING_FLAG = 7;

    private final ClientPlayerEntity source;

    public GhostPlayer(ClientWorld world, ClientPlayerEntity source, int entityId) {
        super(world, new GameProfile(UUID.randomUUID(), "Ghost"));
        this.source = source;

        this.setId(entityId);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);

        // Meme tenue que ton vrai joueur, mais le plastron est remplace par une elytre.
        this.equipStack(EquipmentSlot.HEAD, source.getEquippedStack(EquipmentSlot.HEAD).copy());
        this.equipStack(EquipmentSlot.LEGS, source.getEquippedStack(EquipmentSlot.LEGS).copy());
        this.equipStack(EquipmentSlot.FEET, source.getEquippedStack(EquipmentSlot.FEET).copy());
        this.equipStack(EquipmentSlot.MAINHAND, source.getEquippedStack(EquipmentSlot.MAINHAND).copy());
        this.equipStack(EquipmentSlot.OFFHAND, source.getEquippedStack(EquipmentSlot.OFFHAND).copy());
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));

        this.setFlag(GLIDING_FLAG, true);
        this.setPose(EntityPose.GLIDING);
    }

    /** Le fantome utilise ton skin (le profil aleatoire n'a pas d'entree dans la liste des joueurs). */
    @Override
    public SkinTextures getSkin() {
        return this.source.getSkin();
    }

    /** Meme couches de skin (veste, chapeau...) que ton vrai joueur. */
    @Override
    public boolean isModelPartVisible(PlayerModelPart part) {
        return this.source.isModelPartVisible(part);
    }

    /** Evite que le fantome pousse ton vrai joueur (ce qui l'obligerait a bouger cote serveur). */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.setFlag(GLIDING_FLAG, true);
        this.setPose(EntityPose.GLIDING);
    }

    /** Meme logique que Entity.changeLookDirection, appliquee au fantome (rotation fluide a la souris). */
    public void applyLook(double cursorDeltaX, double cursorDeltaY) {
        float pitchDelta = (float) cursorDeltaY * 0.15F;
        float yawDelta = (float) cursorDeltaX * 0.15F;

        this.setPitch(MathHelper.clamp(this.getPitch() + pitchDelta, -90.0F, 90.0F));
        this.setYaw(this.getYaw() + yawDelta);
        this.lastPitch = MathHelper.clamp(this.lastPitch + pitchDelta, -90.0F, 90.0F);
        this.lastYaw += yawDelta;
    }

    /** Deplace le fantome d'un tick, en gardant les anciennes valeurs pour l'interpolation. */
    public void moveTo(double x, double y, double z, double vx, double vy, double vz) {
        this.lastX = this.getX();
        this.lastY = this.getY();
        this.lastZ = this.getZ();

        this.setPosition(x, y, z);
        this.setVelocity(vx, vy, vz);
        this.setOnGround(false);

        this.lastBodyYaw = this.bodyYaw;
        this.lastHeadYaw = this.headYaw;
        this.bodyYaw = this.getYaw();
        this.headYaw = this.getYaw();
    }
}
