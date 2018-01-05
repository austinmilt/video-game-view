/**
 * Copyright 2018 Austin Walker Milt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

var ABILITY_STATES = {
    '40': 'ability s-4 s-4-0',
    '41': 'ability s-4 s-4-1',
    '42': 'ability s-4 s-4-2',
    '43': 'ability s-4 s-4-3',
    '50': 'ability s-5 s-5-0',
    '51': 'ability s-5 s-5-1',
    '52': 'ability s-5 s-5-2',
    '53': 'ability s-5 s-5-3',
    '54': 'ability s-5 s-5-4',
    '60': 'ability s-6 s-6-0',
    '61': 'ability s-6 s-6-1',
    '62': 'ability s-6 s-6-2',
    '63': 'ability s-6 s-6-3',
    '64': 'ability s-6 s-6-4',
    '65': 'ability s-6 s-6-5',
    'ABADDON': ['003', '002', '001', '000'],
    'ALCHEMIST': ['007', '006', '005', '004', '008'],
    'ANCIENT APPARITION': ['010', '011', '012', '013', '009'],
    'ANTI-MAGE': ['014', '015', '016', '017'],
    'ARC WARDEN': ['018', '019', '021', '020'],
    'AXE': ['025', '024', '023', '022'],
    'BANE': ['030', '027', '026', '029', '028'],
    'BATRIDER': ['032', '033', '031', '034'],
    'BEASTMASTER': ['038', '039', '036', '037', '035'],
    'BLOODSEEKER': ['043', '042', '041', '040'],
    'BOUNTY HUNTER': ['047', '046', '045', '044'],
    'BREWMASTER': ['050', '051', '049', '048'],
    'BRISTLEBACK': ['054', '055', '052', '053'],
    'BROODMOTHER': ['056', '057', '058', '059'],
    'CENTAUR WARRUNNER': ['061', '060', '063', '062'],
    'CHAOS KNIGHT': ['065', '064', '067', '066'],
    'CHEN': ['072', '070', '071', '069', '068'],
    'CLINKZ': ['076', '074', '075', '073'],
    'CLOCKWERK': ['077', '078', '079', '080'],
    'CRYSTAL MAIDEN': ['083', '082', '081', '084'],
    'DARK SEER': ['088', '087', '086', '085'],
    'DAZZLE': ['090', '091', '092', '089'],
    'DEATH PROPHET': ['093', '094', '095', '096'],
    'DISRUPTOR': ['100', '098', '099', '097'],
    'DOOM': ['102', '103', '101', '104'],
    'DRAGON KNIGHT': ['108', '106', '107', '105'],
    'DROW RANGER': ['109', '111', '110', '112'],
    'EARTH SPIRIT': ['120', '121', '122', '115', '114', '117', '116', '113', '119', '118'],
    'EARTHSHAKER': ['120', '121', '122', '115', '114', '117', '116', '113', '119', '118'],
    'ELDER TITAN': ['123', '124', '125', '126', '127'],
    'EMBER SPIRIT': ['128', '129', '132', '131', '130'],
    'ENCHANTRESS': ['133', '136', '135', '134'],
    'ENIGMA': ['140', '137', '139', '138'],
    'FACELESS VOID': ['144', '142', '143', '141'],
    'GYROCOPTER': ['146', '147', '145', '148'],
    'HUSKAR': ['149', '151', '150', '152'],
    'INVOKER': ['164', '165', '166', '160', '161', '162', '163', '153', '155', '154', '157', '156', '159', '158'],
    'IO': ['167', '168', '169', '170', '171', '172', '173'],
    'JAKIRO': ['177', '176', '175', '174'],
    'JUGGERNAUT': ['179', '178', '180', '181'],
    'KEEPER OF THE LIGHT': ['182', '183', '186', '187', '184', '185', '188'],
    'KUNKKA': ['191', '190', '193', '192', '189'],
    'LEGION COMMANDER': ['195', '194', '197', '196'],
    'LESHRAC': ['199', '198', '201', '200'],
    'LICH': ['203', '202', '205', '204'],
    'LIFESTEALER': ['210', '211', '209', '208', '207', '206'],
    'LINA': ['214', '215', '212', '213'],
    'LION': ['216', '217', '219', '218'],
    'LONE DRUID': ['225', '224', '223', '222', '221', '220'],
    'LUNA': ['229', '228', '227', '226'],
    'LYCAN': ['230', '231', '232', '233'],
    'MAGNUS': ['234', '235', '236', '237'],
    'MEDUSA': ['238', '239', '241', '240'],
    'MEEPO': ['243', '242', '245', '244'],
    'MIRANA': ['249', '248', '247', '246'],
    'MORPHLING': ['252', '250', '251', '256', '254', '255', '253'],
    'NAGA SIREN': ['258', '261', '260', '259', '257'],
    'NATURES PROPHET': ['262', '263', '264', '265'],
    'NECROPHOS': ['267', '266', '269', '268'],
    'NIGHT STALKER': ['270', '271', '272', '273'],
    'NYX ASSASSIN': ['274', '275', '276', '277', '278', '279'],
    'OGRE MAGI': ['281', '280', '283', '282', '284'],
    'OMNIKNIGHT': ['288', '285', '287', '286'],
    'ORACLE': ['292', '290', '291', '289'],
    'OUTWORLD DEVOURER': ['293', '294', '295', '296'],
    'PHANTOM ASSASSIN': ['298', '299', '297', '300', '301', '302', '303', '304'],
    'PHANTOM LANCER': ['298', '299', '297', '300', '301', '302', '303', '304'],
    'PHOENIX': ['312', '310', '308', '309', '305', '306', '307', '311'],
    'PUCK': ['317', '313', '314', '316', '315'],
    'PUDGE': ['319', '318', '320', '321'],
    'PUGNA': ['322', '323', '324', '325'],
    'QUEEN OF PAIN': ['326', '327', '328', '329'],
    'RAZOR': ['331', '330', '333', '332'],
    'RIKI': ['335', '334', '337', '336'],
    'RUBICK': ['340', '341', '342', '339', '338'],
    'SAND KING': ['344', '345', '346', '343'],
    'SHADOW DEMON': ['347', '348', '349', '360', '361', '351', '350', '359', '358'],
    'SHADOW FIEND': ['347', '348', '349', '360', '361', '351', '350', '359', '358'],
    'SHADOW SHAMAN': ['347', '348', '349', '360', '361', '351', '350', '359', '358'],
    'SILENCER': ['364', '365', '362', '363'],
    'SKYWRATH MAGE': ['368', '369', '367', '366'],
    'SLARDAR': ['370', '371', '373', '372', '375', '374', '377', '376'],
    'SLARK': ['370', '371', '373', '372', '375', '374', '377', '376'],
    'SNIPER': ['380', '381', '379', '378'],
    'SPECTRE': ['384', '385', '386', '382', '383'],
    'SPIRIT BREAKER': ['387', '389', '390', '388'],
    'STORM SPIRIT': ['391', '393', '392', '394'],
    'SVEN': ['395', '398', '397', '396'],
    'TECHIES': ['404', '403', '402', '401', '400', '399'],
    'TEMPLAR ASSASSIN': ['407', '406', '409', '408', '405'],
    'TERRORBLADE': ['410', '411', '412', '413'],
    'TIDEHUNTER': ['414', '415', '416', '417'],
    'TIMBERSAW': ['418', '419', '420', '421', '422', '423', '424'],
    'TINKER': ['428', '432', '431', '429', '425', '427', '426', '430'],
    'TINY': ['428', '432', '431', '429', '425', '427', '426', '430'],
    'TREANT PROTECTOR': ['436', '437', '434', '435', '433'],
    'TROLL WARLORD': ['442', '441', '440', '438', '439'],
    'TUSK': ['448', '443', '447', '446', '445', '444'],
    'UNDERLORD': ['454', '455', '456', '457'],
    'UNDYING': ['454', '455', '456', '457'],
    'URSA': ['461', '460', '458', '459'],
    'VENGEFUL SPIRIT': ['469', '468', '465', '464', '467', '466', '463', '462'],
    'VENOMANCER': ['469', '468', '465', '464', '467', '466', '463', '462'],
    'VIPER': ['473', '472', '470', '471'],
    'VISAGE': ['476', '477', '474', '475'],
    'WARLOCK': ['481', '480', '479', '478'],
    'WEAVER': ['485', '484', '483', '482'],
    'WINDRANGER': ['493', '490', '491', '492', '489', '488', '487', '486'],
    'WINTER WYVERN': ['493', '490', '491', '492', '489', '488', '487', '486'],
    'WITCH DOCTOR': ['497', '494', '495', '496'],
    'WRAITH KING': ['498', '499', '500', '501'],
    'ZEUS': ['502', '503', '504', '505'],
    '000': 'abaddon_death_coil',
    '001': 'abaddon_aphotic_shield',
    '002': 'abaddon_frostmourne',
    '003': 'abaddon_borrowed_time',
    '004': 'alchemist_acid_spray',
    '005': 'alchemist_unstable_concoction',
    '006': 'alchemist_unstable_concoction_throw',
    '007': 'alchemist_goblins_greed',
    '008': 'alchemist_chemical_rage',
    '009': 'ancient_apparition_cold_feet',
    '010': 'ancient_apparition_ice_vortex',
    '011': 'ancient_apparition_chilling_touch',
    '012': 'ancient_apparition_ice_blast',
    '013': 'ancient_apparition_ice_blast_release',
    '014': 'antimage_mana_break',
    '015': 'antimage_blink',
    '016': 'antimage_spell_shield',
    '017': 'antimage_mana_void',
    '018': 'arc_warden_flux',
    '019': 'arc_warden_magnetic_field',
    '020': 'arc_warden_spark_wraith',
    '021': 'arc_warden_tempest_double',
    '022': 'axe_berserkers_call',
    '023': 'axe_battle_hunger',
    '024': 'axe_counter_helix',
    '025': 'axe_culling_blade',
    '026': 'bane_enfeeble',
    '027': 'bane_brain_sap',
    '028': 'bane_nightmare',
    '029': 'bane_nightmare_end',
    '030': 'bane_fiends_grip',
    '031': 'batrider_sticky_napalm',
    '032': 'batrider_flamebreak',
    '033': 'batrider_firefly',
    '034': 'batrider_flaming_lasso',
    '035': 'beastmaster_wild_axes',
    '036': 'beastmaster_call_of_the_wild',
    '037': 'beastmaster_call_of_the_wild',
    '038': 'beastmaster_inner_beast',
    '039': 'beastmaster_primal_roar',
    '040': 'bloodseeker_bloodrage',
    '041': 'bloodseeker_blood_bath',
    '042': 'bloodseeker_thirst',
    '043': 'bloodseeker_rupture',
    '044': 'bounty_hunter_shuriken_toss',
    '045': 'bounty_hunter_jinada',
    '046': 'bounty_hunter_wind_walk',
    '047': 'bounty_hunter_track',
    '048': 'brewmaster_thunder_clap',
    '049': 'brewmaster_drunken_haze',
    '050': 'brewmaster_drunken_brawler',
    '051': 'brewmaster_primal_split',
    '052': 'bristleback_viscous_nasal_goo',
    '053': 'bristleback_quill_spray',
    '054': 'bristleback_bristleback',
    '055': 'bristleback_warpath',
    '056': 'broodmother_spawn_spiderlings',
    '057': 'broodmother_spin_web',
    '058': 'broodmother_incapacitating_bite',
    '059': 'broodmother_insatiable_hunger',
    '060': 'centaur_hoof_stomp',
    '061': 'centaur_double_edge',
    '062': 'centaur_return',
    '063': 'centaur_stampede',
    '064': 'chaos_knight_chaos_bolt',
    '065': 'chaos_knight_reality_rift',
    '066': 'chaos_knight_chaos_strike',
    '067': 'chaos_knight_phantasm',
    '068': 'chen_penitence',
    '069': 'chen_test_of_faith',
    '070': 'chen_test_of_faith_teleport',
    '071': 'chen_holy_persuasion',
    '072': 'chen_hand_of_god',
    '073': 'clinkz_strafe',
    '074': 'clinkz_searing_arrows',
    '075': 'clinkz_wind_walk',
    '076': 'clinkz_death_pact',
    '077': 'rattletrap_battery_assault',
    '078': 'rattletrap_power_cogs',
    '079': 'rattletrap_rocket_flare',
    '080': 'rattletrap_hookshot',
    '081': 'crystal_maiden_crystal_nova',
    '082': 'crystal_maiden_frostbite',
    '083': 'crystal_maiden_brilliance_aura',
    '084': 'crystal_maiden_freezing_field',
    '085': 'dark_seer_vacuum',
    '086': 'dark_seer_ion_shell',
    '087': 'dark_seer_surge',
    '088': 'dark_seer_wall_of_replica',
    '089': 'dazzle_poison_touch',
    '090': 'dazzle_shallow_grave',
    '091': 'dazzle_shadow_wave',
    '092': 'dazzle_weave',
    '093': 'death_prophet_carrion_swarm',
    '094': 'death_prophet_silence',
    '095': 'death_prophet_spirit_siphon',
    '096': 'death_prophet_exorcism',
    '097': 'disruptor_thunder_strike',
    '098': 'disruptor_glimpse',
    '099': 'disruptor_kinetic_field',
    '100': 'disruptor_static_storm',
    '101': 'doom_bringer_devour',
    '102': 'doom_bringer_scorched_earth',
    '103': 'doom_bringer_infernal_blade',
    '104': 'doom_bringer_doom',
    '105': 'dragon_knight_breathe_fire',
    '106': 'dragon_knight_dragon_tail',
    '107': 'dragon_knight_dragon_blood',
    '108': 'dragon_knight_elder_dragon_form',
    '109': 'drow_ranger_frost_arrows',
    '110': 'drow_ranger_wave_of_silence',
    '111': 'drow_ranger_trueshot',
    '112': 'drow_ranger_marksmanship',
    '113': 'earth_spirit_boulder_smash',
    '114': 'earth_spirit_rolling_boulder',
    '115': 'earth_spirit_geomagnetic_grip',
    '116': 'earth_spirit_stone_caller',
    '117': 'earth_spirit_petrify',
    '118': 'earth_spirit_magnetize',
    '119': 'earthshaker_fissure',
    '120': 'earthshaker_enchant_totem',
    '121': 'earthshaker_aftershock',
    '122': 'earthshaker_echo_slam',
    '123': 'elder_titan_echo_stomp',
    '124': 'elder_titan_ancestral_spirit',
    '125': 'elder_titan_return_spirit',
    '126': 'elder_titan_natural_order',
    '127': 'elder_titan_earth_splitter',
    '128': 'ember_spirit_searing_chains',
    '129': 'ember_spirit_sleight_of_fist',
    '130': 'ember_spirit_flame_guard',
    '131': 'ember_spirit_activate_fire_remnant',
    '132': 'ember_spirit_fire_remnant',
    '133': 'enchantress_untouchable',
    '134': 'enchantress_enchant',
    '135': 'enchantress_natures_attendants',
    '136': 'enchantress_impetus',
    '137': 'enigma_malefice',
    '138': 'enigma_demonic_conversion',
    '139': 'enigma_midnight_pulse',
    '140': 'enigma_black_hole',
    '141': 'faceless_void_time_walk',
    '142': 'faceless_void_time_dilation',
    '143': 'faceless_void_time_lock',
    '144': 'faceless_void_chronosphere',
    '145': 'gyrocopter_rocket_barrage',
    '146': 'gyrocopter_homing_missile',
    '147': 'gyrocopter_flak_cannon',
    '148': 'gyrocopter_call_down',
    '149': 'huskar_inner_vitality',
    '150': 'huskar_burning_spear',
    '151': 'huskar_berserkers_blood',
    '152': 'huskar_life_break',
    '153': 'invoker_quas',
    '154': 'invoker_wex',
    '155': 'invoker_exort',
    '156': 'invoker_cold_snap',
    '157': 'invoker_ghost_walk',
    '158': 'invoker_tornado',
    '159': 'invoker_emp',
    '160': 'invoker_alacrity',
    '161': 'invoker_chaos_meteor',
    '162': 'invoker_sun_strike',
    '163': 'invoker_forge_spirit',
    '164': 'invoker_ice_wall',
    '165': 'invoker_deafening_blast',
    '166': 'invoker_invoke',
    '167': 'wisp_tether',
    '168': 'wisp_tether_break',
    '169': 'wisp_spirits',
    '170': 'wisp_overcharge',
    '171': 'wisp_spirits_in',
    '172': 'wisp_spirits_out',
    '173': 'wisp_relocate',
    '174': 'jakiro_dual_breath',
    '175': 'jakiro_ice_path',
    '176': 'jakiro_liquid_fire',
    '177': 'jakiro_macropyre',
    '178': 'juggernaut_blade_fury',
    '179': 'juggernaut_healing_ward',
    '180': 'juggernaut_blade_dance',
    '181': 'juggernaut_omni_slash',
    '182': 'keeper_of_the_light_illuminate',
    '183': 'keeper_of_the_light_illuminate_end',
    '184': 'keeper_of_the_light_mana_leak',
    '185': 'keeper_of_the_light_chakra_magic',
    '186': 'keeper_of_the_light_recall',
    '187': 'keeper_of_the_light_blinding_light',
    '188': 'keeper_of_the_light_spirit_form',
    '189': 'kunkka_torrent',
    '190': 'kunkka_tidebringer',
    '191': 'kunkka_x_marks_the_spot',
    '192': 'kunkka_return',
    '193': 'kunkka_ghostship',
    '194': 'legion_commander_overwhelming_odds',
    '195': 'legion_commander_press_the_attack',
    '196': 'legion_commander_moment_of_courage',
    '197': 'legion_commander_duel',
    '198': 'leshrac_split_earth',
    '199': 'leshrac_diabolic_edict',
    '200': 'leshrac_lightning_storm',
    '201': 'leshrac_pulse_nova',
    '202': 'lich_frost_nova',
    '203': 'lich_frost_armor',
    '204': 'lich_dark_ritual',
    '205': 'lich_chain_frost',
    '206': 'life_stealer_rage',
    '207': 'life_stealer_feast',
    '208': 'life_stealer_open_wounds',
    '209': 'life_stealer_assimilate',
    '210': 'life_stealer_assimilate_eject',
    '211': 'life_stealer_infest',
    '212': 'lina_dragon_slave',
    '213': 'lina_light_strike_array',
    '214': 'lina_fiery_soul',
    '215': 'lina_laguna_blade',
    '216': 'lion_impale',
    '217': 'lion_voodoo',
    '218': 'lion_mana_drain',
    '219': 'lion_finger_of_death',
    '220': 'lone_druid_spirit_bear',
    '221': 'lone_druid_rabid',
    '222': 'lone_druid_savage_roar',
    '223': 'lone_druid_true_form_battle_cry',
    '224': 'lone_druid_true_form',
    '225': 'lone_druid_true_form_druid',
    '226': 'luna_lucent_beam',
    '227': 'luna_moon_glaive',
    '228': 'luna_lunar_blessing',
    '229': 'luna_eclipse',
    '230': 'lycan_summon_wolves',
    '231': 'lycan_howl',
    '232': 'lycan_feral_impulse',
    '233': 'lycan_shapeshift',
    '234': 'magnataur_shockwave',
    '235': 'magnataur_empower',
    '236': 'magnataur_skewer',
    '237': 'magnataur_reverse_polarity',
    '238': 'medusa_split_shot',
    '239': 'medusa_mystic_snake',
    '240': 'medusa_mana_shield',
    '241': 'medusa_stone_gaze',
    '242': 'meepo_earthbind',
    '243': 'meepo_poof',
    '244': 'meepo_geostrike',
    '245': 'meepo_divided_we_stand',
    '246': 'mirana_starfall',
    '247': 'mirana_arrow',
    '248': 'mirana_leap',
    '249': 'mirana_invis',
    '250': 'morphling_waveform',
    '251': 'morphling_adaptive_strike',
    '252': 'morphling_morph_agi',
    '253': 'morphling_morph_str',
    '254': 'morphling_hybrid',
    '255': 'morphling_replicate',
    '256': 'morphling_morph_replicate',
    '257': 'naga_siren_mirror_image',
    '258': 'naga_siren_ensnare',
    '259': 'naga_siren_rip_tide',
    '260': 'naga_siren_song_of_the_siren',
    '261': 'naga_siren_song_of_the_siren_cancel',
    '262': 'furion_sprout',
    '263': 'furion_teleportation',
    '264': 'furion_force_of_nature',
    '265': 'furion_wrath_of_nature',
    '266': 'necrolyte_death_pulse',
    '267': 'necrolyte_heartstopper_aura',
    '268': 'necrolyte_sadist',
    '269': 'necrolyte_reapers_scythe',
    '270': 'night_stalker_void',
    '271': 'night_stalker_crippling_fear',
    '272': 'night_stalker_hunter_in_the_night',
    '273': 'night_stalker_darkness',
    '274': 'nyx_assassin_impale',
    '275': 'nyx_assassin_mana_burn',
    '276': 'nyx_assassin_spiked_carapace',
    '277': 'nyx_assassin_burrow',
    '278': 'nyx_assassin_unburrow',
    '279': 'nyx_assassin_vendetta',
    '280': 'ogre_magi_fireblast',
    '281': 'ogre_magi_ignite',
    '282': 'ogre_magi_bloodlust',
    '283': 'ogre_magi_unrefined_fireblast',
    '284': 'ogre_magi_multicast',
    '285': 'omniknight_purification',
    '286': 'omniknight_repel',
    '287': 'omniknight_degen_aura',
    '288': 'omniknight_guardian_angel',
    '289': 'oracle_fortunes_end',
    '290': 'oracle_fates_edict',
    '291': 'oracle_purifying_flames',
    '292': 'oracle_false_promise',
    '293': 'obsidian_destroyer_arcane_orb',
    '294': 'obsidian_destroyer_astral_imprisonment',
    '295': 'obsidian_destroyer_essence_aura',
    '296': 'obsidian_destroyer_sanity_eclipse',
    '297': 'phantom_assassin_stifling_dagger',
    '298': 'phantom_assassin_phantom_strike',
    '299': 'phantom_assassin_blur',
    '300': 'phantom_assassin_coup_de_grace',
    '301': 'phantom_lancer_spirit_lance',
    '302': 'phantom_lancer_doppelwalk',
    '303': 'phantom_lancer_phantom_edge',
    '304': 'phantom_lancer_juxtapose',
    '305': 'phoenix_icarus_dive',
    '306': 'phoenix_icarus_dive_stop',
    '307': 'phoenix_fire_spirits',
    '308': 'phoenix_launch_fire_spirit',
    '309': 'phoenix_sun_ray',
    '310': 'phoenix_sun_ray_stop',
    '311': 'phoenix_sun_ray_toggle_move',
    '312': 'phoenix_supernova',
    '313': 'puck_illusory_orb',
    '314': 'puck_waning_rift',
    '315': 'puck_phase_shift',
    '316': 'puck_ethereal_jaunt',
    '317': 'puck_dream_coil',
    '318': 'pudge_meat_hook',
    '319': 'pudge_rot',
    '320': 'pudge_flesh_heap',
    '321': 'pudge_dismember',
    '322': 'pugna_nether_blast',
    '323': 'pugna_decrepify',
    '324': 'pugna_nether_ward',
    '325': 'pugna_life_drain',
    '326': 'queenofpain_shadow_strike',
    '327': 'queenofpain_blink',
    '328': 'queenofpain_scream_of_pain',
    '329': 'queenofpain_sonic_wave',
    '330': 'razor_plasma_field',
    '331': 'razor_static_link',
    '332': 'razor_unstable_current',
    '333': 'razor_eye_of_the_storm',
    '334': 'riki_smoke_screen',
    '335': 'riki_blink_strike',
    '336': 'riki_permanent_invisibility',
    '337': 'riki_tricks_of_the_trade',
    '338': 'rubick_telekinesis',
    '339': 'rubick_telekinesis_land',
    '340': 'rubick_fade_bolt',
    '341': 'rubick_null_field',
    '342': 'rubick_spell_steal',
    '343': 'sandking_burrowstrike',
    '344': 'sandking_sand_storm',
    '345': 'sandking_caustic_finale',
    '346': 'sandking_epicenter',
    '347': 'shadow_demon_disruption',
    '348': 'shadow_demon_soul_catcher',
    '349': 'shadow_demon_shadow_poison',
    '350': 'shadow_demon_shadow_poison_release',
    '351': 'shadow_demon_demonic_purge',
    '352': 'nevermore_shadowraze1',
    '353': 'nevermore_shadowraze1',
    '354': 'nevermore_shadowraze1',
    '355': 'nevermore_necromastery',
    '356': 'nevermore_dark_lord',
    '357': 'nevermore_requiem',
    '358': 'shadow_shaman_ether_shock',
    '359': 'shadow_shaman_voodoo',
    '360': 'shadow_shaman_shackles',
    '361': 'shadow_shaman_mass_serpent_ward',
    '362': 'silencer_curse_of_the_silent',
    '363': 'silencer_glaives_of_wisdom',
    '364': 'silencer_last_word',
    '365': 'silencer_global_silence',
    '366': 'skywrath_mage_arcane_bolt',
    '367': 'skywrath_mage_concussive_shot',
    '368': 'skywrath_mage_ancient_seal',
    '369': 'skywrath_mage_mystic_flare',
    '370': 'slardar_sprint',
    '371': 'slardar_slithereen_crush',
    '372': 'slardar_bash',
    '373': 'slardar_amplify_damage',
    '374': 'slark_dark_pact',
    '375': 'slark_pounce',
    '376': 'slark_essence_shift',
    '377': 'slark_shadow_dance',
    '378': 'sniper_shrapnel',
    '379': 'sniper_headshot',
    '380': 'sniper_take_aim',
    '381': 'sniper_assassinate',
    '382': 'spectre_spectral_dagger',
    '383': 'spectre_desolate',
    '384': 'spectre_dispersion',
    '385': 'spectre_reality',
    '386': 'spectre_haunt',
    '387': 'spirit_breaker_charge_of_darkness',
    '388': 'spirit_breaker_empowering_haste',
    '389': 'spirit_breaker_greater_bash',
    '390': 'spirit_breaker_nether_strike',
    '391': 'storm_spirit_static_remnant',
    '392': 'storm_spirit_electric_vortex',
    '393': 'storm_spirit_overload',
    '394': 'storm_spirit_ball_lightning',
    '395': 'sven_storm_bolt',
    '396': 'sven_great_cleave',
    '397': 'sven_warcry',
    '398': 'sven_gods_strength',
    '399': 'techies_land_mines',
    '400': 'techies_stasis_trap',
    '401': 'techies_suicide',
    '402': 'techies_focused_detonate',
    '403': 'techies_minefield_sign',
    '404': 'techies_remote_mines',
    '405': 'templar_assassin_refraction',
    '406': 'templar_assassin_meld',
    '407': 'templar_assassin_psi_blades',
    '408': 'templar_assassin_trap',
    '409': 'templar_assassin_psionic_trap',
    '410': 'terrorblade_reflection',
    '411': 'terrorblade_conjure_image',
    '412': 'terrorblade_metamorphosis',
    '413': 'terrorblade_sunder',
    '414': 'tidehunter_gush',
    '415': 'tidehunter_kraken_shell',
    '416': 'tidehunter_anchor_smash',
    '417': 'tidehunter_ravage',
    '418': 'shredder_whirling_death',
    '419': 'shredder_timber_chain',
    '420': 'shredder_reactive_armor',
    '421': 'shredder_chakram_2',
    '422': 'shredder_return_chakram_2',
    '423': 'shredder_chakram',
    '424': 'shredder_return_chakram',
    '425': 'tinker_laser',
    '426': 'tinker_heat_seeking_missile',
    '427': 'tinker_march_of_the_machines',
    '428': 'tinker_rearm',
    '429': 'tiny_avalanche',
    '430': 'tiny_toss',
    '431': 'tiny_craggy_exterior',
    '432': 'tiny_grow',
    '433': 'treant_natures_guise',
    '434': 'treant_leech_seed',
    '435': 'treant_living_armor',
    '436': 'treant_eyes_in_the_forest',
    '437': 'treant_overgrowth',
    '438': 'troll_warlord_berserkers_rage',
    '439': 'troll_warlord_whirling_axes_ranged',
    '440': 'troll_warlord_whirling_axes_melee',
    '441': 'troll_warlord_fervor',
    '442': 'troll_warlord_battle_trance',
    '443': 'tusk_ice_shards',
    '444': 'tusk_snowball',
    '445': 'tusk_launch_snowball',
    '446': 'tusk_frozen_sigil',
    '447': 'tusk_walrus_kick',
    '448': 'tusk_walrus_punch',
    '449': 'abyssal_underlord_firestorm',
    '450': 'abyssal_underlord_pit_of_malice',
    '451': 'abyssal_underlord_atrophy_aura',
    '452': 'abyssal_underlord_dark_rift',
    '453': 'abyssal_underlord_cancel_dark_rift',
    '454': 'undying_decay',
    '455': 'undying_soul_rip',
    '456': 'undying_tombstone',
    '457': 'undying_flesh_golem',
    '458': 'ursa_earthshock',
    '459': 'ursa_overpower',
    '460': 'ursa_fury_swipes',
    '461': 'ursa_enrage',
    '462': 'vengefulspirit_magic_missile',
    '463': 'vengefulspirit_wave_of_terror',
    '464': 'vengefulspirit_command_aura',
    '465': 'vengefulspirit_nether_swap',
    '466': 'venomancer_venomous_gale',
    '467': 'venomancer_poison_sting',
    '468': 'venomancer_plague_ward',
    '469': 'venomancer_poison_nova',
    '470': 'viper_poison_attack',
    '471': 'viper_nethertoxin',
    '472': 'viper_corrosive_skin',
    '473': 'viper_viper_strike',
    '474': 'visage_grave_chill',
    '475': 'visage_soul_assumption',
    '476': 'visage_gravekeepers_cloak',
    '477': 'visage_summon_familiars',
    '478': 'warlock_fatal_bonds',
    '479': 'warlock_shadow_word',
    '480': 'warlock_upheaval',
    '481': 'warlock_rain_of_chaos',
    '482': 'weaver_the_swarm',
    '483': 'weaver_shukuchi',
    '484': 'weaver_geminate_attack',
    '485': 'weaver_time_lapse',
    '486': 'windrunner_shackleshot',
    '487': 'windrunner_powershot',
    '488': 'windrunner_windrun',
    '489': 'windrunner_focusfire',
    '490': 'winter_wyvern_arctic_burn',
    '491': 'winter_wyvern_splinter_blast',
    '492': 'winter_wyvern_cold_embrace',
    '493': 'winter_wyvern_winters_curse',
    '494': 'witch_doctor_paralyzing_cask',
    '495': 'witch_doctor_voodoo_restoration',
    '496': 'witch_doctor_maledict',
    '497': 'witch_doctor_death_ward',
    '498': 'skeleton_king_hellfire_blast',
    '499': 'skeleton_king_vampiric_aura',
    '500': 'skeleton_king_mortal_strike',
    '501': 'skeleton_king_reincarnation',
    '502': 'zuus_arc_lightning',
    '503': 'zuus_lightning_bolt',
    '504': 'zuus_static_field',
    '505': 'zuus_thundergods_wrath'
}