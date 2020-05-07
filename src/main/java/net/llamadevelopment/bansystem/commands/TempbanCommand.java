package net.llamadevelopment.bansystem.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.network.protocol.ScriptCustomEventPacket;
import cn.nukkit.scheduler.AsyncTask;
import net.llamadevelopment.bansystem.BanSystem;
import net.llamadevelopment.bansystem.Configuration;
import net.llamadevelopment.bansystem.components.api.BanSystemAPI;
import net.llamadevelopment.bansystem.components.api.SystemSettings;
import net.llamadevelopment.bansystem.components.data.Ban;
import net.llamadevelopment.bansystem.components.managers.database.Provider;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class TempbanCommand extends Command {

    public TempbanCommand(String name) {
        super(name, "Ban a player temporary.");
        commandParameters.put("default", new CommandParameter[]{
                new CommandParameter("player", CommandParamType.TARGET, false),
                new CommandParameter("timeType", false, new String[] {"days", "hours"}),
                new CommandParameter("time", CommandParamType.INT, false),
                new CommandParameter("reason", CommandParamType.TEXT, false)
        });
        setPermission("bansystem.command.tempban");
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        Provider api = BanSystemAPI.getProvider();
        SystemSettings settings = BanSystemAPI.getSystemSettings();
        if (sender.hasPermission(getPermission())) {
            if (args.length >= 4) {
                String player = args[0];
                if (args[1].equalsIgnoreCase("days") || args[1].equalsIgnoreCase("hours")) {
                    String timeString = args[1];
                    try {
                        int time = Integer.parseInt(args[2]);
                        int seconds = 0;
                        String reason = "";
                        for (int i = 3; i < args.length; ++i) reason = reason + args[i] + " ";
                        if (timeString.equalsIgnoreCase("days")) seconds = time * 86400;
                        if (timeString.equalsIgnoreCase("hours")) seconds = time * 3600;
                        if (api.playerIsBanned(player)) {
                            sender.sendMessage(Configuration.getAndReplace("PlayerIsBanned"));
                            return true;
                        }
                        String finalReason = reason;
                        int finalSeconds = seconds;
                        Server.getInstance().getScheduler().scheduleAsyncTask(BanSystem.getInstance(), new AsyncTask() {
                            @Override
                            public void onRun() {
                                api.banPlayer(player, finalReason, sender.getName(), finalSeconds);
                                sender.sendMessage(Configuration.getAndReplace("PlayerBanned", player));
                                if (settings.isWaterdog() && sender instanceof Player) {
                                    Player player1 = (Player) sender;
                                    Ban ban = api.getBan(player);
                                    ScriptCustomEventPacket customEventPacket = new ScriptCustomEventPacket();
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                                    try {
                                        dataOutputStream.writeUTF("banplayer");
                                        dataOutputStream.writeUTF(player);
                                        dataOutputStream.writeUTF(ban.getReason());
                                        dataOutputStream.writeUTF(ban.getBanID());
                                        dataOutputStream.writeUTF(api.getRemainingTime(ban.getTime()));
                                        customEventPacket.eventName = "bansystembridge:main";
                                        customEventPacket.eventData = outputStream.toByteArray();
                                        player1.dataPacket(customEventPacket);
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                Player onlinePlayer = Server.getInstance().getPlayer(player);
                                if (onlinePlayer != null) {
                                    Ban ban = api.getBan(player);
                                    onlinePlayer.kick(Configuration.getAndReplaceNP("BanScreen", ban.getBanID(), ban.getReason(), api.getRemainingTime(ban.getTime())), false);
                                }
                            }
                        });
                    } catch (NumberFormatException exception) {
                        sender.sendMessage(Configuration.getAndReplace("InvalidNumber"));
                    }
                } else sender.sendMessage(Configuration.getAndReplace("TempbanCommandUsage", getName()));
            } else sender.sendMessage(Configuration.getAndReplace("TempbanCommandUsage", getName()));
        } else sender.sendMessage(Configuration.getAndReplace("NoPermission"));
        return false;
    }
}
