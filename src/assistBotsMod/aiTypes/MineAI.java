package assistBotsMod.aiTypes;

import mindustry.content.*;
import arc.struct.Seq;
import mindustry.ctype.ContentType;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MineAI extends AIController{
    public boolean mining = true;
    public Item targetItem;
    public Seq<Item> argsItems = new Seq<>();
    public Tile ore;

    public MineAI(String sitems, Unit unit){
        String[] items = sitems.split(" ");
        for(String item : items){
            Item argsItem = content.getByName(ContentType.item, item);
            if(argsItem != null && argsItem.hardness <= unit.type.mineTier && indexer.hasOre(argsItem)){
                argsItems.add(argsItem);
            }
        }
    }

    @Override
    public void updateMovement(){
        Building core = unit.closestCore();
        if(!(unit.canMine()) || core == null) return;
        if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.miningRange)){
            unit.mineTile(null);
        }
        if(mining){
            if(timer.get(timerTarget2, 60 * 4) || targetItem == null){
                targetItem = argsItems.isEmpty() ? unit.type.mineItems.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i)) : argsItems.min(i -> core.items.get(i));
            }
            if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                unit.clearItem();
                unit.mineTile = null;
                return;
            }
            if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                mining = false;
            }else{
                if(timer.get(timerTarget3, 60) && targetItem != null){
                    ore = indexer.findClosestOre(unit, targetItem);
                }
                if(ore != null){
                    moveTo(ore, unit.type.miningRange / 2f, 20f);

                    if(ore.block() == Blocks.air && unit.within(ore, unit.type.miningRange)){
                        unit.mineTile = ore;
                    }

                    if(ore.block() != Blocks.air){
                        mining = false;
                    }
                }
            }
        }else{
            unit.mineTile = null;
            if(unit.stack.amount == 0){
                mining = true;
                return;
            }
            if(unit.within(core, unit.type.range)){
                if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                    Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
                }
                unit.clearItem();
                mining = true;
            }
            circle(core, unit.type.range / 1.8f);
        }
    }
}
