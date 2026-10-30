__config() -> {
    'commands' -> {
        'add_group <start_pos> <end_pos>' -> 'add_group',
        'remove_group <index_int>' -> 'remove_group',
        'list_groups' -> 'list_groups',
        'reset' -> 'reset',
        'replace_all' -> 'replace_all',
        'replace_one <start_pos> <end_pos>' -> 'replace_one',
    },
};

global_block_groups = [];

add_group(start_pos, end_pos) -> (
    pair = [start_pos, end_pos];
    put(global_block_groups, null, pair);
    pair = get(global_block_groups, -1);
    print(str('Added %s %s', str(pair:0), str(pair:1)));
);

remove_group(index_int) -> (
    length_limit = length(global_block_groups);
    if(and(index_int >= 0, index_int < length_limit),
        delete(global_block_groups, index_int),
        print(str('Index: %d is not valid - expected between: [0, %d)', index_int, length_limit));
    );
);

list_groups() -> (
    print('Replacement Groups:');
    map(global_block_groups, print(str('Group: %d Corner 1: %s Corner 2: %s', _i, str(_:0), str(_:1))));
);

reset() -> (
    global_block_groups = [];
    print('Group List Reset');
);

replace_all() -> (
    map(global_block_groups,
        replace_one(_:0, _:1);
    );
);

replace_one(start, end) -> (
    print(str('Replacing Group: %d Corner 1: %s Corner 2: %s', index, str(start), str(end)));
    without_updates(
        volume(start, end,
            c_block_state = block_state(_);
            c_block_data = block_data(_);
            set(_x, _y, _z, 'air');
            set(_x, _y, _z, _, c_block_state, nbt(c_block_data));
        );
    );
);

