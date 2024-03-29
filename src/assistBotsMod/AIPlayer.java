package assistBotsMod;

import arc.Core.*;
import arc.Events;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import assistBotsMod.*;
import assistBotsMod.aiTypes.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.Blocks;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.Team;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.type.Item;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.Vars;

import static mindustry.Vars.*;

public class AIPlayer{
    public float respawnTimer = assistBotsMod.Config.respawnTime.f();
    public boolean spawnedByCore = true;
    public String behaviorType = "auto";
    public String aiArgs = "";
    public Unit unit;
    public Team team;
    public Interval timer = new Interval(2);
    public static Seq<String> behaviorTypes = Seq.with("auto", "mine", "build", "defend");
    public static float coreCheckTime = 240f;
    public static float defendBuildingAggro = 400f;
    public static float autoAggroDistance = 640f;
    public static float autoAggroMultiplier = 1.2f;
    public static float autoCheckDelay = 60f;
    public static float prefBuildTime = 1800f / autoCheckDelay;

    //AI-related vars
    public String curAI = "mine";
    public float prefBuildTimer = 0f;

    public AIPlayer(Team team){
        this.team = team;
    }

    public void update(){
        if(unit == null || unit.dead()){
            respawnTimer += Time.delta;
        }
        Building core;
        if(respawnTimer >= assistBotsMod.Config.respawnTime.f() || (timer.get(1, coreCheckTime) && unit != null && (core = bestCore()) != null && ((CoreBlock)core.block).unitType != unit.type)){
            core = bestCore();
            if(core == null){
                assistBotsMod.AIPlayers.remove(this);
                return;
            }
            if(unit != null && spawnedByCore){
                Call.unitDespawn(unit);
            }
            int oldCap = state.rules.unitCap;
            state.rules.unitCap = Integer.MAX_VALUE - Units.getCap(team);
            unit = ((CoreBlock)core.block).unitType.spawn(team, core.x, core.y);
            state.rules.unitCap = oldCap;
            spawnedByCore = true;
            respawnTimer = 0f;
        }
        if(unit == null){
            return;
        }
        if(unit.getPlayer() != null){
            unit.spawnedByCore = spawnedByCore;
            unit = null;
            return;
        }
        if(!(unit.controller() instanceof MineAI)){
            unit.mineTile = null;
        }
        doAI(behaviorType);
    }
    public Building bestCore(){
        return team.cores().min(Structs.comps(Structs.comparingInt(c -> -((Building)c).block.size), Structs.comparingFloat(c -> unit == null ? 0f : ((Building)c).dst(unit.x, unit.y))));
    }

    public void doAI(String type){
        switch(type){
            case "mine":
                if(!(unit.controller() instanceof MineAI)){
                    unit.controller(new MineAI(aiArgs, unit));
                }
                break;
            case "defend":
                if(!(unit.controller() instanceof DefendAI)){
                    unit.controller(new DefendAI(aiArgs));
                }
                break;
            case "build":
                if(!(unit.controller() instanceof BuildAI)){
                    unit.controller(new BuildAI());
                }
                break;
            default:
                doAutoAI();
                break;
        }
    }
    public void resetAI(){
        if(unit != null){
            switch(behaviorType){
                case "mine":
                    unit.controller(new MineAI(aiArgs, unit));
                    break;
                case "defend":
                    unit.controller(new DefendAI(aiArgs));
                    break;
                case "build":
                    unit.controller(new BuildAI());
                    break;
                default:
                    unit.controller(new MineAI("", unit));
                    break;
            }
        }
    }

    public Teamc target(float x, float y, float buildingRange, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, buildingRange, u -> u.checkTarget(air, ground), t -> ground && targetBuildings() && unit.within(t, buildingRange));
    }
    public boolean targetBuildings(){
        return unit.type.weapons.first().bullet.buildingDamageMultiplier >= 0.1f;
    }
    public void doAutoAI(){
        doAI(curAI);
        if(timer.get(0, autoCheckDelay)){
            Teamc target = target(unit.x, unit.y, DefendAI.defendRangeDefault, unit.type.targetAir, unit.type.targetGround);
            if(target != null && unit.within(target, autoAggroDistance + unit.range() * autoAggroMultiplier) && !(target instanceof Building && !targetBuildings())){
                curAI = "defend";
                return;
            }
            Building core = unit.closestCore();
            if(core == null){
                return;
            }
            Item min = unit.type.mineItems.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i));
            if(prefBuildTimer == 0f && min != null && core.acceptStack(min, 1, unit) != 0){
                curAI = "mine";
            }else{
                curAI = "build";
                if(prefBuildTimer == 0f){
                    prefBuildTimer = prefBuildTime;
                }
                prefBuildTimer = Math.max(prefBuildTimer - Time.delta, 0f);
            }
        }
    }
}
