-- Seed data for local development and automated tests.
-- Runs on every start with the default H2 profile (spring.sql.init.mode=always).
-- Works on H2 and MySQL; ids are generated, history rows look recipes up by title.

INSERT INTO recipes (title, description, cuisine, category, ingredients, instructions, prep_minutes, cook_minutes, servings, status, created_by, created_at, updated_at, version) VALUES
('Margherita Pizza', 'Classic Neapolitan pizza with tomato, mozzarella and basil.', 'Italian', 'Main Course',
 '1 pizza dough ball
200g San Marzano tomatoes
125g fresh mozzarella
Fresh basil
Olive oil, salt',
 '1. Preheat oven to 250C with a pizza stone.
2. Stretch the dough and spread crushed tomatoes.
3. Add torn mozzarella and bake 8-10 minutes.
4. Finish with basil and olive oil.', 20, 10, 2, 'PUBLISHED', 'author', TIMESTAMP '2026-10-01 10:00:00', TIMESTAMP '2026-10-03 09:00:00', 0),

('Spaghetti Carbonara', 'Roman pasta with eggs, pecorino and guanciale.', 'Italian', 'Main Course',
 '200g spaghetti
100g guanciale
2 eggs + 1 yolk
50g pecorino romano
Black pepper',
 '1. Cook the spaghetti in salted water.
2. Crisp the guanciale in a pan.
3. Whisk eggs with pecorino and pepper.
4. Toss pasta off the heat with guanciale and egg mixture.', 10, 15, 2, 'PUBLISHED', 'author', TIMESTAMP '2026-10-02 11:00:00', TIMESTAMP '2026-10-04 12:00:00', 0),

('Tiramisu', 'Coffee-soaked ladyfingers layered with mascarpone cream.', 'Italian', 'Dessert',
 '250g mascarpone
3 eggs
75g sugar
200ml espresso
Ladyfingers
Cocoa powder',
 '1. Beat yolks with sugar, fold in mascarpone.
2. Whip whites and fold in.
3. Dip ladyfingers in espresso and layer with cream.
4. Chill 6 hours and dust with cocoa.', 30, 0, 6, 'PENDING_REVIEW', 'author', TIMESTAMP '2026-10-05 14:00:00', TIMESTAMP '2026-10-06 08:30:00', 0),

('Butter Chicken', 'Tandoori chicken in a creamy tomato and butter sauce.', 'Indian', 'Main Course',
 '500g chicken thighs
200g yogurt
2 tbsp tandoori masala
400g tomato puree
50g butter
100ml cream',
 '1. Marinate chicken in yogurt and masala for 2 hours.
2. Grill or pan-sear the chicken.
3. Simmer tomato puree with butter and spices.
4. Add cream and chicken and simmer 10 minutes.', 20, 40, 4, 'PUBLISHED', 'admin', TIMESTAMP '2026-10-03 09:00:00', TIMESTAMP '2026-10-05 10:00:00', 0),

('Chana Masala', 'Spiced chickpea curry.', 'Indian', 'Main Course',
 '2 cans chickpeas
1 onion
2 tomatoes
Ginger-garlic paste
Chana masala spice',
 '1. Saute onion until golden.
2. Add ginger-garlic paste and spices.
3. Add tomatoes and cook down.
4. Add chickpeas and simmer 20 minutes.', 10, 30, 4, 'DRAFT', 'author', TIMESTAMP '2026-10-07 16:00:00', TIMESTAMP '2026-10-07 16:00:00', 0),

('Masala Chai', 'Spiced Indian milk tea.', 'Indian', 'Beverage',
 '2 cups water
1 cup milk
2 tsp black tea
Cardamom, ginger, cinnamon
Sugar to taste',
 '1. Boil water with crushed spices.
2. Add tea and simmer 2 minutes.
3. Add milk and sugar and bring to a boil.
4. Strain and serve.', 5, 10, 2, 'PUBLISHED', 'author', TIMESTAMP '2026-10-04 07:00:00', TIMESTAMP '2026-10-06 07:30:00', 0),

('Pad Thai', 'Stir-fried rice noodles with tamarind sauce.', 'Thai', 'Main Course',
 '200g rice noodles
150g shrimp or tofu
2 eggs
3 tbsp tamarind paste
2 tbsp fish sauce
Bean sprouts, peanuts, lime',
 '1. Soak noodles in warm water.
2. Stir-fry protein, push aside and scramble eggs.
3. Add noodles and sauce and toss.
4. Serve with sprouts, peanuts and lime.', 20, 10, 2, 'PENDING_REVIEW', 'author', TIMESTAMP '2026-10-08 12:00:00', TIMESTAMP '2026-10-09 09:00:00', 0),

('Green Curry', 'Fragrant Thai curry with coconut milk.', 'Thai', 'Main Course',
 '2 tbsp green curry paste
400ml coconut milk
300g chicken
Thai basil
Bamboo shoots',
 '1. Fry curry paste in thick coconut cream.
2. Add chicken and cook through.
3. Add remaining coconut milk and vegetables.
4. Finish with basil.', 15, 20, 4, 'DRAFT', 'admin', TIMESTAMP '2026-10-09 18:00:00', TIMESTAMP '2026-10-09 18:00:00', 0),

('Guacamole', 'Fresh avocado dip.', 'Mexican', 'Appetizer',
 '3 ripe avocados
1 lime
1/2 red onion
1 jalapeno
Cilantro, salt',
 '1. Mash avocados.
2. Mix in lime juice, chopped onion, jalapeno and cilantro.
3. Season with salt.', 10, 0, 4, 'PUBLISHED', 'author', TIMESTAMP '2026-10-02 15:00:00', TIMESTAMP '2026-10-03 15:00:00', 0),

('Chicken Tacos', 'Street-style tacos with charred chicken.', 'Mexican', 'Main Course',
 '500g chicken thighs
Taco seasoning
Corn tortillas
Onion, cilantro, lime
Salsa',
 '1. Season and grill chicken.
2. Chop and warm tortillas.
3. Assemble with onion, cilantro and salsa.', 15, 15, 4, 'DRAFT', 'author', TIMESTAMP '2026-10-10 13:00:00', TIMESTAMP '2026-10-10 13:00:00', 0),

('French Onion Soup', 'Caramelised onion soup with a cheese crouton.', 'French', 'Soup',
 '1kg onions
50g butter
1.5L beef stock
Baguette slices
150g gruyere',
 '1. Slowly caramelise onions in butter for 45 minutes.
2. Add stock and simmer 20 minutes.
3. Top bowls with bread and cheese and grill until bubbling.', 15, 70, 4, 'PUBLISHED', 'admin', TIMESTAMP '2026-10-01 17:00:00', TIMESTAMP '2026-10-02 17:00:00', 0),

('Creme Brulee', 'Vanilla custard with a caramelised sugar crust.', 'French', 'Dessert',
 '500ml cream
5 egg yolks
100g sugar
1 vanilla pod',
 '1. Infuse cream with vanilla.
2. Whisk yolks and sugar, add cream.
3. Bake in a water bath at 150C for 35 minutes.
4. Chill, sprinkle sugar and torch.', 20, 35, 4, 'DRAFT', 'author', TIMESTAMP '2026-10-11 10:00:00', TIMESTAMP '2026-10-11 10:00:00', 0);

-- Workflow audit trail for the seeded non-draft recipes
INSERT INTO recipe_status_history (recipe_id, from_status, to_status, action, changed_by, comment, changed_at)
SELECT id, 'DRAFT', 'PENDING_REVIEW', 'SUBMIT', created_by, 'Ready for review', created_at
FROM recipes WHERE status IN ('PENDING_REVIEW', 'PUBLISHED');

INSERT INTO recipe_status_history (recipe_id, from_status, to_status, action, changed_by, comment, changed_at)
SELECT id, 'PENDING_REVIEW', 'PUBLISHED', 'APPROVE', 'reviewer', 'Approved', updated_at
FROM recipes WHERE status = 'PUBLISHED';
