INSERT INTO hobby_categories (id, name) VALUES
    ('0f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'Sports & Movement'),
    ('0f1f49ea-6b5d-4c2e-9ce7-3e621f081002', 'Arts & Creativity'),
    ('0f1f49ea-6b5d-4c2e-9ce7-3e621f081003', 'Learning & Intellectual'),
    ('0f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'Games & Strategy');

INSERT INTO hobbies (id, category_id, name, icon, default_equipment_category) VALUES
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081001', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'Running', 'figure.run', 'Shoes'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081002', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'Cycling', 'bicycle', 'Bike'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081003', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'Strength Training', 'dumbbell', 'Gear'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081004', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081002', 'Photography', 'camera', 'Camera'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081005', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081002', 'Drawing', 'pencil', 'Art Supplies'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081006', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081002', 'Guitar', 'music.note', 'Instrument'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081007', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081003', 'Reading', 'book', 'Book'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081008', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081003', 'Language Learning', 'globe', 'Study Materials'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081009', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'Chess', 'chess.knight', 'Board'),
    ('1f1f49ea-6b5d-4c2e-9ce7-3e621f081010', '0f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'Board Games', 'dice', 'Game Set');

INSERT INTO hobby_attribute_template (id, hobby_id, key, label, type, unit, display_order) VALUES
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081001', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'distance_km', 'Distance', 'number', 'km', 1),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081002', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'avg_pace_min_km', 'Average Pace', 'number', 'min/km', 2),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081003', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081001', 'surface', 'Surface', 'text', NULL, 3),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081004', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'shots_taken', 'Shots Taken', 'number', NULL, 1),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081005', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'best_photo_subject', 'Best Photo Subject', 'text', NULL, 2),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081006', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081004', 'location_style', 'Location or Style', 'text', NULL, 3),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081007', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081007', 'pages_read', 'Pages Read', 'number', 'pages', 1),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081008', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081007', 'book_title', 'Book Title', 'text', NULL, 2),
    ('2f1f49ea-6b5d-4c2e-9ce7-3e621f081009', '1f1f49ea-6b5d-4c2e-9ce7-3e621f081007', 'reading_format', 'Reading Format', 'text', NULL, 3);
