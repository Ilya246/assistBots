package assistBotsMod.aiTypes;

import arc.math.*;
import arc.struct.Queue;
import arc.util.Time;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.BlockPlan;
import mindustry.gen.*;
import mindustry.world.Build;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;

import static mindustry.Vars.*;

public class BuildAI extends AIController{
    private boolean found = false;
    public float buildOverclock = 0f;
    public Unit following;
    public BlockPlan lastPlan;

    public static float buildRadius = 1600f;
    public static float rebuildTime = 20f;
    public static float searchTime = 30f;

    @Override
    public void updateUnit(){
        unit.updateBuilding = true;
        if(following != null){
            if(!following.isValid() || !following.activelyBuilding()){
                following = null;
                unit.plans.clear();
                return;
            }
            unit.plans.clear();
            unit.plans.addFirst(following.buildPlan());
            lastPlan = null;
        }
        if(unit.buildPlan() != null){
            BuildPlan req = unit.buildPlan();
            if(!req.breaking && timer.get(timerTarget2, searchTime)){
                for(Player player : Groups.player){
                    if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                        unit.plans.removeFirst();
                        unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                        return;
                    }
                }
            }
            boolean valid = !(lastPlan != null && lastPlan.removed) &&
            ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) ||
            (req.breaking ? Build.validBreak(unit.team(), req.x, req.y) : Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));
            if(valid){
                moveTo(req.tile(), buildingRange - 20f, 40f);
            }else{
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{
            if(timer.get(timerTarget2, searchTime)){
                found = false;
                Units.nearby(unit.team, unit.x, unit.y, buildRadius, u -> {
                    if(found) return;
                    if(u.canBuild() && u != unit && u.activelyBuilding()){
                        BuildPlan plan = u.buildPlan();
                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - buildingRange, 0);
                            if(dist / unit.speed() < cons.buildCost * 0.9f){
                                following = u;
                                found = true;
                            }
                        }
                    }
                });
            }
            if(unit.team.data().plans.isEmpty()){
                buildOverclock = Math.max(buildOverclock - Time.delta, 0f);
            }else if(following == null && timer.get(timerTarget3, rebuildTime - buildOverclock)){
                Queue<BlockPlan> blocks = unit.team.data().plans;
                BlockPlan block = blocks.first();
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation)){
                    lastPlan = block;
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                    blocks.addLast(blocks.removeFirst());
                    buildOverclock = Math.min(buildOverclock + Time.delta * 2f, rebuildTime);
                }else{
                    blocks.addLast(blocks.removeFirst());
                }
            }
        }
    }
}
