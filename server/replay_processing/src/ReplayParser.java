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

import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityPropertyChanged;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class ReplayParser {

    // CLASS VARIABLES ***************************************************** //
    
    // constants
    public static final String ABL_PATTERN = "m_hAbilities.*";
    public static final int ABILITY_SLOTS = 24;
    public static final String ABILITY_SLOT_FMT = "m_hAbilities.%04d";
    public static final String CSV_ENCODE = "utf-8";
    public static final String CSV_HED_ABILITIES = "abilities";
    public static final String CSV_HED_HERO = "hero";
    public static final String CSV_HED_ITEMS = "items";
    public static final String CSV_HED_TIME = "time";
    public static final String CSV_SEP_ARRAY = ";";
    public static final String CSV_SEP_CAT = ",";
    public static final String CSV_NEWLINE = "\n";
    public static final String ENTITY_HIDDEN_PROP = "m_bHidden";
    public static final String ENTITY_INDEX_PROP = "m_pEntity.m_nameStringableIndex";
    public static final int ENTITY_NULL = 16777215;
    public static final String GRP_STR = "CDOTAGamerulesProxy";
    public static final String HERO_PATTERN = "CDOTA_Unit_Hero_.*";
    public static final String ITEM_PATTERN = "m_hItems.*";
    public static final int ITEM_SLOTS = 17;
    public static final String ITEM_SLOT_FMT = "m_hItems.%04d";
    public static final int MAX_PLAYERS = 50;
    public static final String STRTABLE_ENTNAME = "EntityNames";
    public static final String TIME_CUR_PROP = "m_pGameRules.m_fGameTime";
    public static final Float TIME_EPS = new Float(0.01);
    public static final String TIME_FMT = "%.3f";
    public static final String TIME_PRE_PROP = "m_pGameRules.m_flPreGameStartTime";
    public static final String TIME_SEP = ":";
    public static final String TIME_STA_PROP = "m_pGameRules.m_flGameStartTime";
    public static final String TIME_TRN_PROP = "m_pGameRules.m_flStateTransitionTime";
    
    // class globals
    // public static int nPlayers;
    private final SimpleRunner runner;
    private final BufferedWriter writer;
    private Float lastTime = new Float(-9999.0);
    private int lastHero = 999999999;
    private HashMap<Integer, HashMap<String, ArrayList<Integer>>> entityHandles = new HashMap<Integer, HashMap<String, ArrayList<Integer>>>();
    private HashMap<Integer, Float> times = new HashMap<Integer, Float>();
    
    
    
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
        String itemSlotName;
        int itemID;
        int j;
        
        // get current game time
        Entities entities = ctx.getProcessor(Entities.class);
        Float gameTime = getRealGameTimeSeconds(entities);
        
        // get hero abilities and items (if hero has been set yet)
        if (hero != null) {
        
            // get the hero name and see if this is another update in the same 
            //  time. If so, skip it
            heroID = hero.getHandle();
            if (Math.abs(gameTime - lastTime) < TIME_EPS) {
                lastTime = gameTime;
                if (heroID == lastHero) {
                    return;
                }
                else {
                    lastHero = heroID;
                }
            }
            lastTime = gameTime;
            
            // get hero abilities
            entityHandles.put(heroID, new HashMap<String, ArrayList<Integer>>());
            entityHandles.get(heroID).put("abilities", new ArrayList<Integer>());
            times.put(heroID, gameTime);
            for (j = 0; j < ABILITY_SLOTS; j++){
                abilitySlotName = getAbilitySlotNameByInt(j);
                abilityID = hero.getProperty(abilitySlotName);
                if (abilityID == null) {
                    abilityID = ENTITY_NULL;
                }
                entityHandles.get(heroID).get("abilities").add(abilityID);
            }
            
            // get hero items
            entityHandles.get(heroID).put("items", new ArrayList<Integer>());
            for (j = 0; j < ITEM_SLOTS; j++){
                itemSlotName = getItemSlotNameByInt(j);
                itemID = hero.getProperty(itemSlotName);
                entityHandles.get(heroID).get("items").add(itemID);
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
    @OnTickEnd
    @UsesStringTable(STRTABLE_ENTNAME)
    @UsesEntities
    public void writeEntityNames(Context ctx, boolean synthetic) throws IOException {
        
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
        ArrayList<Integer> sublist;
        int j;
        
        // resolve hero, ability, and item names
        for (Map.Entry<Integer, HashMap<String, ArrayList<Integer>>> entry : entityHandles.entrySet()) {
            
            // hero name
            entityName = getEntityNameByHandle(entry.getKey(), entities, stringTable);
            line = String.format(TIME_FMT, times.get(entry.getKey())) + CSV_SEP_CAT + entityName + CSV_SEP_CAT; 
            
            // ability names
            sublist = entry.getValue().get("abilities");
            for (j = 0; j < ABILITY_SLOTS; j++) {
                entityID = sublist.get(j);
                
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
                        line += CSV_SEP_ARRAY + entityName;
                    }
                    else {
                        line += entityName;
                    }
                }
            }
            
            // item names
            line += CSV_SEP_CAT;
            sublist = entry.getValue().get("items");
            for (j = 0; j < ITEM_SLOTS; j++) {
                entityID = sublist.get(j);
                entityName = getEntityNameByHandle(entityID, entities, stringTable);
                if (j > 0) {
                    line += CSV_SEP_ARRAY + entityName;
                }
                else {
                    line += entityName;
                }
            }
            
            // update the time log
            writer.write(CSV_NEWLINE + line);
        }
        
        // clear the entity handle buffer so the check at the beginngin of this
        // method passes unless heroUpdate() adds something to it
        entityHandles.clear();
    }
    
    
    /**
    * Uses the OnEntityPropertyChanged decorator to automatically update hero status during a replay.
    * <p>
    * This is essentially a listener for changes to hero items during a replay.
    * It uses such events to update hero items and abilities.
    *
    * @param ctx  the {@code Context} of the current runner
    * @param hero  the hero {@code Entity} for which to get updates
    * @param fp  not used, present by necessity with the {@code OnEntityPropertyChanged} decorator
    * @throws IOException passed from other code that throws the same
    */
    @OnEntityPropertyChanged(classPattern = HERO_PATTERN, propertyPattern = ITEM_PATTERN)
    public void onItemChange(Context ctx, Entity hero, FieldPath fp) throws IOException {
        heroUpdate(ctx, hero);
    }
    
    
    /**
    * Uses the OnEntityPropertyChanged decorator to automatically update hero status during a replay.
    * <p>
    * This is essentially a listener for changes to hero abilities during a replay.
    * It uses such events to update hero items and abilities.
    *
    * @param ctx  the {@code Context} of the current runner
    * @param hero  the hero {@code Entity} for which to get updates
    * @param fp  not used, present by necessity with the {@code OnEntityPropertyChanged} decorator
    * @throws IOException passed from other code that throws the same
    */
    @OnEntityPropertyChanged(classPattern = HERO_PATTERN, propertyPattern = ABL_PATTERN)
    public void onAbilityChange(Context ctx, Entity hero, FieldPath fp) throws IOException {
        heroUpdate(ctx, hero);
    }
    
    
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
        
        // create the match analyzer (runner), which will search for item and
        //  ability changes as the match progresses
        new ReplayParser(args[0], args[1]);
    }
}