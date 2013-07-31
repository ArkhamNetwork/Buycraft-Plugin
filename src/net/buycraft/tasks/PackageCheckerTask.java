package net.buycraft.tasks;

import net.buycraft.Plugin;
import net.buycraft.api.ApiTask;
import net.buycraft.util.Chat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class PackageCheckerTask extends ApiTask {
    private Plugin plugin;

    private Boolean manualExecution;

    private static long lastExecution;

    public static long getLastExecution() {
        return lastExecution;
    }

    public static void call(Boolean manualExecution) {
        Plugin.getInstance().addTask(new PackageCheckerTask(manualExecution));
    }

    private PackageCheckerTask(Boolean manualExecution) {
        this.plugin = Plugin.getInstance();
        this.manualExecution = manualExecution;
    }

    public void run() {
        try {
            lastExecution = System.currentTimeMillis();
            if (plugin.isAuthenticated(null)) {
                if (plugin.getSettings().getBoolean("commandChecker") || manualExecution) {
                	// Fetch online player list
                	Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
                    if (onlinePlayers.length > 0 || manualExecution) {
                    	// Put players in a HashMap for quick access
                    	HashMap<String, Player> onlinePlayerSet = new HashMap<String, Player>(onlinePlayers.length);
                    	for (Player player : onlinePlayers) {
                    		onlinePlayerSet.put(player.getName().toLowerCase(), player);
                    	}
                    	
                        JSONObject apiResponse = plugin.getApi().commandsGetAction();
                        
                        	if (apiResponse != null && apiResponse.getInt("code") == 0) {
                        		JSONObject apiPayload = apiResponse.getJSONObject("payload");
                        		JSONArray commandsPayload = apiPayload.getJSONArray("commands");

                        		LinkedHashSet<String> executedCommands = new LinkedHashSet<String>();

                        		for (int i = 0; i < commandsPayload.length(); i++) {
                        			JSONObject row = commandsPayload.getJSONObject(i);

                        			String username = row.getString("ign").toLowerCase();
                        			Boolean requireOnline = row.getBoolean("requireOnline");
                        			String command = row.getJSONArray("commands").getString(0);
                                    int delay = row.getInt("delay");

                        			if (requireOnline == false || onlinePlayerSet.containsKey(username)) {
                        				executedCommands.add(username);
                        					
                        				String c = command;
                        				String u = username;
                        				
                        				Plugin.getInstance().getCommandExecutor().queueCommand(c, u, delay);
                        			}
                        		}
                        		
                        		// If the plugin is disabled here our commands won't get executed so we return
                        		if (!Plugin.getInstance().isEnabled()) {
                        		    return;
                        		}
                        		
                        		Plugin.getInstance().getCommandExecutor().scheduleExecutor();

                        		if (executedCommands.size() > 0) {
                        			for (String username : executedCommands) {
                        				Player player = onlinePlayerSet.get(username);

                        				if (player != null) {

                        					player.sendMessage(new String[] {
                        							Chat.header(), 
                        							Chat.seperator(),
                        							Chat.seperator() + ChatColor.GREEN + plugin.getLanguage().getString("commandsExecuted"),
                        							Chat.seperator(), 
                        							Chat.footer()
                        					});
                        				}
                        			}

                        			CommandDeleteTask.call(new JSONArray(executedCommands.toArray()).toString());
                        		}

                        		plugin.getLogger().info("Package checker successfully executed.");
                        	} else {
                        		plugin.getLogger().severe("No response/invalid key during package check.");
                        	}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ReportTask.setLastException(e);
        }
    }
}
