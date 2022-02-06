# Overview
Adds bots to the default team in survival, and to all teams in PvP.
# Functionality
Right now, the bots are capable of flying up to and shooting enemy units, acting as miners, and acting as builder units (poly). They can choose between those behaviors by themselves, but players can order them to stick to a specific one via the `order` command.
# Specifics
In survival/attack gamemodes, there is a set amount of bots, plus more bots added per every player on the server. In PvP, the amount of players on the most populated team decides bot amount. Teams with less players will recieve more bots.
All of those values are configurable through the `botsconfig` server command.
