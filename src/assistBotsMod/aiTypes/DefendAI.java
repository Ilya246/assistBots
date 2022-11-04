package assistBotsMod.aiTypes;

import arc.math.*;
import arc.util.Tmp;
import mindustry.ai.Pathfinder;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class DefendAI extends AIController{

    public Teamc target;
    public float defendMaxRange;

    public static float defendRangeDefault = 6400f;
    public static float buildingRange = 320f * 320f;

    public DefendAI(String range){
        try{
            defendMaxRange = Float.parseFloat(range) * tilesize;
        }catch(NumberFormatException e){
            defendMaxRange = defendRangeDefault;
        }
    }

    @Override
    public void updateMovement(){
        Building ownCore = unit.closestCore();
        if(ownCore == null){
            return;
        }
        if(Units.invalidateTarget(target, unit, defendMaxRange) && target != null){
            target = findMainTarget(unit.x, unit.y, 0, unit.type.targetAir, unit.type.targetGround);
        }
        if(timer.get(timerTarget2, target == null ? 30f : 60f)){
            target = findMainTarget(unit.x, unit.y, 0, unit.type.targetAir, unit.type.targetGround);
        }
        if(!Units.invalidateTarget(target, unit, defendMaxRange) && unit.hasWeapons()){
            if(target.within(ownCore, unit.range())){
                moveTo(vec.set(ownCore).add(target).scl(0.5f), unit.hitSize, unit.hitSize * 8f);
            }else{
                moveTo(target, unit.range() * 0.8f, 8f);
            }
        }else{
            if(unit.isFlying()){
                moveTo(ownCore, unit.range() * 0.8f, 40f);
            }else{
                pathfind(Pathfinder.fieldCore);
            }
        }
        if(unit.type.omniMovement){
            faceTarget();
        }else{
            faceMovement();
        }
    }
    @Override
    public boolean retarget(){
        return timer.get(timerTarget, target == null ? 30f : 60f);
    }
    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, defendMaxRange, u -> u.checkTarget(air, ground), t -> unit.type.weapons.first().bullet.buildingDamageMultiplier >= 0.1f && ground && unit.dst2(t) < buildingRange);
    }
}
