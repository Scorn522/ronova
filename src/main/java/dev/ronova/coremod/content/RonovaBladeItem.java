package dev.ronova.coremod.content;

import dev.ronova.coremod.authority.AuthorityBinder;
import dev.ronova.coremod.network.RonovaNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public final class RonovaBladeItem extends Item {
    public RonovaBladeItem() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer owner) {
            RonovaNetwork.executeTargeted(owner, target, "item-hit");
        }
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        // Raw GLFW input plus acknowledged packets are the authoritative lane.
        // Running the full action here as well can cross a tick boundary and
        // evaluate the same target twice.
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if(level.isClientSide())net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            ()->()->dev.ronova.coremod.client.ClientRuntime.beginBladeBlock(player,stack,hand));
        return InteractionResultHolder.consume(stack);
    }
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack){return net.minecraft.world.item.UseAnim.BLOCK;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining) {
        if(level.isClientSide()&&entity instanceof Player player)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            ()->()->dev.ronova.coremod.client.ClientRuntime.bladeBlockReleased(player,stack));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            AuthorityBinder.restore(player);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
