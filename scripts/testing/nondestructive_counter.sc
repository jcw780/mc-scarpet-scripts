__config() -> {
    'commands' -> {
        'start <search_center> <search_range> <age> <wait_for_item>' -> 'start',
        'stop' -> 'stop',
        'reset' -> 'reset_counts',
        'status' -> 'status',
    },
    'arguments' -> {
        'search_center' -> {
            'type' -> 'location'
        },
        'search_range' -> {
            'type' -> 'location',
            'min' -> 0,
        },
        'age' -> {
            'type' -> 'int',
            'min' -> 0,
        },
        'wait_for_item' -> {
            'type' -> 'bool',
        }
    },
    'scope' -> 'player',
};

global_item_counter = {};
global_start_time = -1;
  = -1;
global_search_center = null;
global_search_range = null;

global_should_run = false;

add_or_create(dict, key, value) -> (
    if(has(dict, key), dict:key += value, dict:key = value);
);

start(s_center, s_range, age, wait) -> (
    print(s_center);
    if(!global_should_run,
        global_should_run = true;
        if(!wait, global_start_time = system_info('world_time'));
        global_search_center = s_center;
        global_search_range = s_range;
        show_search_area();
        continue_running(age);
    );
);

continue_running(age) -> (
    if(global_should_run,
        items = filter(entity_area('item', global_search_center, global_search_range),_~'age'==age);
        global_most_recent_sample_time = system_info('world_time');
        if(global_start_time == -1 && length(items) > 0, global_start_time = system_info('world_time'));
        for(items,
            item_type = _~'item':0;
            count = _~'count';
            add_or_create(global_item_counter, item_type, count);
        );
        if(global_should_run, schedule(1, 'continue_running', age));
    );
);

status() -> (
    if(global_most_recent_sample_time != -1,
        show_search_area();
        duration = (global_most_recent_sample_time - global_start_time + 1) / 72000;
        print(str('start: %d current: %d', global_start_time, global_most_recent_sample_time));
        total = 0;
        if(length(keys(global_item_counter)) > 0,
            for(sort(keys(global_item_counter)),
                count = global_item_counter:_;
                total += count;
                print(str('%s: %d total, %d/hr', _, count, count/duration))
            );
            print('------------');
            print(str('Total: %d, %d/hr', total, total/duration));
        ,
            print('No items have been counted yet');
        );
    ,
        print('Counter not set');
    );
);

stop() -> (
    if(global_should_run,
        global_should_run = false;
        print('Final Count');
        status();
        reset_counts();
        global_search_center = null;
        global_search_range = null;
    ,
        print('No items counted');
    );
);

reset_counts() -> (
    global_item_counter = {};
    global_start_time = -1;
    global_most_recent_sample_time = -1;
);

show_search_area() -> (
    from = global_search_center - global_search_range;
    to = global_search_center + global_search_range;
    draw_shape('box', 200, {
        'from' -> from,
        'to' -> to
    });
);

