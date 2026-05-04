import discord
from discord import app_commands
from discord.ext import commands, tasks
import requests

bot_token = "BOT_TOKEN" # Change this to your bot's token
zipcord_token = "zipcord" # Default Zipcord token
zipcord_url = "http://localhost:21960/" # Default Zipcord port

default_link_time = 600 # 600 seconds = 10 minutes
update_time = 10 # Status updates every 10 seconds

### BEGIN BOT CODE ###

default = {'token':zipcord_token}

class MyBot(discord.Client):
    def __init__(self):
        super().__init__(intents=discord.Intents.default())
        self.tree = app_commands.CommandTree(self)

    async def setup_hook(self):
        await self.tree.sync()

bot = MyBot()

@bot.tree.command(name="link", description="Generates a link code to link your Discord account on the Minecraft server.")
async def link(interaction: discord.Interaction):
    json = default | {"tags":str(interaction.user.id)}
    response = requests.get(zipcord_url + "link/find-username", json=json)

    msg = "There was an error with the command. Please try again later."

    if response.text:
        first = response.text.split(",")[0]
        msg = f"You are already linked to {first}."
        print(first)
    else:
        json = default | {"tags":str(interaction.user.id),"time":default_link_time}
        response = requests.post(zipcord_url + "link/create", json=json)
        
        if response.text:
            msg = f"`{response.text}`\n\nIn Minecraft, type the command `/link {response.text}` to link your accounts.\n-# Code is valid for 10 minutes"
            
    await interaction.response.send_message(msg,ephemeral=True)


@tasks.loop(seconds=update_time)
async def change_status(): # Update status
    status = "Error fetching info." # Fallback text.
    online = None
    forecast = None
    try: # Error catching if Zipcord is unavailable
        online_response = requests.get(zipcord_url + "players/count", json=default)
        online = online_response.text
        forecast_response = requests.get(zipcord_url + "world/forecast", json=default)
        forecast = forecast_response.text
    except requests.exceptions.ConnectionError:
        pass
    
    if online and forecast:
        status = f"{online} online | It's a {forecast}."
    await bot.change_presence(status=discord.Status.idle,activity=discord.Activity(type=discord.ActivityType.watching, name=status))

@bot.event
async def on_ready():
    change_status.start()
    await bot.change_presence(
        status=discord.Status.idle
    )

bot.run(bot_token)
