package dev.lone.rpghuds.core.data;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper;
import dev.lone.rpghuds.Main;
import dev.lone.rpghuds.core.settings.StarsSettings;
import me.clip.placeholderapi.PlaceholderAPI;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class StarsHud extends PAPIHud<StarsSettings>
{
    private static boolean HAS_CHECKED_PLACEHOLDER = false;

    private final Player player;

    private double prevBalance;
    private String prevAmount;

    @Nullable
    BukkitTask arrowRemoveSchedule;
    @Nullable
    private FontImageWrapper currentArrow;

    private PlayerPointsAPI ppAPI = Main.inst().getPpAPI();

    public StarsHud(String placeholder,
                    PlayerHudsHolderWrapper holder,
                    StarsSettings settings) throws NullPointerException
    {
        super(placeholder, holder, settings);
        this.player = holder.getPlayer();

        this.prevBalance = ppAPI.look(player.getUniqueId());

        hud.setVisible(true);
    }

    @Override
    public RenderAction refreshRender()
    {
        return refreshRender(false);
    }

    @Override
    public RenderAction refreshRender(boolean forceRender)
    {
        if (hidden)
            return RenderAction.HIDDEN;

        if (!hudSettings.isEnabledInWorld(player.getWorld()))
        {
            hud.setVisible(false); //I think this will cause problems
            return RenderAction.HIDDEN;
        }

        if(ppAPI != null)
        {
            double balance = ppAPI.look(player.getUniqueId());
            if(balance != prevBalance)
            {
                if (balance > prevBalance)
                    currentArrow = hudSettings.char_arrow_up;
                else
                    currentArrow = hudSettings.char_arrow_down;

                prevBalance = balance;

                if(arrowRemoveSchedule != null)
                    arrowRemoveSchedule.cancel();
                arrowRemoveSchedule = Bukkit.getScheduler().runTaskLaterAsynchronously(Main.inst(), () -> {
                    currentArrow = null;
                    arrowRemoveSchedule.cancel();
                    arrowRemoveSchedule = null;
                    refreshRender(true);
                }, 20 * 3);
            }
        }

        //TODO: better abstract logic: HudDataProvider ???
        String amount = PlaceholderAPI.setPlaceholders(holder.getPlayer(), placeholder);
        if (!forceRender && currentArrow == null && amount.equals(prevAmount))
            return RenderAction.SAME_AS_BEFORE;

        //TODO: Shit, recode this.
        // PAPI doesn't allow me to preemptively check if a placeholder is working or not.
        if(!HAS_CHECKED_PLACEHOLDER && amount.equals(placeholder))
        {
            Main.inst().getLogger().severe(
                    ChatColor.RED +
                            "Failed to replace PAPI placeholder for player " + player.getName() + ". '" + placeholder + "' probably doesn't exists. " +
                            "Check RPGhuds/config.yml file and check if you have the correct economy plugin installed."
            );
            prevAmount = amount;
            HAS_CHECKED_PLACEHOLDER = true;
            return RenderAction.HIDDEN;
        }

        imgsBuffer.clear();

        if(currentArrow != null)
            imgsBuffer.add(currentArrow);

        hudSettings.appendAmountToImages(amount, imgsBuffer);
        imgsBuffer.add(hudSettings.icon);

        hud.setFontImages(imgsBuffer);
        adjustOffset();

        prevAmount = amount;

        return RenderAction.SEND_REFRESH;
    }

    @Override
    public void deleteRender()
    {
        hud.clearFontImagesAndRefresh();

        if (arrowRemoveSchedule != null)
            arrowRemoveSchedule.cancel();
        arrowRemoveSchedule = null;
    }
}
