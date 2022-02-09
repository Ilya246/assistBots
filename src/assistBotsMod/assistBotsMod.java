package assistBotsMod;

import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import arc.util.*;
import arc.math.*;
import arc.struct.*;
import arc.Events;
import assistBotsMod.AIPlayer;

import static mindustry.Vars.*;

public class assistBotsMod extends Plugin{
    // public Seq<String> adminBehaviors = Seq.with("base"); //currently unused
    public static Seq<AIPlayer> AIPlayers = new Seq<>();

    public float rebalanceDelay = 1f;

    public enum Config{
        baseBotCount("The base amount of bots.", 4),
        botFraction("The amount a single player contributes to bot count.", 0.5f),
        compensationMultiplier("In PvP games, the amount of bots a team recieves per missing player.", 1.5f),
        tooltipChance("Chance of the /order tip appearing every tick.", 0.00005f),
        orderRestrict("Restricts the /order command to admins only. Also disables the /order tip.", false);

        public static final Config[] all = values();

        public final Object defaultValue;
        public Object value;
        public String description;

        Config(String description, Object value){
            this.description = description;
            this.defaultValue = value;
            this.value = defaultValue;
        }
        public int i(){
            return (int)value;
        }
        public float f(){
            return (float)value;
        }
        public boolean b(){
            return (boolean)value;
        }
        public String s(){
            return value.toString();
        }
    }

    @Override
    public void init(){
        Timer.schedule(() -> {
            if(state.isGame()){
                balancePlayers();
            };
        }, rebalanceDelay, rebalanceDelay);
        Events.run(Trigger.update, () -> {
            AIPlayers.each(p -> {
                p.update();
            });
            if(!Config.orderRestrict.b() && Mathf.chance(Config.tooltipChance.f() * Time.delta)){
                Call.sendMessage("[crimson]<Bot Plugin>: [accent]Consider using the [lightgray]/order []command to command bots!");
            }
        });
        Events.on(WorldLoadEvent.class, e -> {
            AIPlayers.clear();
        });
    }

    public void balancePlayers(){
        ObjectIntMap<Team> teamPlayerMap = new ObjectIntMap<>();
        ObjectIntMap<Team> teamBotMap = new ObjectIntMap<>();
        if(state.rules.pvp){
            state.teams.active.each(t -> {
                teamPlayerMap.put(t.team, 0);
            });
        }else if(state.rules.defaultTeam.core() != null){
            teamPlayerMap.put(state.rules.defaultTeam, 0);
        }
        Groups.player.each(p -> {
            teamPlayerMap.put(p.team(), teamPlayerMap.get(p.team(), 0) + 1);
        });
        AIPlayers.each(p -> {
            teamBotMap.put(p.team, teamBotMap.get(p.team, 0) + 1);
        });
        int targetBots = Config.baseBotCount.i();
        int maxPlayers = 0;
        for(ObjectIntMap.Entry<Team> e : teamPlayerMap){
            targetBots = Math.max(targetBots, Config.baseBotCount.i() + (int)(e.value * Config.botFraction.f()));
            maxPlayers = Math.max(maxPlayers, e.value);
        }
        for(ObjectIntMap.Entry<Team> e : teamPlayerMap){
            int thisTarget = targetBots + (int)((maxPlayers - e.value) * Config.compensationMultiplier.f());
            for(int i = teamBotMap.get(e.key, 0); i < thisTarget; i++){
                AIPlayer nplayer = new AIPlayer(e.key);
                AIPlayers.add(nplayer);
            }
            for(int i = teamBotMap.get(e.key, 0); i > thisTarget; i--){
                AIPlayer removePlayer = AIPlayers.find(b -> b.team == e.key);
                if(removePlayer != null){
                    removePlayer.unit.resetController();
                    if(removePlayer.spawnedByCore){
                        removePlayer.unit.spawnedByCore = true;
                    }
                    AIPlayers.remove(removePlayer);
                }
            }
        }
    }

    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("order", "[command] [amount]", "Order bots to switch their behavior.", (args, player) -> {
            if(args.length == 0){
                StringBuilder s = new StringBuilder("[accent]Available commands:");
                AIPlayer.behaviorTypes.each(b -> {
                    s.append(" " + b);
                });
                player.sendMessage(s.toString());
                return;
            }
            if(Config.orderRestrict.b() && !player.admin){
                player.sendMessage("[scarlet]/order is currently only usable by admins.");
                return;
            }
            String cmd = AIPlayer.behaviorTypes.find(b -> b.equals(args[0]));
            if(cmd == null){
                StringBuilder s = new StringBuilder("[scarlet]Invalid command. Available commands:");
                AIPlayer.behaviorTypes.each(b -> {
                    s.append(" " + b);
                });
                player.sendMessage(s.toString());
                return;
            }
            /*if(adminBehaviors.contains(cmd)){
                player.sendMessage("[scarlet]That command is only for admins.");
                return;
            }*/ //currently unused
            int amount = AIPlayers.size;
            if(args.length > 1){
                try{
                    amount = Math.min(amount, Integer.parseInt(args[1]));
                }catch(NumberFormatException e){
                    player.sendMessage("[scarlet]Invalid amount.");
                    return;
                }
            }
            Seq<AIPlayer> validBots = AIPlayers.select(p -> p.team == player.team());
            int botsChecked = 0;
            int botsSwitched = 0;
            for(AIPlayer p : validBots){
                if(botsSwitched == amount){
                    break;
                }
                if(p.behaviorType == "auto" || validBots.size - botsChecked <= amount){
                    p.behaviorType = cmd;
                    botsSwitched++;
                }
                botsChecked++;
            }
            final int botsSwitchedFinal = botsSwitched; //java is bad
            Groups.player.each(p -> p.team() == player.team(), p -> {
                p.sendMessage(Strings.format("@[accent] is switching behavior of @ bots to @.", player.coloredName(), botsSwitchedFinal, cmd));
            });
        });
    }

    public void registerServerCommands(CommandHandler handler){
        handler.register("botsconfig", "[name] [value]", "Configure bot plugin settings. Run with no arguments to list values.", args -> {
            if(args.length == 0){
                Log.info("All config values:");
                for(Config c : Config.all){
                    Log.info("&lk| @: @", c.name(), "&lc&fi" + c.s());
                    Log.info("&lk| | &lw" + c.description);
                    Log.info("&lk|");
                }
                return;
            }
            try{
                Config c = Config.valueOf(args[0]);
                if(args.length == 1){
                    Log.info("'@' is currently @.", c.name(), c.s());
                }else{
                    if(args[1].equals("default")){
                        c.value = c.defaultValue;
                    }else{
                        try{
                            if(c.defaultValue instanceof Integer){
                                c.value = Integer.parseInt(args[1]);
                            }else if(c.defaultValue instanceof Float){
                                c.value = Float.parseFloat(args[1]);
                            }else{
                                c.value = Boolean.parseBoolean(args[1]);
                            }
                        }catch(NumberFormatException e){
                            Log.err("Not a valid number: @", args[1]);
                            return;
                        }
                    }
                    Log.info("@ set to @.", c.name(), c.s());
                }
            }catch(IllegalArgumentException e){
                Log.err("Unknown config: '@'. Run the command with no arguments to get a list of valid configs.", args[0]);
            }
        });
    }
}
