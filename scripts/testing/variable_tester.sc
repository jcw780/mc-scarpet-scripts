__config() -> {
    'commands' -> {
        'sort_mixed <input_pos> <mixed_pos> <single_pos>' -> ['sort_mixed', true],
        'sort_mixed <input_pos> <mixed_pos> <single_pos> <recolor>' -> 'sort_mixed',
        'start_randomize_items <intended_items_pos> <interference_items_pos> <intended_spread> <interference_spread> <mean_duration> <interference_chance> <drop_position_pos> <pause_chance> <pause_duration>' -> 'randomize_items',
        'start_randomize_items <intended_items_pos> <interference_items_pos> <intended_spread> <interference_spread> <mean_duration> <interference_chance> <drop_position_pos>' -> ['randomize_items', 0.0, 1],
        'stop_randomize_items' -> 'randomize_items_stop',
        'randomize_items_display_last_intended <summary>' -> 'randomize_items_display_last_intended',
    },
    'arguments' -> {
        'intended_spread' -> {'type' -> 'int', 'min' -> 0, 'max' -> 64, 'suggest' -> [8]},
        'interference_spread' -> {'type' -> 'int', 'min' -> 0, 'max' -> 64, 'suggest' -> [4]},
        'mean_duration' -> {'type' -> 'int', 'min' -> 1, 'suggest' -> [2000, 2500, 3000]},
        'interference_chance' -> {'type' -> 'float', 'min' -> 0.0, 'max' -> 1.0, 'suggest' -> [0.01, 0.025]},
        'pause_chance' -> {'type' -> 'float', 'min' -> 0.0, 'max' -> 1.0, 'suggest' -> [0.1]},
        'pause_duration' -> {'type' -> 'int', 'min' -> 1, 'suggest' -> [500, 1000]},
        'summary' -> {'type' -> 'bool', 'suggest' -> [true]},
        'recolor' -> {'type' -> 'bool', 'suggest' -> [true]},
    },
    'strict' -> true,
    'scope' -> 'global',
};

sort_mixed(input_pos, mixed_pos, single_pos, recolor) -> (
    input_block = block(input_pos);
    mixed_block = block(mixed_pos);
    single_block = block(single_pos);
    inv_size = inventory_size(input_block);
    c_for(slot = 0, slot < inv_size, slot +=1,
        item = inventory_get(input_block, slot);
        if(item:0 == null, continue());
        mixed = false;
        empty_box = true;
        full_box_slots = 0;
        if(item:0 == 'shulker_box',
            if((box_items = parse_nbt(item:2:'components'):'minecraft:container') != null,
                empty_box = false;
                first_item = null;
                for(box_items,
                    inner_item_type = _:'item':'id';
                    if(stack_limit(inner_item_type) == _:'item':'count', full_box_slots += 1);
                    if(first_item == null, first_item = inner_item_type,
                        if(first_item != inner_item_type,
                            mixed = true;
                        );
                    );
                );
            );
        ,
            mixed = true;
        );

        destination = if(mixed, mixed_block, single_block);
        if ((dest_slot = inventory_find(destination, null)) != null,
            moved_nbt = item:2;
            if(recolor,
                if(empty_box,
                    moved_nbt:'id' = 'white_shulker_box';,
                    if(full_box_slots == 27,
                        moved_nbt:'id' = 'black_shulker_box';
                    );
                );
            );

            inventory_set(destination, dest_slot, item:1, item:0, moved_nbt);
            inventory_set(input_block, slot, 0);
        ,
            print(str('Cannot find free slot in %s at: %s', if(mixed, 'mixed', 'sorted'), if(mixed, mixed_pos, single_pos)));
        );
    );
);

global_run_randomize_items = false;
global_last_intended_items = [];
global_last_intended_times = [];
global_last_intended_size = 16;
global_last_intended_next_idx = 0;
global_tick_counter = -1;

randomize_items(intended_items_pos, interference_items_pos,
    intended_spread, interference_spread,
    mean_duration, interference_chance,
    drop_position, pause_start_chance, mean_pause_duration) -> (
    global_last_intended_items = [];
    c_for(i=0, i<global_last_intended_size, i+=1,
        put(global_last_intended_items, null, null);
        put(global_last_intended_times, null, -1);
    );
    global_run_randomize_items = true;
    intended_switch_chance = 1-euler ^ (-1 / mean_duration);
    pause_stop_chance = 1-euler ^ (-1 / mean_pause_duration);
    print(str('Switch Chance / gt: %f \n Pause Stop Chance / gt: %f', intended_switch_chance, pause_stop_chance));

    global_tick_counter = 0;
    current_intended = randomly_pick_from_inventory(intended_items_pos, null);
    add_to_last_intended(current_intended);
    randomize_items_run(intended_items_pos, interference_items_pos,
        intended_spread, interference_spread,
        intended_switch_chance, interference_chance,
        pause_start_chance, pause_stop_chance,
        drop_position, current_intended);
);

randomize_items_run(intended_items_pos, interference_items_pos,
                    intended_spread, interference_spread,
                    intended_switch_chance, interference_chance,
                    pause_start_chance, pause_stop_chance,
                    drop_position, current_intended) -> (
    if(interference_chance >= rand(1.0),
        randomly_drop_in_block(drop_position, randomly_pick_from_inventory(interference_items_pos, null), interference_spread);
    );

    if(current_intended == null,
        if(pause_stop_chance >= rand(1.0),
            current_intended = randomly_pick_from_inventory(intended_items_pos, current_intended);
            add_to_last_intended(current_intended);
        ),
        if(intended_switch_chance >= rand(1.0),
            current_intended = if(and(pause_start_chance != 0.0, pause_start_chance >= rand(1.0)), null,
                randomly_pick_from_inventory(intended_items_pos, current_intended)
            );
            add_to_last_intended(current_intended);
        );
    );

    if(current_intended != null,
        randomly_drop_in_block(drop_position, current_intended, intended_spread);
    );

    if(global_run_randomize_items,
        schedule(1, 'randomize_items_run',
            intended_items_pos, interference_items_pos,
            intended_spread, interference_spread,
            intended_switch_chance, interference_chance,
            pause_start_chance, pause_stop_chance,
            drop_position, current_intended
        );
    );

    global_tick_counter += 1;
);

randomize_items_stop() -> (
    global_run_randomize_items = false;
);

randomize_items_display_last_intended(summary) -> (
    print(str('Last Intended After %d Ticks', global_tick_counter));
    count = 0;
    if (length(global_last_intended_items) == 0, print('No data yet'); return());

    durations = [range(global_last_intended_size)];

    c_for(i = 0, i < global_last_intended_size, i += 1,
        entry_idx = (global_last_intended_next_idx + i) % global_last_intended_size;
        entry_time = global_last_intended_times:entry_idx;
        if(entry_time != -1,
            next_idx = (global_last_intended_next_idx + i + 1) % global_last_intended_size;
            next_time = if(and(i < global_last_intended_size - 1, global_last_intended_times:next_idx != -1), global_last_intended_times:next_idx, global_tick_counter);
            put(durations, i, next_time - entry_time);
        );
    );

    c_for(i = 0, i < global_last_intended_size, i += 1,
        entry_idx = (global_last_intended_next_idx + i) % global_last_intended_size;
        if(global_last_intended_times:entry_idx != -1,
            entry_time = global_last_intended_times:entry_idx;
            entry_item = global_last_intended_items:entry_idx;
            if(summary,
                print(str('%d: Item: %s Count: %d Duration: %d', count, entry_item:0, entry_item:1, durations:i));
            ,
                print(str('%d: Item: %s Duration: %d Time: %d', count, entry_item, durations:i, entry_time));
            );
            count += 1;
        );
    );
);

randomly_drop_in_block(drop_position, item, spread) -> (
    item_data = parse_nbt(item:2);
    item_data:'count' = max(min(floor(rand(2*spread+1)) - spread + item:1, stack_limit(item:0)),1);

    spawn('item',
        rand(0.75) + 0.125 + drop_position:0,
        rand(0.75) + drop_position:1,
        rand(0.75) + 0.125 + drop_position:2,
        encode_nbt({'Item' -> item_data}, true)
    );
);

get_item_list(inventory_pos) -> (
    list = [];
    size = inventory_size(block(inventory_pos));
    c_for(s = 0, s < size, s+=1,
        item = inventory_get(block(inventory_pos), s);
        if(item != null,
            put(list, null, item);
        );
    );
    return(list);
);

randomly_pick_from_inventory(inventory_pos, exclude) -> (
    inventory_item_list = get_item_list(inventory_pos);
    if(exclude != null,
        exclusion_string = str(exclude:2);
        inventory_item_list = filter(inventory_item_list, str(_:2) != exclusion_string);
    );
    return(inventory_item_list:floor(rand(length(inventory_item_list))));
);

add_to_last_intended(item) -> (
    global_last_intended_items:global_last_intended_next_idx = item;
    global_last_intended_times:global_last_intended_next_idx = global_tick_counter;
    global_last_intended_next_idx = (global_last_intended_next_idx + 1) % global_last_intended_size;
);
