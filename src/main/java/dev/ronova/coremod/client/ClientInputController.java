package dev.ronova.coremod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ronova.coremod.content.DominionContent;
import dev.ronova.coremod.network.RonovaAction;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Raw mouse-edge capture and instant-mining targeting. */
public final class ClientInputController {
    private final ClientActionController actions;
    private final Consumer<BlockPos> breakSender;
    private boolean leftDown;
    private boolean rightDown;
    private boolean rightShifted;
    private final BladeBlockGesture blockGesture=new BladeBlockGesture();
    private LocalPlayer blockingPlayer;
    private net.minecraft.world.item.ItemStack blockingStack;
    private net.minecraft.world.InteractionHand blockingHand;
    private RonovaAction releasedAction;
    private boolean explicitMouseRelease;
    private BlockPos lastMinedBlock;

    public ClientInputController(
        ClientActionController actions,
        Consumer<BlockPos> breakSender
    ) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.breakSender = Objects.requireNonNull(breakSender, "breakSender");
    }

    public void pulse(Minecraft minecraft) {
        flushReleased(minecraft);
        LocalPlayer player = minecraft.player;
        long window = minecraft.getWindow().getWindow();
        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
            == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            == GLFW.GLFW_PRESS;
        boolean blade = hasBlade(player);
        boolean gameplay = minecraft.screen == null;
        if (blade && gameplay && left && !leftDown) {
            actions.queue(RonovaAction.PRIMARY);
        }
        if (blade && gameplay && left) {
            Vec3 eye = player.getEyePosition();
            BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
                eye, eye.add(player.getLookAngle().scale(64.0D)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos position = blockHit.getBlockPos();
                if (!position.equals(lastMinedBlock)) {
                    breakSender.accept(position);
                    lastMinedBlock = position.immutable();
                }
            }
        } else {
            lastMinedBlock = null;
        }
        if(right&&!rightDown)rightShifted=shifted(window);
        if(blockGesture.armed()&&!validBlock(minecraft))cancelBlock();
        if(!right&&rightDown)releaseBlock(minecraft);
        leftDown = left;
        rightDown = right;
    }

    /** Captures an edge before a stalled game tick can lose it. */
    public void onMouseButton(long window, int button, int action, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || window != minecraft.getWindow().getWindow()) {
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            leftDown = action == GLFW.GLFW_PRESS;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if(action==GLFW.GLFW_PRESS)rightShifted=(modifiers&GLFW.GLFW_MOD_SHIFT)!=0;
            rightDown = action == GLFW.GLFW_PRESS;
            if(action==GLFW.GLFW_RELEASE)releaseBlock(minecraft);
        }
        if (action != GLFW.GLFW_PRESS || minecraft.player == null || minecraft.level == null
            || minecraft.screen != null || !hasBlade(minecraft.player)) {
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            actions.queue(RonovaAction.PRIMARY);
        }
    }

    public void reset() {
        leftDown = false;
        rightDown = false;
        rightShifted = false;
        cancelBlock();
        lastMinedBlock = null;
    }

    private static boolean hasBlade(LocalPlayer player) {
        return player != null && (player.getMainHandItem().is(DominionContent.RONOVA_BLADE.get())
            || player.getOffhandItem().is(DominionContent.RONOVA_BLADE.get()));
    }
    public void beginBlock(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack,net.minecraft.world.InteractionHand hand) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.player!=player||minecraft.screen!=null||!minecraft.isWindowActive())return;
        if(blockingPlayer!=player||blockingStack!=stack||blockingHand!=hand)cancelBlock();
        blockingPlayer=minecraft.player;blockingStack=stack;blockingHand=hand;
        blockGesture.begin(rightDown?rightShifted:shifted(minecraft.getWindow().getWindow())||player.isShiftKeyDown());
    }
    private boolean validBlock(Minecraft minecraft) {
        return validContext(minecraft)&&blockingPlayer.isUsingItem()
            &&blockingPlayer.getUseItem()==blockingStack&&blockingStack.is(DominionContent.RONOVA_BLADE.get());
    }
    private boolean validContext(Minecraft minecraft) {
        return minecraft.player==blockingPlayer&&blockingPlayer!=null&&minecraft.level!=null&&minecraft.screen==null&&minecraft.isWindowActive()
            &&blockingPlayer.getItemInHand(blockingHand)==blockingStack&&blockingStack!=null&&blockingStack.is(DominionContent.RONOVA_BLADE.get());
    }
    private void releaseBlock(Minecraft minecraft) {
        if(releasedAction!=null){flushReleased(minecraft);return;}
        if(!blockGesture.armed())return;
        if(!validBlock(minecraft)||minecraft.gameMode==null){cancelBlock();return;}
        explicitMouseRelease=true;
        try{minecraft.gameMode.releaseUsingItem(blockingPlayer);}
        finally{explicitMouseRelease=false;}
        flushReleased(minecraft);
        if(releasedAction==null)cancelBlock();
    }
    public void blockReleased(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack) {
        var minecraft=Minecraft.getInstance();
        if(player!=blockingPlayer||stack!=blockingStack)return;
        boolean mouseReleased=explicitMouseRelease||GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_RELEASE;
        if(!validBlock(minecraft)||!mouseReleased){cancelBlock();return;}
        // Item.releaseUsing runs before LivingEntity clears its use state. Submit only after that state has ended.
        releasedAction=blockGesture.release(true);
    }
    private void flushReleased(Minecraft minecraft) {
        if(releasedAction==null)return;
        if(!validContext(minecraft)){cancelBlock();return;}
        if(blockingPlayer.isUsingItem())return;
        var action=releasedAction;cancelBlock();actions.queue(action);
    }
    private void cancelBlock(){blockGesture.cancel();blockingPlayer=null;blockingStack=null;blockingHand=null;releasedAction=null;}
    private static boolean shifted(long window){return InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT_SHIFT)||InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT_SHIFT);}
}
