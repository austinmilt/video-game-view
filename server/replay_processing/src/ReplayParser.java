/*
Copyright 2018 Austin Walker Milt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import org.apache.log4j.BasicConfigurator;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.Predicate;

import javax.management.Attribute;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;

public class ReplayParser {

    // CLASS VARIABLES ***************************************************** //

    // constants
    public static final String ABL_PATTERN = "m_hAbilities.*";
    public static final int ABILITY_SLOTS = 24;
    public static final String ABILITY_SLOT_FMT = "m_hAbilities.%04d";
    public static final String ABILITY_LVL_PROP = "m_iLevel";
    public static final String CSV_ENCODE = "utf-8";
    public static final String CSV_HED_ABILITIES = "abilities";
    public static final String CSV_HED_HERO = "hero";
    public static final String CSV_HED_ITEMS = "items";
    public static final String CSV_HED_TIME = "time";
    public static final String CSV_SEP_ARRAY = ";";
    public static final String CSV_SEP_CAT = ",";
    public static final String CSV_SEP_LVL = ":";
    public static final String CSV_NEWLINE = "\n";
    public static final String ENTITY_HIDDEN_PROP = "m_bHidden";
    public static final String ENTITY_INDEX_PROP = "m_pEntity.m_nameStringableIndex";
    public static final String ENTITY_SUBPROP_DEL = ".";
    public static final int ENTITY_NULL = 16777215;
    public static final String GRP_STR = "CDOTAGamerulesProxy";
    public static final String HERO_PATTERN = "CDOTA_Unit_Hero_.*";
    public static final String HERO_ORDER = "m_iPlayerID";
    public static final String HERO_TEAMDATA_PREFIX = "m_vecDataTeam";
    public static final String HERO_PLAYERTEAMDATA_PREFIX = "m_vecPlayerTeamData";
    public static final String ITEM_PATTERN = "m_hItems.*";
    public static final int ITEM_SLOTS = 17;
    public static final String ITEM_SLOT_FMT = "m_hItems.%04d";
    public static final int MAX_PLAYERS = 50;
    public static final String PLAYERDATA_DTNAME = "CDOTA_PlayerResource";
    public static final String STRTABLE_ENTNAME = "EntityNames";
    public static final String TEAM_RADIANT = "CDOTA_DataRadiant";
    public static final String TEAM_DIRE = "CDOTA_DataDire";
    public static final String TIME_CUR_PROP = "m_pGameRules.m_fGameTime";
    public static final Float TIME_EPS = new Float(0.01);
    public static final String TIME_FMT = "%.3f";
    public static final Float TIME_INTERVAL_SECONDS = new Float(1.0);
    public static final String TIME_PRE_PROP = "m_pGameRules.m_flPreGameStartTime";
    public static final String TIME_SEP = ":";
    public static final String TIME_STA_PROP = "m_pGameRules.m_flGameStartTime";
    public static final String TIME_TRN_PROP = "m_pGameRules.m_flStateTransitionTime";
    public static final Predicate<Entity> IS_HERO = new Predicate<Entity>() {
        public boolean apply(Entity e) {
            boolean flag = e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
            if (flag) { flag = e.hasProperty("m_nPlayerOwnerID"); }
            if (flag) { flag = (e.getProperty("m_nPlayerOwnerID").equals(-1)); }
            if (flag && e.hasProperty("m_hReplicatingOtherHeroModel")) {
                flag = e.getProperty("m_hReplicatingOtherHeroModel").equals(ENTITY_NULL);
            }
            return flag;
        }
    };
    public final HashMap<String, AttributeHandler> HERO_ATTRIBUTES = build_attr();


    // class globals
    // public static int nPlayers;
    private final SimpleRunner runner;
    private final BufferedWriter writer;
    private Float lastTime = new Float(-9999.0);
    private int lastHero = 999999999;
    private HashMap<Integer, HashMap<String, ArrayList<Object>>> entityHandles = new HashMap<>();
    private HashMap<Integer, Float> times = new HashMap<>();
    private Float prevGameTime = Float.NEGATIVE_INFINITY;
    
    public HashMap<String, AttributeHandler> build_attr() {
        HashMap<String, AttributeHandler> output = new HashMap<>();
        output.put("str_natural", new DefaultAttributeHandler("m_flStrength"));
        output.put("str_total", new DefaultAttributeHandler("m_flStrengthTotal"));
        output.put("int_natural", new DefaultAttributeHandler("m_flIntellect"));
        output.put("int_total", new DefaultAttributeHandler("m_flIntellectTotal"));
        output.put("agi_natural", new DefaultAttributeHandler("m_flAgility"));
        output.put("agi_total", new DefaultAttributeHandler("m_flAgilityTotal"));
        output.put("dmg_min", new DefaultAttributeHandler("m_iDamageMin"));
        output.put("dmg_max", new DefaultAttributeHandler("m_iDamageMax"));
        output.put("dmg_bonus", new DefaultAttributeHandler("m_iDamageBonus"));
        output.put("speed", new DefaultAttributeHandler("m_iMoveSpeed"));
        output.put("level", new DefaultAttributeHandler("m_iCurrentLevel"));
        output.put("xp_current", new DefaultAttributeHandler("m_iCurrentXP"));
        output.put("xp_total", new TeamHeroAttributeHandler("m_iTotalEarnedXP"));
        output.put("order", new DefaultAttributeHandler("m_iPlayerID"));
        output.put("mana_current", new DefaultAttributeHandler("m_flMana"));
        output.put("mana_max", new DefaultAttributeHandler("m_flMaxMana"));
        output.put("mana_regen", new ManaRegenHandler("m_flManaRegen", "m_flManaThinkRegen", "m_flIntellectTotal"));
        output.put("health_current", new DefaultAttributeHandler("m_iHealth"));
        output.put("health_max", new DefaultAttributeHandler("m_iMaxHealth"));
        output.put("health_regen", new HealthRegenHandler("m_flHealthRegen", "m_flHealthThinkRegen", "m_flStrengthTotal"));
        output.put("net_worth", new TeamHeroAttributeHandler("m_iNetWorth"));
        output.put("denies", new TeamHeroAttributeHandler("m_iDenyCount"));
        output.put("last_hits", new TeamHeroAttributeHandler("m_iLastHitCount"));
        output.put("hero_dmg", new TeamHeroAttributeHandler("m_iHeroDamage"));
        output.put("gold_total", new TeamHeroAttributeHandler("m_iTotalEarnedGold"));
        output.put("kills", new PlayerResourceHeroAttributeHandler("m_iKills"));
        output.put("deaths", new PlayerResourceHeroAttributeHandler("m_iDeaths"));
        output.put("assists", new PlayerResourceHeroAttributeHandler("m_iAssists"));
        return output;
    }


    // HERO ATTRIBUTE HANDLERS ********************************************** //

    /**
     * Short classes for getting basic properties of heroes.
     */
    public abstract class AttributeHandler {
        public abstract Object get(skadistats.clarity.model.Entity hero, Context ctx);
    }


    /**
     * Default handler for getting hero attributes. One attribute key in, one
     * Object out.
     */
    public class DefaultAttributeHandler extends AttributeHandler {
        private String key;
        public DefaultAttributeHandler(String key) {
            this.key = key;
        }
        public Object get(Entity hero) { return hero.getProperty(this.key); }
        public Object get(Entity hero, Context ctx) { return this.get(hero); }
    }


    /**
     * Mana regeneration calculator/handler.
     */
    public class ManaRegenHandler extends AttributeHandler {
        private String base;
        private String bonus;
        private String intel;

        /**
         * Constructs a mana regeneration handler
         * @param baseKey base mana regen key
         * @param bonusKey bonus regen from items etc key
         * @param intKey intelligence key
         */
        ManaRegenHandler(String baseKey, String bonusKey, String intKey) {
            this.base = baseKey;
            this.bonus = bonusKey;
            this.intel = intKey;
        }

        /**
         * Calculates the net mana regeneration for the hero.
         * @param hero hero entity at the current snapshot in the replay
         * @return net mana regeneration
         */
        public Object get(Entity hero) {
            Float base = hero.getProperty(this.base);
            Float bonus = hero.getProperty(this.bonus);
            Float intel = hero.getProperty(this.intel);
            return (base + bonus)*(1 + intel*0.02);
        }

        public Object get(Entity hero, Context ctx) { return this.get(hero); }
    }


    /**
     * Health regeneration calculator/handler.
     */
    public class HealthRegenHandler extends AttributeHandler {
        private String base;
        private String bonus;
        private String strength;

        /**
         * Constructs a health regeneration handler
         * @param baseKey base health regen key
         * @param bonusKey bonus regen from items etc key
         * @param strKey strength key
         */
        HealthRegenHandler(String baseKey, String bonusKey, String strKey) {
            this.base = baseKey;
            this.bonus = bonusKey;
            this.strength = strKey;
        }

        /**
         * Calculates the net health regeneration for the hero.
         * @param hero hero entity at the current snapshot in the replay
         * @return net health regeneration
         */
        public Object get(Entity hero) {
            Float base = hero.getProperty(this.base);
            Float bonus = hero.getProperty(this.bonus);
            Float strength = hero.getProperty(this.strength);
            return (base + bonus)*(1 + strength*0.00714285714);
        }

        public Object get(Entity hero, Context ctx) { return this.get(hero); }
    }


    /**
     * Handlers for attributes in the team data entities.
     */
     public class TeamHeroAttributeHandler extends AttributeHandler {
        private String key;

        /**
         * Constructs the handler to find the terminal attribute under CDOTA_Data[Radiant/Dire] player vector.000#.attribute.
         * @param attribute attribute to find for the hero
         */
        TeamHeroAttributeHandler(String attribute) {
            this.key = attribute;
        }


        /**
         * Gets the hero global order (0-9) as it would appear in the top of the screen.
         *
         * @param hero hero Entity to get the order of
         * @return order of the hero
         */
        public Integer get_order(Entity hero) {
            return hero.getProperty(HERO_ORDER);
        }


        /**
         * Uses the hero order to get the team offset to construct the 000# id.
         * @param hero hero entity to get the offset of
         * @return offset (either 0 for radiant or -5 for dire)
         */
        public Integer get_offset(Entity hero) {
            if (this.get_order(hero) < 5) { return 0; }
            else { return -5; }
        }


        /**
         * Uses the hero order to get the entity DT name of the hero's team.
         *
         * @param hero hero to get the team of
         * @return team name, either @code TEAM_RADIANT or @code TEAM_DIRE
         */
        public String get_team(Entity hero) {
            if (this.get_order(hero) < 5) { return TEAM_RADIANT; }
            else { return TEAM_DIRE; }
        }


        /**
         * Gets the full key of the attribute for this handler, including the relative index and other prefix info.
         *
         * @param hero hero to get the attribute key for
         * @return full key to getProperty of the team entity
         */
        public String full_key(Entity hero) {
            Integer order = this.get_order(hero);
            Integer offset = this.get_offset(hero);
            Integer teamOrder = order + offset;
            String stringOrder = String.format("%04d", teamOrder);
            String prefix = HERO_TEAMDATA_PREFIX + ENTITY_SUBPROP_DEL + stringOrder + ENTITY_SUBPROP_DEL;
            return prefix + this.key;
        }


        /**
         * Gets this handler's info for the hero in the current context.
         *
         * @param hero hero to get the info for
         * @param ctx Replay Context to get team entities
         * @return value of this handler's attribute
         */
        @UsesEntities
        public Object get(Entity hero, Context ctx) {
            Entities entities = ctx.getProcessor(Entities.class);
            Entity teamData = entities.getByDtName(this.get_team(hero));
            return teamData.getProperty(full_key(hero));
        }
     }


    /**
     * Handler for getting data from CDOTA_PLayerResource
     */
    public class PlayerResourceHeroAttributeHandler extends AttributeHandler {
         private String key;

         /**
          * Constructs a handler to get data from CDOTA_PlayerResource for a particular hero.
          * @param attribute
          */
         PlayerResourceHeroAttributeHandler(String attribute) {
             this.key = attribute;
         }

         /**
          * Gets the hero global order (0-9) as it would appear in the top of the screen.
          *
          * @param hero hero Entity to get the order of
          * @return order of the hero
          */
         public Integer get_order(Entity hero) {
             return hero.getProperty(HERO_ORDER);
         }


         /**
          * Gets the full key of the attribute for this handler, including the relative index and other prefix info.
          *
          * @param hero hero to get the attribute key for
          * @return full key to getProperty of the team entity
          */
         public String full_key(Entity hero) {
             Integer order = this.get_order(hero);
             String stringOrder = String.format("%04d", order);
             String prefix = HERO_PLAYERTEAMDATA_PREFIX + ENTITY_SUBPROP_DEL + stringOrder + ENTITY_SUBPROP_DEL;
             return prefix + this.key;
         }


         /**
          * Gets this handler's info for the hero in the current context.
          *
          * @param hero hero to get the info for
          * @param ctx Replay Context to get team entities
          * @return value of this handler's attribute
          */
         @UsesEntities
         public Object get(Entity hero, Context ctx) {
             Entities entities = ctx.getProcessor(Entities.class);
             Entity playerResource = entities.getByDtName(PLAYERDATA_DTNAME);
             return playerResource.getProperty(full_key(hero));
         }

     }
    
    
    // METHODS ************************************************************* //

    /**
    * Creates the string representation of a hero's item slot property.
    * <p>
    * This should be used to automatically get the formatted name of the item
    * slot property for a hero entity in a replay file, to be used along with
    * the Entity.getProperty() method.
    *
    * @param id  the index ID of the item slot to get the property name for
    * @return the property name of the given item slot index
    */
    public String getItemSlotNameByInt(int id) {
        return String.format(ITEM_SLOT_FMT, id);
    }
    
    
    /**
    * Creates the string representation of a hero's ability slot property.
    * <p>
    * This should be used to automatically get the formatted name of the
    * ability slot property for a hero entity in a replay file, to be used 
    * along with the Entity.getProperty() method.
    *
    * @param id  the index ID of the ability slot to get the property name for
    * @return the property name of the given ability slot index
    */
    public String getAbilitySlotNameByInt(int id) {
        return String.format(ABILITY_SLOT_FMT, id);
    }
    
    
    /**
    * Gets the name of an entity from a replay's {@code StringTables}.
    * <p>
    * This should be used to get a human-readable tag for an {@code Entity} (as
    * opposed to a handle or other representation) as represented in a replay's
    * {@code StringTables}. It is intended to be used during the execution of a
    * runner (e.g. {@code SimpleRunner}) that {@code UsesStringTables}.
    *
    * @param id  the handle of the {@code Entity} for which a name should be retrieved
    * @param entities  the entities for the current tick of the current replay (e.g. as returned by Context.getProcessor(Entities.class))
    * @param stringTable  the replay {@code StringTable} from which to grab the entity's name
    * @return the name of the entity from [stringTable], null if the entity is null
    */
    public String getEntityNameByHandle(int id, Entities entities, StringTable stringTable) {
        Entity entity = entities.getByHandle(id);
        int index;
        if (entity == null) {
            return null;
        }
        else {
            index = entity.getProperty(ENTITY_INDEX_PROP);
            return stringTable.getNameByIndex(index);
        }
    }
    
    public boolean entityIsHiddenByHandle(int id, Entities entities) {
        Entity entity = entities.getByHandle(id);
        boolean hidden;
        if (entity == null) {
            return true;
        }
        else {
            return entity.getProperty(ENTITY_HIDDEN_PROP);
        }
    }
    
    /**
    * Gets the real game time of a match in seconds as reflected in the in-game clock.
    * <p>
    * This gets the game time at the current tick in the runner of a replay 
    * from the {@code Entities}. It is intended to be used during the execution
    * of a runner (e.g. {@code SimpleRunner}).
    *
    * @param entities  the entities for the current tick of the current replay (e.g. as returned by Context.getProcessor(Entities.class))
    * @return the current game time in seconds, null before pre-game begins, negative during pre-game, positive after 0:00 of game clock
    */
    public Float getRealGameTimeSeconds(Entities entities) {
        Entity grules = entities.getByDtName(GRP_STR);
        Float gameTime = null;
        Float startTime = null;
        Float preGameTime = null;
        Float transitionTime = null;
        Float realTime = null;
        
        // before the match starts, there's CDOTAGamerulesProxy
        if (grules != null) {
            gameTime = grules.getProperty(TIME_CUR_PROP);
            
            // before the match starts, there's no game "time"
            if (gameTime != null) {
                preGameTime = grules.getProperty(TIME_PRE_PROP);
                
                // before hero picking and strategy time are finished, the
                //  pre-game countdown is still at 0, i.e. nothing has happened
                //  in the match
                if (preGameTime > TIME_EPS) {
                    startTime = grules.getProperty(TIME_STA_PROP);
                    
                    // time after the clock hits 0:00
                    if (startTime > TIME_EPS) {
                        realTime = gameTime - startTime;
                    }
                    
                    // between the pre-game and 0:00 time of the match, the
                    //  transition time reflects when the match is supposed to
                    //  start (i.e. hit 0:00 on the clock), and gives a good
                    //  approximation of when the match will start. Up to that
                    //  point, the start time is set to 0.
                    else {
                        transitionTime = grules.getProperty(TIME_TRN_PROP);
                        realTime = gameTime - transitionTime;
                    }
                }
            }
        }
        
        return realTime;
    }
    
    // NOT USED, BUT HERE FOR POSTERITY ***************************************
    // public String getClockTime(Entities entities) {
        // Float gameTime = getRealGameTimeSeconds(entities);
        // String clockTime = null;
        // if (gameTime != null) {
            // int minutes = (int) Math.floor(gameTime / 60.);
            // int seconds = (int) Math.round(Math.abs(gameTime % 60.));
            // clockTime = String.format("%d%s%02d", minutes, TIME_SEP, seconds);
        // }
        // return clockTime;
    // }
    // ************************************************************************
    
    
    /**
    * Records a hero's attribute and item handles at a runner's current tick.
    * <p>
    * Note this attempts to avoid duplication of hero status updates at high
    * frequency by checking for recent updates to heroes.
    *
    * @param ctx  the {@code Context} of the current runner
    * @param hero  the hero {@code Entity} for which to get updates
    * @throws IOException passed from other code that throws the same
    */
    @UsesEntities
    public void heroUpdate(Context ctx, Entity hero) throws IOException {
        
        // loop declarations
        int heroID;
        String abilitySlotName;
        Integer abilityID;
        Integer abilityLevel;
        String itemSlotName;
        int itemID;
        int j;
        
        // get current game time
        Entities entities = ctx.getProcessor(Entities.class);
        Float gameTime = getRealGameTimeSeconds(entities);

        // get hero abilities and items (if hero has been set yet)
        if ((hero != null) && (gameTime != null)) {
        
            // get the hero name and see if this is another update in the same 
            //  time. If so, skip it
            heroID = hero.getHandle();
            if (Math.abs(gameTime - lastTime) < TIME_EPS) {
                lastTime = gameTime;
                if (heroID == lastHero) {
                    return;
                } else {
                    lastHero = heroID;
                }
            }
            lastTime = gameTime;

            /***************************************************************************************/
//            StringTable stringTable = ctx.getProcessor(StringTables.class).forName(STRTABLE_ENTNAME);
//            String heroName = getEntityNameByHandle(heroID,entities,stringTable);
//            System.out.println("logging " + heroName + " at " + gameTime);
            /****************************************************************************************/

            // get hero abilities and their levels
            entityHandles.put(heroID, new HashMap<>());
            entityHandles.get(heroID).put("abilities", new ArrayList<>());
            times.put(heroID, gameTime);
            for (j = 0; j < ABILITY_SLOTS; j++){
                abilitySlotName = getAbilitySlotNameByInt(j);
                abilityID = hero.getProperty(abilitySlotName);
                abilityLevel = 0;
                if ((abilityID == null) || (abilityID == ENTITY_NULL)){
                    abilityID = ENTITY_NULL;
                    abilityLevel = 0;
                }
                else {
                    Entity ability = ctx.getProcessor(Entities.class).getByHandle(abilityID);
                    abilityLevel = ability.getProperty(ABILITY_LVL_PROP);
                }
                entityHandles.get(heroID).get("abilities").add(new Integer[]{abilityID, abilityLevel});
            }
            
            // get hero items
            entityHandles.get(heroID).put("items", new ArrayList<>());
            for (j = 0; j < ITEM_SLOTS; j++){
                itemSlotName = getItemSlotNameByInt(j);
                itemID = hero.getProperty(itemSlotName);
                entityHandles.get(heroID).get("items").add(itemID);
            }

            // get the hero's status (attributes, etc.)
            for (String k : HERO_ATTRIBUTES.keySet()) {
                AttributeHandler handler = HERO_ATTRIBUTES.get(k);
                ArrayList<Object> value = new ArrayList<>();
                value.add(handler.get(hero, ctx));
                entityHandles.get(heroID).put(k, value);
            }
        }  
    }

    
    
    /**
    * Writes a hero's attributes and items to file at a runner's current tick.
    * <p>
    * This method resolves the names of entities stored by heroUpdate() and 
    * writes those names to the log file. Though it checks for updates at every
    * tick, it only performs further actions when heroUpdate() has been called.
    * <p>
    * Lines in the log file are written like the following:
    * <p>
    * 5.555,hero_a,ability_1;ability_2;...;ability_n,item_1;item_2;...;item_n
    * <p>
    * 7.344,hero_b,ability_1;ability_2;...;ability_n,item_1;item_2;...;item_n
    * <p>
    * Note how different separators are used for categories of things to be
    * reported for a hero on each line of the file.
    *
    * @param ctx  the {@code Context} of the current runner
    * @param synthetic  variable automatically passed by {@code OnTickEnd}. Not
    *   used here
    * @throws IOException passed from other code that throws the same
    */
    @UsesStringTable(STRTABLE_ENTNAME)
    @UsesEntities
    public void writeData(Context ctx, boolean synthetic) throws IOException {
        
        // skip the rest of the function if there are no entities to process
        if (entityHandles.size() == 0) {
            return;
        }
        
        // loop variable declarations
        Entities entities = ctx.getProcessor(Entities.class);
        StringTable stringTable = ctx.getProcessor(StringTables.class).forName(STRTABLE_ENTNAME);
        Boolean hidden;
        String entityName;
        Integer entityID;
        String line;
        ArrayList<Object> sublist;
        int j;
        
        // resolve hero, ability, and item names
        for (Map.Entry<Integer, HashMap<String, ArrayList<Object>>> entry : entityHandles.entrySet()) {
            
            // hero name
            entityName = getEntityNameByHandle(entry.getKey(), entities, stringTable);
            line = String.format(TIME_FMT, times.get(entry.getKey())) + CSV_SEP_CAT + entityName + CSV_SEP_CAT; 
            
            // ability names and levels
            sublist = entry.getValue().get("abilities");
            for (j = 0; j < ABILITY_SLOTS; j++) {
                Integer[] idAndLevel = (Integer[]) sublist.get(j);
                entityID = idAndLevel[0];
                Integer entityLevel = idAndLevel[1];
                
                // skip abilities that are hidden (e.g. those that activate
                // upon the activation of another ability). This is supposed to
                // ensure that ability slots are correctly filled as they appear
                // in-game
                if (entityID == ENTITY_NULL) {
                    hidden = false;
                }
                else {
                    hidden = entityIsHiddenByHandle(entityID, entities);
                }
                if (!hidden) {
                    entityName = getEntityNameByHandle(entityID, entities, stringTable);
                    if (j > 0) {
                        line += CSV_SEP_ARRAY + entityName + CSV_SEP_LVL + entityLevel;
                    }
                    else {
                        line += entityName + CSV_SEP_LVL + entityLevel;
                    }
                }
            }
            
            // item names
            line += CSV_SEP_CAT;
            sublist = entry.getValue().get("items");
            for (j = 0; j < ITEM_SLOTS; j++) {
                entityID = (Integer) sublist.get(j);
                entityName = getEntityNameByHandle(entityID, entities, stringTable);
                if (j > 0) {
                    line += CSV_SEP_ARRAY + entityName;
                }
                else {
                    line += entityName;
                }
            }

            // other hero data
            line += CSV_SEP_CAT;
            j = 0;
            for (String attr : HERO_ATTRIBUTES.keySet()) {
                Object value = entry.getValue().get(attr).get(0);
                if (j > 0) {
                    line += CSV_SEP_CAT + value;
                }
                else {
                    line += value;
                }
                j += 1;
            }
            
            // update the time log
            writer.write(CSV_NEWLINE + line);
        }
        
        // clear the entity handle buffer so the check at the beginngin of this
        // method passes unless heroUpdate() adds something to it
        entityHandles.clear();
    }


    /**
     * An updater that runs once every some number of seconds.
     *
     * @param ctx the {@code Context} of the current runner
     * @param synthetic variable automatically passed by {@code OnTickEnd}. Not
     *   used here
     */
    @OnTickEnd
    @UsesEntities
    public void onTimeInterval(Context ctx, boolean synthetic) throws IOException {
        Entities entities = ctx.getProcessor(Entities.class);
        Entity grules = entities.getByDtName(GRP_STR);
        if (grules != null) {
            Float gameTime = grules.getProperty(TIME_CUR_PROP);
            Float timeSinceLast = gameTime - prevGameTime;
            if (timeSinceLast >= TIME_INTERVAL_SECONDS) {
                prevGameTime = gameTime;
                Iterator<Entity> heroes = entities.getAllByPredicate(IS_HERO);
                while (heroes.hasNext()) {
                    heroUpdate(ctx, heroes.next());
                }
                writeData(ctx, synthetic);
            }
        }
    }

    
//    /**
//    * Uses the OnEntityPropertyChanged decorator to automatically update hero status during a replay.
//    * <p>
//    * This is essentially a listener for changes to hero items during a replay.
//    * It uses such events to update hero items and abilities.
//    *
//    * @param ctx  the {@code Context} of the current runner
//    * @param hero  the hero {@code Entity} for which to get updates
//    * @param fp  not used, present by necessity with the {@code OnEntityPropertyChanged} decorator
//    * @throws IOException passed from other code that throws the same
//    */
//    @OnEntityPropertyChanged(classPattern = HERO_PATTERN, propertyPattern = ITEM_PATTERN)
//    public void onItemChange(Context ctx, Entity hero, FieldPath fp) throws IOException {
//        heroUpdate(ctx, hero);
//    }
//
//
//    /**
//    * Uses the OnEntityPropertyChanged decorator to automatically update hero status during a replay.
//    * <p>
//    * This is essentially a listener for changes to hero abilities during a replay.
//    * It uses such events to update hero items and abilities.
//    *
//    * @param ctx  the {@code Context} of the current runner
//    * @param hero  the hero {@code Entity} for which to get updates
//    * @param fp  not used, present by necessity with the {@code OnEntityPropertyChanged} decorator
//    * @throws IOException passed from other code that throws the same
//    */
//    @OnEntityPropertyChanged(classPattern = HERO_PATTERN, propertyPattern = ABL_PATTERN)
//    public void onAbilityChange(Context ctx, Entity hero, FieldPath fp) throws IOException {
//        heroUpdate(ctx, hero);
//    }

    
    
    /**
    * Creates and executes a replay runner as well as the writer for abilities and items.
    * <p>
    *
    * @param inputFile  the path to the input replay file
    * @param outputFile  the path to the output file where hero abilities and items should be recorded
    * @throws IOException passed from other code that throws the same
    * @throws InterruptedException passed from other code that throws the same
    */
    public ReplayParser(String inputFile, String outputFile) throws IOException, InterruptedException {
        writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(outputFile), CSV_ENCODE
            )
        );
        writer.write(CSV_HED_TIME + CSV_SEP_CAT + CSV_HED_HERO + CSV_SEP_CAT + CSV_HED_ABILITIES + CSV_SEP_CAT + CSV_HED_ITEMS);
        for (String k : HERO_ATTRIBUTES.keySet()) { writer.write(CSV_SEP_CAT + k); }
        runner = new SimpleRunner(new MappedFileSource(inputFile)).runWith(this);
        writer.close();
    }
    
    
    /**
    * Creates and executes the ReplayParser class.
    * <p>
    *
    * @param args  the command-line args for {@code ReplayParser}
    * @throws Exception passed from other code that throws the same
    */
    public static void main(String[] args) throws Exception {
        
        // ***** NO LONGER USING BUT MAY USE LATER TO GET OTHER PLAYER INFO ***
        // // load final hero list by grabbing match info from the file using
        // //  clarity's infoForFile
        // CDotaGameInfo dotaInfo = new Clarity().infoForFile(args[0]).getGameInfo().getDota();
        // nPlayers = dotaInfo.getPlayerInfoCount();
        // CPlayerInfo player;
        // for (int i = 0; i < nPlayers; i++) {
            // player = dotaInfo.getPlayerInfo(i);
            // heroesNames[i] = player.getHeroName();
        // }
        // ********************************************************************
        BasicConfigurator.configure();

        if (args.length == 0) {
            args = new String[]{"D:\\Dropbox\\video-game-view\\client\\deployment\\marketing\\video_examples\\example_2\\0-00.dem", "D:\\Dropbox\\video-game-view\\client\\deployment\\marketing\\video_examples\\example_2\\results.csv"};
        }
        
        // create the match analyzer (runner), which will search for item and
        //  ability changes as the match progresses
        new ReplayParser(args[0], args[1]);
    }
}