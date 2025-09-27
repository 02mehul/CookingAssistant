# CookingAssistant (Android)

Smart cooking assistant with pantry management, recipe search (TheMealDB), and an Open Kitchen Mode using multiple sensors (light, proximity, accelerometer).

--------------------------------------------------------------------------------------------------------------------------------------------------------------
## Features

Modern UI with Bottom Navigation (Home • Pantry • Recipes • Kitchen Tools).
Local persistence with Room (SQLite): pantry items + shopping list.
Recipe search via TheMealDB (Retrofit + Gson) with images (Glide).

“Open Kitchen” sensors:
Accelerometer: shake-to-start/pause a quick timer.
Proximity: hands-free next step while cooking + silences ringing alarms.
Camera/Flashlight: blinking alert when timers finish (requires CAMERA permission).

MoodMeal: choose mood + (optional) sensor signals → get a matching recipe.
CSV import/export of pantry for fast grading/demo.
Auto-generate Shopping List: compute (recipe ingredients − pantry) and store to shopping_items.

Deep-linking: Snackbars jump straight to Pantry tab after actions.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

🧭 Project structure

app/
 ├─ manifests/
 │   └─ AndroidManifest.xml
 ├─ java/com/example/cookingassistant/
 │   ├─ adapters/
 │   │   └─ ItemAdapter.java           # RecyclerView adapter for pantry
 │   ├─ api/
 │   │   ├─ ApiClient.java             # Retrofit singleton
 │   │   ├─ MealApi.java               # TheMealDB endpoints
 │   │   └─ model/                     # POJOs from TheMealDB
 │   ├─ db/
 │   │   ├─ AppDatabase.java           # Room database (items + shopping_items)
 │   │   ├─ Item.java                  # @Entity table "items"
 │   │   ├─ ItemDao.java               # CRUD + queries for items
 │   │   ├─ ShoppingItem.java          # @Entity table "shopping_items"
 │   │   └─ ShoppingItemDao.java       # CRUD + queries for shopping list
 │   ├─ ui/
 │   │   ├─ MainActivity.java          # BottomNav host
 │   │   ├─ HomeFragment.java          # Dashboard + gauge + quick actions
 │   │   ├─ PantryFragment.java        # Pantry list, add/edit/delete, CSV I/O
 │   │   ├─ ItemEditActivity.java      # Editor for a single item
 │   │   ├─ RecipesFragment.java       # Search/browse recipes
 │   │   ├─ RecipeDetailActivity.java  # Image + ingredients + steps + TTS +
 │   │   │                             #  add to pantry / shopping list + YouTube
 │   │   ├─ SensorsLauncherFragment.java  # Entry to kitchen tools
 │   │   ├─ SensorsActivity.java       # Timers + accelerometer + proximity +
 │   │   │                             #  flashlight, camera permission handling
 │   │   ├─ ShoppingListActivity.java  # Check/clear shopping list
 │   │   └─ mood/
 │   │       ├─ MoodCaptureActivity.java     # mood selection (+ optional sensors/camera)
 │   │       ├─ IngredientInputActivity.java # voice/text ingredients
 │   │       ├─ MoodMatchActivity.java       # suggested recipe based on mood+pantry
 │   │       └─ MoodMeterView.java           # animated custom view
 │   └─ models/ (optional UI models if any)
 └─ res/
     ├─ layout/ (activities, fragments, rows)
     ├─ menu/   (bottom_nav_menu.xml, pantry_menu.xml, row menus, shopping_menu.xml)
     ├─ drawable/ (gradients, backgrounds)
     └─ values/ (colors.xml, strings.xml, themes/)


--------------------------------------------------------------------------------------------------------------------------------------------------------------     

🌐 Networking

Retrofit (ApiClient, MealApi) calls TheMealDB (public) for:
Search by term/category
Random / lookup by id
Glide loads recipe thumbnails.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

🧪 Sensors & “Open Kitchen”

Activity: SensorsActivity
Accelerometer (shake): toggles a Quick Timer.

Shake again = pause/resume. UI shows shake count and a big countdown.

Named timers: create multiple concurrent timers (“Rice 10:00”), each card has a cancel/clear button.
Ringtone + Flashlight blink when a timer finishes.
Flash uses Camera2 torch; app requests CAMERA permission on first use.

Proximity: waving a hand while an alarm rings silences it.
Lifecycle manages registration/unregistration of listeners and cancels timers.

Activity: RecipeDetailActivity

Proximity: hands-free Next Step while reading directions.

Text-To-Speech: optional “Read aloud” for steps.

MoodMeal flow (ui/mood)
Mood selection + optional sensor input (energy via accelerometer).
Ingredient input (voice or text).
Suggests a recipe (Mood + available ingredients). “Try Another” randomizes within the mood category.
On emulators, proximity & flashlight may be unavailable; demo on a real device.
--------------------------------------------------------------------------------------------------------------------------------------------------------------

🍳 Pantry & Shopping List

PantryFragment
Toolbar actions: Add, Clear All (with Undo snackbar), Export CSV, Import CSV, Open Shopping List.
FAB & empty state “Add item” button.

Row overflow menu: Edit, Delete.
Uses Room on a background thread (singleThread Executors), UI updates via runOnUiThread.

CSV import/export
Implemented with Storage Access Framework:
Export → choose location (CreateDocument) as pantry.csv.
Import → pick a CSV (OpenDocument). Import merges by name (case-insensitive).

Auto-generate Shopping List
From Recipe Detail:
Add missing to Shopping List → compute (recipe ingredients − pantry items), merge into shopping_items.
Add all ingredients to Pantry → bulk add/increment everything; snackbar action Open Pantry deep-links to Pantry tab.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

🧑‍🍳 Demo script (for grading)

Home → Gauge shows pantry stock. Tap Pantry quick action.

Pantry → Add an item via + (or toolbar Add). Edit and delete from row menu.
Export to CSV; Import from CSV; show Undo after Clear All.

Recipes → Search “chicken”; open a result.

Recipe Detail → show image/ingredients. Toggle Read aloud and Hands-free; wave hand to go next step.
Tap Add missing to Shopping List; then Open → show Shopping List and check items.
Back, Add all ingredients to Pantry; tap Open Pantry (deep-link to Pantry tab).

Kitchen Tools → set a Quick Timer (1:00), shake to start; at finish hear ringtone + flash blink; wave to silence.
Add a Named Timer (“Rice 10:00”) and cancel it.

MoodMeal → choose a mood, enter ingredients, and show a matching recipe; try “Try another”.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

🛠 Troubleshooting

Manifest merger / exported: launcher activity must set android:exported="true".
compileSdk mismatch: set compileSdk 35 to satisfy latest androidx libs.
Room “cannot verify data integrity”: bump DB version and/or add proper Migration.
Proximity/flash not working on emulator: use a real device.
Deep-link returns to Home: ensure MainActivity.handleDeepLink() sets bottomNav.setSelectedItemId(R.id.nav_pantry) and launchMode="singleTop" is set.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

Tech

- Android Studio Ladybug (2024.2.2), Java 17, SDK 35  
- Room, Retrofit + Gson, Glide, Material Components  
- MVVM-ish, ViewBinding

--------------------------------------------------------------------------------------------------------------------------------------------------------------

Build

- Open in Android Studio → Sync Gradle → Run on device (sensors work best on a real device).
- Use Pixel 6 in the emulator (best choice for this project)

--------------------------------------------------------------------------------------------------------------------------------------------------------------

Repo hygiene

- Secrets not committed (`local.properties`, keystores ignored).
- Use branches + PRs. Protected `main`.

--------------------------------------------------------------------------------------------------------------------------------------------------------------

Team

- @Mehul Modha (owner) Matrikel - 319519, 
- @Yash Ghildiyal (owner) Matrikel - 319567, 
- @Swarnim Tiwari (owner) Matrikel - 319302, 
