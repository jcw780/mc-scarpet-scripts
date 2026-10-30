//Entity Search Visualizer by stacked_dynamite

__config() -> {
    'commands' -> {
        'entity <target_entity> <expansion> <duration>' -> 'entity_search',
        'corner <first_corner> <second_corner> <duration>' -> 'corner_search',
        'block <block> <first_corner> <second_corner> <offset> <duration>' -> 'block_search',
        'block_pixel <block> <first_corner> <second_corner> <offset> <duration>' -> 'block_px_search',
    },
    'arguments' -> {
        'first_corner' -> {'type' -> 'location', 'block_centered' -> false,},
        'second_corner' -> {'type' -> 'location', 'block_centered' -> false,},
        'target_entity' -> {'type' -> 'entities', 'single' -> 'true',},
        'expansion' -> {'type' -> 'location', 'block_centered' -> false,},
        'duration' -> {'type' -> 'int', 'min' -> 0,},
        'block' -> {'type' -> 'pos',},
        'offset' -> {'type' -> 'location', 'block_centered' -> false,},
    },
    'scope' -> 'player',
};

entity_search(entity, expansion, duration) -> (
    entity_pos = entity~'pos';
    entity_width = entity~'width';
    entity_height = entity~'height';

    print(str('Entity Pos: %s', [entity_pos]));
    print(str('Width: %f Height: %f', entity_width, entity_height));

    half_width = entity_width/2;
    min_entity_corner = [entity_pos:0-half_width, entity_pos:1, entity_pos:2-half_width];
    max_entity_corner = [entity_pos:0+half_width, entity_pos:1+entity_height, entity_pos:2+half_width];

    //print(str('%s %s', min_entity_corner, max_entity_corner));

    expanded_min_corner = map(min_entity_corner,_-expansion:_i);
    expanded_max_corner = map(max_entity_corner,_+expansion:_i);

    search(expanded_min_corner, expanded_max_corner, duration);
);

corner_search(c1, c2, duration) -> (search(map(c1, min(_, c2:_i)), map(c1, max(_, c2:_i)), duration));

block_px_search(center_block, c1, c2, offset, duration) -> (
    block_search(center_block, c1/16, c2/16, offset, duration);
);

block_search(center_block, c1, c2, offset, duration) -> (
    min_box = map(c1, min(_, c2:_i));
    max_box = map(c1, max(_, c2:_i));

    translation = center_block + [0.5, 0, 0.5] + offset -
        [(max_box:0 - min_box:0) / 2, 0, (max_box:2 - min_box:2) / 2];

    search(min_box + translation, max_box + translation, duration);
);

search(min_pos, max_pos, duration) -> (
    ss_min_pos = [min_pos:0-2, min_pos:1-4, min_pos:2-2];
    ss_max_pos = [max_pos:0+2, max_pos:1, max_pos:2+2];

    s_min_pos = map(ss_min_pos, entity_to_chunk_pos(_));
    s_max_pos = map(ss_max_pos, entity_to_chunk_pos(_));

    print(str('Search Box: %s %s', min_pos, max_pos));
    draw_shape('box', duration, {
        'from' -> min_pos,
        'to' -> max_pos,
        'color' -> 0xFF0000FF,
    });
    print(str('Expanded Box: %s %s', ss_min_pos, ss_max_pos));
    draw_shape('box', duration, {
        'from' -> ss_min_pos,
        'to' -> ss_max_pos,
        'color' -> 0x00FFFFFF,
    });
    print(str('Subchunk Range: %s %s', s_min_pos, s_max_pos));
    iterate_subchunks(s_min_pos, s_max_pos,
        _(x, y, z, order, outer(duration)) -> (
            print(str('%d: %d %d %d', order, x, y, z));
            draw_shape('label', duration, {
                'pos' -> [x*16+8,y*16+8,z*16+8],
                'text' -> str('%d %s', order, [x, y, z]),
                'color' -> 0xFFFFFFFF,
                'size' -> 20,
            });
            draw_shape('box', duration, {
                'from' -> [x*16,y*16,z*16],
                'to' -> [(x+1)*16,(y+1)*16,(z+1)*16],
                'color' -> 0x0000FFFF,
            });
        )
    );
);

//From Lithium
//https://github.com/CaffeineMC/lithium/blob/18ba411cc125d2faead57cd9ca7846fc6e7214d2/common/src/main/java/net/caffeinemc/mods/lithium/mixin/entity/fast_retrieval/EntitySectionStorageMixin.java#L38

iterate_subchunks(s_min, s_max, f) -> (
    [x_min, y_min, z_min] = s_min;
    [x_max, y_max, z_max] = s_max;

    order = [0];
    c_for(x=x_min, x<=x_max, x+=1,
        iterate_zy_order(z_min, z_max,
            _(z, outer(x), outer(y_min), outer(y_max), outer(order), outer(f)) -> (
                iterate_zy_order(y_min, y_max, _(y, outer(x), outer(z), outer(order), outer(f)) -> (
                        call(f, x, y, z, order:0);
                        order:0 += 1;
                    )
                )
            )
        );
    );
);

iterate_zy_order(c_min, c_max, f) -> (
    c_for(c=max(c_min,0), c<=c_max, c+=1, call(f, c));
    c_for(c=c_min, c<=min(c_max, -1), c+=1, call(f, c));
);

entity_to_chunk_pos(entity_pos) -> (
    number(floor(entity_pos/16));
);