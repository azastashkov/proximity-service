CREATE TABLE IF NOT EXISTS business (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(50),
    zip_code        VARCHAR(20),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    category        VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    website         VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_business_category ON business(category);

CREATE TABLE IF NOT EXISTS geospatial_index (
    geohash       VARCHAR(12) NOT NULL,
    business_id   BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    PRIMARY KEY (geohash, business_id)
);

CREATE INDEX idx_geospatial_geohash ON geospatial_index(geohash);

-- ============================================================
-- Seed data: ~20 businesses across NYC, San Francisco, London
-- Geohashes computed via GeoHashUtil.encode() at precisions 4, 5, 6
-- ============================================================

INSERT INTO business (id, name, description, address, city, state, country, zip_code, latitude, longitude, category, phone, website) VALUES
-- NYC businesses
(1,  'Joe''s Pizza',
     'Classic New York slice joint serving thin-crust pizza since 1975.',
     '7 Carmine St', 'New York', 'NY', 'US', '10014',
     40.730610, -73.935242, 'restaurant', '+12125551001', 'https://joespizzanyc.com'),

(2,  'Central Park Cafe',
     'Charming cafe with outdoor seating overlooking Central Park.',
     '250 W 77th St', 'New York', 'NY', 'US', '10024',
     40.785091, -73.968285, 'cafe', '+12125551002', 'https://centralparkafe.com'),

(3,  'Brooklyn Bagel Co',
     'Hand-rolled, kettle-boiled bagels baked fresh every morning.',
     '754 Nostrand Ave', 'New York', 'NY', 'US', '11216',
     40.678178, -73.944158, 'bakery', '+17185551003', 'https://brooklynbagelco.com'),

(4,  'Manhattan Gym',
     'Full-service fitness center with personal training and group classes.',
     '333 8th Ave', 'New York', 'NY', 'US', '10001',
     40.748817, -73.985428, 'gym', '+12125551004', 'https://manhattangym.com'),

(5,  'Queens Night Market',
     'Outdoor food market featuring 100+ vendors from around the world.',
     '47-01 111th St', 'New York', 'NY', 'US', '11368',
     40.745776, -73.894775, 'market', '+17185551005', 'https://queensnightmarket.com'),

(6,  'Harlem Soul Kitchen',
     'Southern comfort food with live jazz on weekends.',
     '308 Lenox Ave', 'New York', 'NY', 'US', '10027',
     40.811230, -73.949921, 'restaurant', '+12125551006', 'https://harlemsoulkitchen.com'),

(7,  'Financial District Brokerage',
     'Full-service financial advisory and brokerage firm.',
     '55 Water St', 'New York', 'NY', 'US', '10041',
     40.707493, -74.011276, 'finance', '+12125551007', 'https://fidibrokerage.com'),

-- San Francisco businesses
(8,  'Golden Gate Bakery',
     'Legendary egg custard tarts and pastries in Union Square.',
     '1029 Grant Ave', 'San Francisco', 'CA', 'US', '94133',
     37.786971, -122.407437, 'bakery', '+14155551008', 'https://goldengatebakery.com'),

(9,  'Mission Burrito House',
     'Authentic Mission-style super burritos, made to order.',
     '2390 Mission St', 'San Francisco', 'CA', 'US', '94110',
     37.762234, -122.419464, 'restaurant', '+14155551009', 'https://missionburritohouse.com'),

(10, 'SoMa Coworking Space',
     'Flexible hot desks, private offices, and meeting rooms in SoMa.',
     '45 Fremont St', 'San Francisco', 'CA', 'US', '94105',
     37.778500, -122.407700, 'coworking', '+14155551010', 'https://somacowork.com'),

(11, 'Fisherman''s Wharf Seafood',
     'Fresh Dungeness crab and clam chowder in sourdough bowls.',
     '2800 Jones St', 'San Francisco', 'CA', 'US', '94133',
     37.808300, -122.417500, 'restaurant', '+14155551011', 'https://wharfseafood.com'),

(12, 'Castro Coffee',
     'Specialty single-origin espresso drinks in the heart of Castro.',
     '4026 18th St', 'San Francisco', 'CA', 'US', '94114',
     37.762710, -122.435100, 'cafe', '+14155551012', 'https://castrocoffee.com'),

(13, 'Haight Ashbury Books',
     'Independent bookstore specialising in counterculture and rare finds.',
     '1855 Haight St', 'San Francisco', 'CA', 'US', '94117',
     37.769500, -122.447400, 'bookstore', '+14155551013', 'https://haightbooks.com'),

(14, 'Richmond Dim Sum',
     'Traditional Cantonese dim sum brunch served daily until 3 pm.',
     '4149 Geary Blvd', 'San Francisco', 'CA', 'US', '94118',
     37.780200, -122.473700, 'restaurant', '+14155551014', 'https://richmonddimsum.com'),

-- London businesses
(15, 'Covent Garden Tea Shop',
     'Traditional British afternoon tea in a Victorian setting.',
     '10 Neal St', 'London', NULL, 'GB', 'WC2H 9PU',
     51.512300, -0.122800, 'cafe', '+442071551015', 'https://coventgardentea.co.uk'),

(16, 'Shoreditch Artisan Bakery',
     'Sourdough loaves and pastries baked in a wood-fired oven.',
     '23 Curtain Rd', 'London', NULL, 'GB', 'EC2A 3LT',
     51.523300, -0.076200, 'bakery', '+442071551016', 'https://shoreditchbakery.co.uk'),

(17, 'Camden Market Diner',
     'Eclectic street food diner inside the iconic Camden Lock Market.',
     '54 Camden Lock Pl', 'London', NULL, 'GB', 'NW1 8AF',
     51.541400, -0.143400, 'restaurant', '+442071551017', 'https://camdenmarketdiner.co.uk'),

(18, 'Canary Wharf Sushi',
     'Omakase and a la carte Japanese dining overlooking the docks.',
     '25 Canada Square', 'London', NULL, 'GB', 'E14 5LB',
     51.505200, -0.019400, 'restaurant', '+442071551018', 'https://canarycosushi.co.uk'),

(19, 'Notting Hill Bistro',
     'French-inspired neighbourhood bistro with a seasonal menu.',
     '112 Portobello Rd', 'London', NULL, 'GB', 'W11 2DZ',
     51.511200, -0.199800, 'restaurant', '+442071551019', 'https://nottinghillbistro.co.uk'),

(20, 'Greenwich Pub',
     'Historic riverside pub serving cask ales and Sunday roasts.',
     '2 College Approach', 'London', NULL, 'GB', 'SE10 9HY',
     51.482600, 0.007700, 'pub', '+442071551020', 'https://greenwichpub.co.uk');

-- ============================================================
-- geospatial_index entries (precisions 4, 5, 6 per business)
-- Geohashes verified with GeoHashUtil.encode() Python port
-- ============================================================

INSERT INTO geospatial_index (geohash, business_id) VALUES
-- 1. Joe's Pizza (40.730610, -73.935242) dr5r / dr5rt / dr5rtw
('dr5r',   1), ('dr5rt',  1), ('dr5rtw', 1),
-- 2. Central Park Cafe (40.785091, -73.968285) dr72 / dr72h / dr72hb
('dr72',   2), ('dr72h',  2), ('dr72hb', 2),
-- 3. Brooklyn Bagel Co (40.678178, -73.944158) dr5r / dr5rm / dr5rmm
('dr5r',   3), ('dr5rm',  3), ('dr5rmm', 3),
-- 4. Manhattan Gym (40.748817, -73.985428) dr5r / dr5ru / dr5ru6
('dr5r',   4), ('dr5ru',  4), ('dr5ru6', 4),
-- 5. Queens Night Market (40.745776, -73.894775) dr5r / dr5ry / dr5ry3
('dr5r',   5), ('dr5ry',  5), ('dr5ry3', 5),
-- 6. Harlem Soul Kitchen (40.811230, -73.949921) dr72 / dr72j / dr72jj
('dr72',   6), ('dr72j',  6), ('dr72jj', 6),
-- 7. Financial District Brokerage (40.707493, -74.011276) dr5r / dr5re / dr5ref
('dr5r',   7), ('dr5re',  7), ('dr5ref', 7),
-- 8. Golden Gate Bakery (37.786971, -122.407437) 9q8y / 9q8yy / 9q8yyw
('9q8y',   8), ('9q8yy',  8), ('9q8yyw', 8),
-- 9. Mission Burrito House (37.762234, -122.419464) 9q8y / 9q8yy / 9q8yy6
('9q8y',   9), ('9q8yy',  9), ('9q8yy6', 9),
-- 10. SoMa Coworking Space (37.778500, -122.407700) 9q8y / 9q8yy / 9q8yyt
('9q8y',  10), ('9q8yy', 10), ('9q8yyt',10),
-- 11. Fisherman's Wharf Seafood (37.808300, -122.417500) 9q8z / 9q8zn / 9q8zn6
('9q8z',  11), ('9q8zn', 11), ('9q8zn6',11),
-- 12. Castro Coffee (37.762710, -122.435100) 9q8y / 9q8yv / 9q8yvf
('9q8y',  12), ('9q8yv', 12), ('9q8yvf',12),
-- 13. Haight Ashbury Books (37.769500, -122.447400) 9q8y / 9q8yv / 9q8yve
('9q8y',  13), ('9q8yv', 13), ('9q8yve',13),
-- 14. Richmond Dim Sum (37.780200, -122.473700) 9q8y / 9q8yv / 9q8yvj
('9q8y',  14), ('9q8yv', 14), ('9q8yvj',14),
-- 15. Covent Garden Tea Shop (51.512300, -0.122800) gcpv / gcpvj / gcpvj1
('gcpv',  15), ('gcpvj', 15), ('gcpvj1',15),
-- 16. Shoreditch Artisan Bakery (51.523300, -0.076200) gcpv / gcpvn / gcpvn7
('gcpv',  16), ('gcpvn', 16), ('gcpvn7',16),
-- 17. Camden Market Diner (51.541400, -0.143400) gcpv / gcpvh / gcpvhw
('gcpv',  17), ('gcpvh', 17), ('gcpvhw',17),
-- 18. Canary Wharf Sushi (51.505200, -0.019400) gcpv / gcpvp / gcpvp8
('gcpv',  18), ('gcpvp', 18), ('gcpvp8',18),
-- 19. Notting Hill Bistro (51.511200, -0.199800) gcpv / gcpv5 / gcpv53
('gcpv',  19), ('gcpv5', 19), ('gcpv53',19),
-- 20. Greenwich Pub (51.482600, 0.007700) u10h / u10hb / u10hbh
('u10h',  20), ('u10hb', 20), ('u10hbh',20);

-- Reset sequence after explicit id inserts
SELECT setval('business_id_seq', 20);
