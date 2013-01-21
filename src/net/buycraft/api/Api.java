package net.buycraft.api;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import net.buycraft.Plugin;
import net.buycraft.util.Updater;

public class Api 
{
	private Plugin plugin;
	
	private String apiUrl;
	private String apiKey;
	
	public Api()
	{
		this.plugin = Plugin.getInstance();
		this.apiKey = plugin.getSettings().getString("secret");
		
		if(plugin.getSettings().getBoolean("https"))
		{
			this.apiUrl = "https://api.buycraft.net/v3";
		}
		else
		{
			this.apiUrl = "http://api.buycraft.net/v3";
		}
	}

	public Boolean infoAction()
	{
		try 
		{
			HashMap<String, String> apiCall = new HashMap<String, String>();
			
			apiCall.put("action", "info");
						
			JSONObject apiResponse = call(apiCall);
		
			if(apiResponse != null)
			{
				if(apiResponse.getInt("code") == 0)
				{
					JSONObject payload = apiResponse.getJSONObject("payload");
					
					plugin.setServerID(payload.getInt("serverId"));
					plugin.setServerCurrency(payload.getString("serverCurrency"));
					plugin.setServerStore(payload.getString("serverStore"));
					
					if(payload.getDouble("latestVersion") > Double.valueOf(plugin.getVersion()))
					{
						String downloadUrl = payload.getString("latestDownload");

						if(plugin.getSettings().getBoolean("autoUpdate"))
						{
							Updater updater = new Updater();
							updater.download(downloadUrl);
						}
						else
						{
							plugin.getLogger().info("Ignoring update due to auto update disabled.");
						}
					}
					
					return true;
				}
				else if(apiResponse.getInt("code") == 101)
				{
					plugin.getLogger().severe("Could not find an account with the specified secret key! Please follow the installation tutorial or read the README.txt file for more information.");
					
					return false;
				}
			}
		} 
		catch(JSONException e)
		{
			plugin.getLogger().severe("JSON parsing error.");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		plugin.getLogger().severe("Unexpected error occured in the authentication process.");
		
		return false;
	}
	
	public JSONObject packagesAction()
	{
		HashMap<String, String> apiCallParams = new HashMap<String, String>();
		
		apiCallParams.put("action", "packages");
		
		return call(apiCallParams);
	}

	public JSONObject commandsGetAction()
	{
		HashMap<String, String> apiCallParams = new HashMap<String, String>();
		
		apiCallParams.put("action", "commands");
		apiCallParams.put("do", "lookup");
		
		return call(apiCallParams);
	}

	public void commandsDeleteAction(String commandsToDelete)
	{
		HashMap<String, String> apiCallParams = new HashMap<String, String>();
		
		apiCallParams.put("action", "commands");
		apiCallParams.put("do", "remove");
		
		apiCallParams.put("commands", commandsToDelete);
		
		call(apiCallParams);
	}
	
	private JSONObject call(HashMap<String, String> apiCallParams)
	{
		if(apiKey.length() == 0)
		{
			apiKey = "unspecified";
		}
		
		apiCallParams.put("secret", apiKey);
		apiCallParams.put("version", String.valueOf(plugin.getVersion()));
		
		String url = apiUrl + generateUrlQueryString(apiCallParams);
		
		if(url != null)
		{
			String HTTPResponse = HttpRequest(url);
			
			try 
			{
				if(HTTPResponse != null)
				{
					return new JSONObject(HTTPResponse);
				}
				else
				{
					return null;
				}
			} 
			catch (JSONException e) 
			{
				plugin.getLogger().severe("JSON parsing error.");
			}
		}
		
		return null;
	}
	
	public String HttpRequest(String url)
	{
		try
		{
			String content = "";
			
			URL conn = new URL(url);
	        URLConnection yc = conn.openConnection();
	        
	        yc.setConnectTimeout(10000);
	        yc.setReadTimeout(10000);
	        
	        BufferedReader in;
			
			in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

	        String inputLine;
	       
			while ((inputLine = in.readLine()) != null) 
			{
				content = content + inputLine;
			}
			
			in.close();

			return content;
		}
		catch(ConnectException e)
		{
			plugin.getLogger().severe("HTTP request failed due to connection error.");
		}
		catch(SocketTimeoutException e)
		{
			plugin.getLogger().severe("HTTP request failed due to timeout error.");
		}
		catch(FileNotFoundException e)
		{
			plugin.getLogger().severe("HTTP request failed due to file not found.");
		}
		catch(UnknownHostException e)
		{
			plugin.getLogger().severe("HTTP request failed due to unknown host.");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String generateUrlQueryString(HashMap<String, String> map)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("?");
		
        for (Map.Entry<String, String> entry : map.entrySet()) 
        {
            if (sb.length() > 1) 
            {
                sb.append("&");
            }
            
            sb.append(String.format("%s=%s",
            	entry.getKey().toString(),
            	entry.getValue().toString()
            ));
        }

        return sb.toString();  
	}
}
