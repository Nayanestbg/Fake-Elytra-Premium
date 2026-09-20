package nevercry.larp.flight;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Freecam d'elytre 100 % cote client.
 *
 * Ton vrai personnage ne bouge pas et aucun paquet de mouvement n'est envoye :
 * une copie de ton joueur ("fantome") est ajoutee uniquement dans ton monde client,
 * pilotee avec la physique de l'elytre, et la camera la suit.
 */
public final class ElytraCam {

    /** Id d'entite negatif : ne peut pas entrer en collision avec un id envoye par le serveur. */
    private static final int GHOST_ID = -777001;

    private static final double GRAVITY = 0.08;
    private static final double START_SPEED = 0.8;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("fake-elytra", "main"));

    private static KeyBinding toggleKey;
    private static KeyBinding boostKey;

    private static boolean active;
    private static GhostPlayer ghost;
    private static ClientWorld ghostWorld;
    private static ClientPlayerEntity ghostOwner;
    private static Perspective previousPerspective;

    private static double x;
    private static double y;
    private static double z;
    private static double vx;
    private static double vy;
    private static double vz;
    private static int boostTicks;
    private static int hudTimer;

    private ElytraCam() {
    }

    public static void init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.fake-elytra.toggle", GLFW.GLFW_KEY_G, CATEGORY));
        boostKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.fake-elytra.boost", GLFW.GLFW_KEY_R, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(ElytraCam::tick);
    }

    public static boolean isActive() {
        return active;
    }

    /** Appele par le mixin de souris : la souris oriente le fantome au lieu de ton vrai joueur. */
    public static void look(double cursorDeltaX, double cursorDeltaY) {
        GhostPlayer g = ghost;
        if (g != null) {
            g.applyLook(cursorDeltaX, cursorDeltaY);
        }
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            if (active) {
                stop(client, "Retour dans ton corps.");
            } else {
                start(client);
            }
        }

        boolean boostRequested = false;
        while (boostKey.wasPressed()) {
            boostRequested = true;
        }

        if (!active) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.world != ghostWorld || player != ghostOwner) {
            // Deconnexion, changement de dimension, respawn... : on nettoie sans toucher l'ancien monde.
            reset(client);
            return;
        }

        if (!player.isAlive() || player.hurtTime > 0) {
            stop(client, "Degats subis : retour dans ton corps.");
            return;
        }

        if (client.isPaused()) {
            return;
        }

        flyTick(client, player, boostRequested);
    }

    private static void start(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null || active) {
            return;
        }

        x = player.getX();
        y = player.getY() + 1.0;
        z = player.getZ();

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        double[] look = lookVector(yaw, pitch);
        vx = look[0] * START_SPEED;
        vy = look[1] * START_SPEED;
        vz = look[2] * START_SPEED;
        boostTicks = 0;
        hudTimer = 0;

        GhostPlayer g = new GhostPlayer(world, player, GHOST_ID);
        g.refreshPositionAndAngles(x, y, z, yaw, pitch);
        g.setVelocity(vx, vy, vz);
        world.addEntity(g);

        ghost = g;
        ghostWorld = world;
        ghostOwner = player;
        active = true;

        previousPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        client.setCameraEntity(g);

        player.sendMessage(
                Text.literal("Freecam elytre ON  -  souris : diriger  |  R : fusee  |  G : retour")
                        .formatted(Formatting.AQUA),
                true);
    }

    private static void stop(MinecraftClient client, String message) {
        ClientWorld world = client.world;
        if (world != null && world == ghostWorld) {
            world.removeEntity(GHOST_ID, Entity.RemovalReason.DISCARDED);
        }
        reset(client);
        if (client.player != null && message != null) {
            client.player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), true);
        }
    }

    private static void reset(MinecraftClient client) {
        Entity cameraEntity = client.getCameraEntity();
        if (client.player != null && ghost != null && cameraEntity == ghost) {
            client.setCameraEntity(client.player);
        }
        if (previousPerspective != null) {
            client.options.setPerspective(previousPerspective);
            previousPerspective = null;
        }
        active = false;
        ghost = null;
        ghostWorld = null;
        ghostOwner = null;
        boostTicks = 0;
    }

    private static void flyTick(MinecraftClient client, ClientPlayerEntity player, boolean boostRequested) {
        GhostPlayer g = ghost;
        ClientWorld world = client.world;

        double[] look = lookVector(g.getYaw(), g.getPitch());
        double lx = look[0];
        double ly = look[1];
        double lz = look[2];

        // --- Fusee de feu d'artifice ---
        if (boostRequested) {
            boostTicks = 20 + ThreadLocalRandom.current().nextInt(13);
            world.playSoundClient(x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    SoundCategory.PLAYERS, 3.0F, 1.0F, false);
        }
        if (boostTicks > 0) {
            boostTicks--;
            vx += lx * 0.1 + (lx * 1.5 - vx) * 0.5;
            vy += ly * 0.1 + (ly * 1.5 - vy) * 0.5;
            vz += lz * 0.1 + (lz * 1.5 - vz) * 0.5;
            spawnSparks(world, lx, ly, lz);
        }

        // --- Physique de vol plane (meme formule que l'elytre vanilla) ---
        double pitchRad = Math.toRadians(g.getPitch());
        double lookHoriz = Math.sqrt(lx * lx + lz * lz);
        double velHoriz = Math.sqrt(vx * vx + vz * vz);
        double cosP = Math.cos(pitchRad);
        double liftScale = cosP * cosP;

        vy += GRAVITY * (-1.0 + liftScale * 0.75);
        if (vy < 0.0 && lookHoriz > 0.0) {
            double lift = vy * -0.1 * liftScale;
            vx += lx * lift / lookHoriz;
            vy += lift;
            vz += lz * lift / lookHoriz;
        }
        if (pitchRad < 0.0 && lookHoriz > 0.0) {
            double climb = velHoriz * -Math.sin(pitchRad) * 0.04;
            vx += -lx * climb / lookHoriz;
            vy += climb * 3.2;
            vz += -lz * climb / lookHoriz;
        }
        if (lookHoriz > 0.0) {
            vx += (lx / lookHoriz * velHoriz - vx) * 0.1;
            vz += (lz / lookHoriz * velHoriz - vz) * 0.1;
        }
        vx *= 0.99;
        vy *= 0.98;
        vz *= 0.99;

        // Pas de collision : c'est une camera libre.
        x += vx;
        y += vy;
        z += vz;
        g.moveTo(x, y, z, vx, vy, vz);

        // --- Indicateur dans la barre d'action ---
        hudTimer++;
        if (hudTimer % 2 == 0) {
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz) * 20.0;
            player.sendMessage(
                    Text.literal(String.format(Locale.ROOT,
                            "Vitesse %.0f m/s   |   Y %.0f   |   R : fusee   G : retour", speed, y))
                            .formatted(Formatting.AQUA),
                    true);
        }
    }

    private static void spawnSparks(ClientWorld world, double lx, double ly, double lz) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 3; i++) {
            world.addImportantParticleClient(ParticleTypes.FIREWORK,
                    x - lx * 0.6 + random.nextGaussian() * 0.05,
                    y + 0.3 - ly * 0.6 + random.nextGaussian() * 0.05,
                    z - lz * 0.6 + random.nextGaussian() * 0.05,
                    -vx * 0.2 + random.nextGaussian() * 0.05,
                    -vy * 0.2 + random.nextGaussian() * 0.05,
                    -vz * 0.2 + random.nextGaussian() * 0.05);
        }
    }

    /** Meme calcul que Entity.getRotationVector(pitch, yaw). */
    private static double[] lookVector(float yaw, float pitch) {
        double p = Math.toRadians(pitch);
        double h = Math.toRadians(-yaw);
        double cosP = Math.cos(p);
        return new double[] {Math.sin(h) * cosP, -Math.sin(p), Math.cos(h) * cosP};
    }
}
