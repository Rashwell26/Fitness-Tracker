# XML Explained - And How This Project Uses It

Everything in this app's appearance is described in XML files. Not one colour, label,
spacing value or screen layout is written in Java. This document explains what XML is,
what the syntax means, and exactly which file does what here.

---

## 1. What XML is, in one paragraph

XML is text that describes **structure** using tags. A tag looks like `<TextView ... />`.
Tags can sit inside other tags, which makes a tree. Each tag can carry **attributes** -
`android:text="Hello"` - which are settings for that tag. XML says *what* something is; it
does not run any logic. Android reads these files and builds the real screen objects from
them.

A tiny example:

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Hello" />
```

This means: "make a text view, as big as its text, and put Hello in it."

---

## 2. Why Android uses XML instead of Java code

| Reason | What it means for this project |
|---|---|
| Separate design from logic | `MainActivity.java` never mentions padding, colours or font sizes |
| No recompiling logic to change a colour | edit `colors.xml`, rebuild, done |
| Android Studio can preview it | the Design pane shows the screen without running the app |
| Resources can be swapped | `values-night/` could add dark colours; `values-fr/` could add French text, with no code change |
| Less code | building this screen by hand in Java would be 200+ lines |

---

## 3. The XML files in this project, and what each one is for

| File | Kind of XML | Job |
|---|---|---|
| `app/src/main/AndroidManifest.xml` | manifest | lists the app's screens, icon, label and theme |
| `res/layout/activity_main.xml` | layout | the main screen: summary card, button, list |
| `res/layout/activity_log_workout.xml` | layout | the form screen: three fields, spinner, save button |
| `res/layout/item_workout.xml` | layout | one row of the list |
| `res/values/strings.xml` | values | every word the user sees, plus the spinner array |
| `res/values/colors.xml` | values | the five colours |
| `res/values/themes.xml` | values | the app theme: colours, Material 3, status bar, edge-to-edge |
| `res/drawable/ic_launcher.xml` | drawable | the launcher icon, drawn as a vector |

That is eight XML files. The `build.gradle` files are not XML - they are Gradle scripts.

---

## 4. The syntax, piece by piece

### 4.1 Tags, nesting, and the tree

```xml
<LinearLayout ...>          <!-- a container -->
    <TextView ... />         <!-- a child -->
    <Button ... />           <!-- another child -->
</LinearLayout>
```

`<TextView />` closes itself with `/>`. Containers that hold children need
`</LinearLayout>` at the end. **Every tag must close**, or the build fails.

The nesting matters: a view can only be positioned inside its parent. In
`activity_main.xml`:

```
LinearLayout (vertical, 16dp padding)     <- root
├── MaterialCardView                      <- summary panel
│   └── LinearLayout (horizontal)
│       ├── LinearLayout (weight 1)       <- left number
│       │   ├── TextView (label)
│       │   └── TextView (value)
│       └── LinearLayout (weight 1)       <- right number
└── MaterialButton
└── TextView ("Recent workouts")
└── FrameLayout (weight 1)
    ├── RecyclerView
    └── TextView (empty message)
```

### 4.2 Attributes and namespaces

An attribute is `name="value"`. The part before the colon is a **namespace**:

| Prefix | Where it comes from | Example in this project |
|---|---|---|
| `android:` | the Android framework itself | `android:text`, `android:padding` |
| `app:` | library attributes, e.g. Material | `app:cardCornerRadius`, `app:boxBackgroundMode` |
| `tools:` | design-time only, removed at build time | `tools:text="Running"` |
| `xmlns:` | declares a prefix | `xmlns:android="http://schemas.android.com/apk/res/android"` |

`tools:text` is a neat one: it shows placeholder text in the Android Studio preview but
never exists in the running app. It is used in `item_workout.xml` so the row looks sensible
while designing.

### 4.3 Size attributes - the ones that cause most confusion

| Attribute | Meaning |
|---|---|
| `match_parent` | as big as the parent allows |
| `wrap_content` | as big as the content needs |
| `0dp` + `layout_weight` | take a share of the leftover space |

Weight is how the two summary numbers share the card:

```xml
<LinearLayout
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1" ...>
```

Two children each with `0dp` and `weight="1"` end up 50/50. The `RecyclerView` uses
`0dp` with `weight="1"` to take all remaining height below the button - that is why the
list fills the screen without hard-coding a height.

`layout_` prefix means "this is an instruction to my **parent**". So `layout_weight` and
`layout_marginBottom` are read by the parent, while `padding` and `textSize` belong to the
view itself. `gravity` aligns content inside the view; `layout_gravity` aligns the view
inside its parent.

### 4.4 Units

| Unit | Used for |
|---|---|
| `dp` | sizes and spacing (16dp padding, 12dp corners) - scales with screen density |
| `sp` | text sizes (13sp, 28sp) - also scales with the user's font size setting |
| plain numbers | only things like `android:maxLines="1"` |

Never use `px`. That is why the app looks the same on a small phone and a tablet.

### 4.5 Resource references - the `@` and `?` signs

| Syntax | Meaning | Example |
|---|---|---|
| `@string/name` | a value from `strings.xml` | `android:text="@string/log_workout"` |
| `@color/name` | a colour from `colors.xml` | `@color/green_600` |
| `@style/name` | a style from `themes.xml` | `@style/Theme.FitnessTracker` |
| `@id/name` | an id declared elsewhere | used in the manifest |
| `@+id/name` | **create** a new id | `android:id="@+id/totalCaloriesText"` |
| `?attr/name` | read the value from the **current theme** | `?attr/colorPrimary`, `?attr/colorError` |
| `?android:attr/name` | read a framework theme value | `?android:attr/textColorSecondary` |

The `@+id/...` form is the one that matters for the code. It adds the id to the generated
`R` class, and then `findViewById(R.id.totalCaloriesText)` can find it. **If a view has no
id, Java cannot reach it.**

`?attr/colorPrimary` is the interesting one: it does not name a fixed colour, it says
"whatever this theme calls the primary colour". That is why the same layout works in light
and dark mode - the theme supplies a different value.

---

## 5. How XML becomes a screen

1. **Build time.** The Android resource compiler (AAPT2) reads every XML file and produces
   a binary version, plus one `R` class containing an integer id for every layout, id,
   string, colour and array.
2. **`setContentView(R.layout.activity_main)`.** Java passes the layout's id.
3. **Inflation.** Android reads the compiled XML and creates real objects: a
   `LinearLayout` object, a `MaterialCardView` object, and so on, with every attribute
   applied.
4. **Lookup.** `findViewById(R.id.totalCaloriesText)` walks that tree and returns the
   `TextView` object, which Java can then change at runtime.

That is why the same file can produce a static design and still be dynamic: XML builds the
objects, Java changes them afterwards.

Everything in the app that changes at runtime is set in Java: the two totals, the list rows,
the error messages, the empty-state visibility.

---

## 6. Inside each file

### 6.1 `AndroidManifest.xml`

```xml
<application
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.FitnessTracker">

    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <activity
        android:name=".LogWorkoutActivity"
        android:exported="false"
        android:label="@string/log_workout"
        android:parentActivityName=".MainActivity" />
</application>
```

What each part does:

- `<intent-filter>` with `MAIN` + `LAUNCHER` marks the entry screen - the one the icon
  opens.
- `android:exported="true"` is required for that launcher activity (Android 12+ refuses to
  install otherwise). The form activity is `false`: only this app can open it.
- `android:label` on an activity sets the app bar title - that is why the form says
  "Log Workout" instead of "Fitness Tracker".
- `android:parentActivityName` tells Android the form belongs to Main. AppCompat uses it to
  draw the up arrow. (The arrow still needs a click handler in Java - the manifest only
  draws it.)

### 6.2 `activity_main.xml` - the summary card

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="20dp">
```

Points to notice:

- The tag name is the **full class name** for library widgets: `com.google.android.material.card.MaterialCardView`.
  Framework widgets use the short form (`LinearLayout`, `TextView`).
- `app:cardCornerRadius` and `app:cardElevation` come from the Material library, hence the
  `app:` prefix. These are the only two attributes that make the panel look like a card.
- Padding 20dp inside the card, 16dp on the screen, so the card floats with an even margin.

The two number blocks each use `android:layout_width="0dp"` + `android:layout_weight="1"`,
and the second one adds `android:gravity="end"` so its text hugs the right edge.

### 6.3 `activity_main.xml` - list area

```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/workoutsRecyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <TextView
        android:id="@+id/emptyText"
        android:layout_gravity="center"
        android:visibility="gone" ... />
</FrameLayout>
```

- `FrameLayout` stacks children on top of each other - the empty message sits centred over
  the (empty) list.
- `android:visibility="gone"` means "not drawn and takes no space". Java flips it to
  `View.VISIBLE` when the list is empty. Starting hidden avoids a flicker on launch.
- `0dp` + `weight="1"` here makes the list fill whatever height is left after the card and
  button, on any screen size.

### 6.4 `activity_log_workout.xml` - a Material text field

```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/typeLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/workout_type_hint"
    app:boxBackgroundMode="outline"
    app:boxCornerRadiusTopStart="12dp" ... >

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/typeInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textCapWords" />
</com.google.android.material.textfield.TextInputLayout>
```

- `TextInputLayout` is a **wrapper**: it draws the outline box, the floating hint and the
  error line. The `TextInputEditText` inside is the actual typing area.
- `app:boxBackgroundMode="outline"` is what produces the bordered box. Without it you get a
  filled underline field.
- The four `boxCornerRadius...` values must be given for each corner - there is no single
  "corners" attribute in this version of the library.
- `android:inputType="textCapWords"` capitalises each word; `"number"` on the other two
  fields opens the numeric keyboard and makes typing letters harder.
- The parent has the id `typeLayout` because **Material shows errors on the wrapper**, not on
  the inner `EditText`.

The `ScrollView` at the top matters too: with the keyboard open the form is taller than the
screen on small phones, and a ScrollView lets the user reach the Save button.

### 6.5 `item_workout.xml` - one row

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_marginBottom="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="1dp">
```

Inside: a horizontal `LinearLayout` with the type text (`0dp` + weight 1, so it can shrink)
and a Delete `Button` styled as a text button:

```xml
<Button
    android:id="@+id/deleteButton"
    style="@style/Widget.Material3.Button.TextButton"
    android:text="@string/delete"
    android:textColor="?attr/colorError" />
```

- A `style=` attribute applies a whole bundle of attributes at once. This one turns a normal
  button into a flat, borderless text button.
- `?attr/colorError` makes it red in both light and dark themes.

This file is **inflated once per visible row** by the adapter. That is why the ids inside it
are looked up in the ViewHolder's constructor, not on every scroll.

### 6.6 `strings.xml`

```xml
<string name="total_calories_value">%1$d kcal</string>
<string name="workout_details">%1$d min - %2$d kcal - %3$s</string>

<string-array name="intensity_levels">
    <item>Low</item>
    <item>Medium</item>
    <item>High</item>
</string-array>
```

- `%1$d` means "first argument, an integer". `%2$s` means "second argument, a string". The
  number before `$` is the position, so translations can reorder them.
- `getString(R.string.total_calories_value, totalCalories)` fills it in.
- A `string-array` can be read in Java: `ArrayAdapter.createFromResource(this, R.array.intensity_levels, ...)`.
  This is what feeds the Spinner - one list of values, one place to change it.

Escaping rules that catch people out:

| Character | Written as | Why |
|---|---|---|
| `'` | `\'` | otherwise it starts a quoted section |
| `"` | `\"` | a bare `"` is **removed**, so `&quot;` alone is not enough |
| `&` | `&amp;` | XML itself reserves it |
| `<` `>` | `&lt;` `&gt;` | XML reserves them |

That last quote rule caused a real bug here: the delete dialog showed
`Remove Running from your history?` with no quotes, because `&quot;` became a bare `"` that
Android stripped. Changing it to `\"` fixed it.

### 6.7 `colors.xml`

```xml
<color name="green_600">#2E7D32</color>
```

`#RRGGBB`, where each pair is a hex value from 00 to FF. All five colours are named by
purpose-neutral names, so a theme can pick them.

### 6.8 `themes.xml`

```xml
<style name="Theme.FitnessTracker" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/green_600</item>
    <item name="colorOnPrimary">@color/white</item>
    ...
    <item name="android:windowOptOutEdgeToEdgeEnforcement" tools:targetApi="35">true</item>
    <item name="android:statusBarColor">@color/green_900</item>
    <item name="android:windowLightStatusBar">false</item>
</style>
```

- A **style** is a named bag of attribute values; a **theme** is a style applied to the whole
  app. `parent=` means "start from that theme and only change what I list". That is why a
  dozen lines produce a complete Material 3 look.
- `colorPrimary` is the green used by the app bar, buttons and links. `colorOnPrimary` is
  what sits *on* it, so white text on green - the pairing keeps contrast readable.
- `windowLightStatusBar=false` means "the status bar is dark, so use light icons".
- `tools:targetApi="35"` only tells the editor that this attribute exists from API 35, so it
  does not warn. It does not affect runtime.
- `windowOptOutEdgeToEdgeEnforcement` is the fix for the app bar overlapping content on
  Android 15+: without it, the system draws the app from the very top and the toolbar covers
  the first field of the form.

Dark mode: because the parent is `DayNight`, the same file works at night - only the colour
roles change, no second layout is needed.

### 6.9 `drawable/ic_launcher.xml`

```xml
<vector android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#2E7D32" android:pathData="M0,0h108v108H0z" />
    <path android:fillColor="#FFFFFF" android:pathData="M30,44h6v20h-6z" />
</vector>
```

- A **vector** is shapes, not pixels: it stays sharp at any size and is one small file
  instead of eight PNGs for different screen densities.
- `viewportWidth/Height` sets the drawing grid; `pathData` uses the same commands as SVG -
  `M` move, `h` horizontal line, `v` vertical, `z` close.
- This is referenced from the manifest as both `android:icon` and `android:roundIcon`.

---

## 7. XML values that Java reads back

Not all XML is layout. Values files feed the code directly:

| In XML | In Java |
|---|---|
| `<string name="log_workout">` | `getString(R.string.log_workout)` |
| `<string-array name="intensity_levels">` | `R.array.intensity_levels` |
| `<color name="green_600">` | `R.color.green_600`, used by the theme |
| `android:id="@+id/saveButton"` | `findViewById(R.id.saveButton)` |
| `@style/Theme.FitnessTracker` | applied by the manifest, nothing in Java |

The rule: if Java needs it by name, it has an `id` or a resource name.

---

## 8. XML mistakes to recognise

| Symptom | Cause |
|---|---|
| `Error: ... is not a valid attribute` | wrong namespace - used `android:` on a Material attribute, or forgot `xmlns:app` |
| `Element must be closed` | missing `/>` or `</...>` |
| `Cannot resolve symbol 'R.id.xyz'` | the view has no id, or the project has not rebuilt |
| Preview looks right, app looks wrong | a `tools:` attribute instead of a real one, or a value from the wrong theme |
| Everything is squashed or overflowing | missing `weight`, or `match_parent` inside a `wrap_content` parent |
| `You must specify a valid layout reference` | `setContentView` points at a layout that does not exist |
| Hard-coded text warning | text in the layout instead of `strings.xml` |

---

## 9. The one-sentence summary to give an examiner

> "Every screen is described in XML under `res/layout`, every colour, word and style in
> `res/values`, and the manifest names the screens. The build compiles them into resources
> with ids in `R`, and the activities inflate them with `setContentView` and then look up
> views by id with `findViewById`. No layout is built in Java."
